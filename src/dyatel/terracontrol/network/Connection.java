package dyatel.terracontrol.network;

import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.util.ErrorLogger;
import dyatel.terracontrol.window.GameWindow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class Connection {

    public static final int BUFFER_SIZE = 1024;

    protected GameWindow window;
    protected Debug debug;

    protected DatagramSocket socket;

    protected Thread receiver;
    protected boolean running = false;

    protected int transmitted = 0;
    protected int received = 0;

    public Connection(GameWindow window) throws Exception {
        this.window = window;
        debug = window.getDebug();

        socket = new DatagramSocket();
        debug.println("Bound socket at " + socket.getLocalPort());
    }

    public Connection(int port, GameWindow window) throws Exception {
        this.window = window;
        debug = window.getDebug();

        socket = new DatagramSocket(port);
        debug.println("Bound socket at " + socket.getLocalPort());
    }

    protected void start() {
        receiver = new Thread("Receiver") {
            public void run() {
                while (running) {
                    try {
                        process(receive());
                    } catch (IOException e) {
                        debug.println("Socket closed!");
                    } catch (Exception e) {
                        ErrorLogger.add(e);
                    }
                }
            }
        };

        running = true;
        receiver.start();
    }

    public void stop() {
        running = false;
        debug.println("Closing connection...");
        socket.close();
        try {
            waitForThreads();
            receiver.join();
        } catch (InterruptedException e) {
            ErrorLogger.add(e);
        }
        debug.println("Connection closed!");
    }

    protected abstract void process(DatagramPacket packet);

    protected abstract void waitForThreads() throws InterruptedException;

    protected void send(String message, InetAddress address, int port) {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, port);
        try {
            socket.send(packet);
            transmitted += packet.getLength();
        } catch (IOException e) {
            ErrorLogger.add(e);
        }
    }

    protected DatagramPacket receive() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        received += packet.getLength();
        return packet;
    }

    public String getTransmitted() {
        if (transmitted / 1024 > 0) {
            if (transmitted / 1024 / 1024 > 0) {
                return (int) ((transmitted / 1024d / 1024) * 10) / 10 + " MB";
            } else return (int) (transmitted / 1024d * 10) / 10 + " KB";
        } else return transmitted + " bytes";
    }

    public String getReceived() {
        if (received / 1024 > 0) {
            if (received / 1024 / 1024 > 0) {
                return (int) ((received / 1024d / 1024) * 10) / 10 + " MB";
            } else return (int) (received / 1024d * 10) / 10 + " KB";
        } else return received + " bytes";
    }

}
