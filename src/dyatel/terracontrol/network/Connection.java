package dyatel.terracontrol.network;

import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.window.GameWindow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public abstract class Connection {

    public static final int BUFFER_SIZE = 1024;

    protected GameWindow window;
    protected Debug debug;

    protected DatagramSocket socket;

    protected Thread receiver;

    protected boolean running = false;

    protected int transmitted = 0;
    protected int received = 0;

    public Connection(GameWindow window) {
        this.window = window;
        debug = window.getDebug();

        try {
            socket = new DatagramSocket();
            debug.println("Bound socket at " + socket.getLocalPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public Connection(int port, GameWindow window) {
        this.window = window;
        debug = window.getDebug();

        try {
            socket = new DatagramSocket(port);
            debug.println("Bound socket at " + socket.getLocalPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
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
                        e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
