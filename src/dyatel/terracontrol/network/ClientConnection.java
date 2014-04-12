package dyatel.terracontrol.network;

import dyatel.terracontrol.Client;
import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.ClientLevel;
import dyatel.terracontrol.level.Owner;
import dyatel.terracontrol.util.Debug;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientConnection extends Connection {

    private Client client;

    private InetAddress address;
    private int port;

    private Thread connecter;
    private boolean connected = false;

    private Thread levelReceiver;
    private int receivedMasters = 0;
    private int receivedCells = 0;

    private Thread turnThread;
    private boolean turnSuccessfulO;
    private boolean turnSuccessfulE;

    public ClientConnection(String address, int port, Client client) {
        super(Debug.clientDebug);

        try {
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.port = port;

        this.client = client;
        level = client.getLevel();

        start();
        connect();
    }

    protected void process(DatagramPacket packet) {
        String message = new String(packet.getData()).trim();
        //debug.println(message);
        if (message.startsWith("/da/")) {
            String[] dataR = message.substring(4).split("x");
            debug.println("Connected!");
            client.statusBar[0] = "";
            ((ClientLevel) level).init(Integer.parseInt(dataR[0]), Integer.parseInt(dataR[1]), Integer.parseInt(dataR[2]), Integer.parseInt(dataR[3]), Integer.parseInt(dataR[4]), Integer.parseInt(dataR[5]), Integer.parseInt(dataR[6]), this);
            connected = true;
        } else if (message.startsWith("/ma/")) {
            ArrayList<CellMaster> masters = level.getMasters();

            String[] cellsR = message.substring(4).split("x");
            int start = Integer.parseInt(cellsR[0]);
            int end = Integer.parseInt(cellsR[1]);

            if (start == receivedMasters) {
                for (int i = start; i <= end; i++) {
                    int masterColor = Integer.parseInt(cellsR[i - start + 2]);
                    CellMaster master = masters.get(i);
                    master.setColor(masterColor);
                    master.setID(i);
                    receivedMasters++;
                }
            } else {
                debug.println("Received wrong masters, ignoring (received from " + start + ", need from " + receivedMasters + ")");
            }

            client.statusBar[0] = "Masters: " + receivedMasters * 100 / masters.size() + "%";
        } else if (message.startsWith("/ce/")) {
            String[] cellsR = message.substring(4).split("x");
            int start = Integer.parseInt(cellsR[0]);
            int end = Integer.parseInt(cellsR[1]);

            int width = level.getWidth();
            int height = level.getHeight();
            ArrayList<CellMaster> masters = level.getMasters();

            if (start == receivedCells) {
                for (int i = start; i <= end; i++) {
                    int masterID = Integer.parseInt(cellsR[i - start + 2]);
                    new Cell(i % width, i / width, masters.get(masterID));
                    receivedCells++;
                }
            } else {
                debug.println("Received wrong cells, ignoring (received from " + start + ", need from " + receivedCells + ")");
            }

            client.statusBar[0] = "Cells: " + receivedCells * 100 / (width * height) + "%";
        } else if (message.startsWith("/to/")) {
            // Receiving our move
            String[] dataR = message.substring(4).split("x");
            int color = Integer.parseInt(dataR[0]);
            ((ClientLevel) level).getOwner().setColor(color);

            turnSuccessfulO = true;
        } else if (message.startsWith("/te/")) {
            // Receiving enemy`s move
            String[] dataR = message.substring(4).split("x");
            int color = Integer.parseInt(dataR[0]);
            ((ClientLevel) level).getEnemy().setColor(color);

            turnSuccessfulE = true;
        } else if (message.startsWith("/st/")) {
            int state = Integer.parseInt(message.substring(4));
            ((ClientLevel) level).changeState(state);
        }
    }

    protected void waitForThreads() throws InterruptedException {
        if (connecter != null) connecter.join();
        if (levelReceiver != null) levelReceiver.join();
        if (turnThread != null) turnThread.join();
    }

    public void connect() {
        connecter = new Thread("Connecter") {
            public void run() {
                while (!connected && running) {
                    try {
                        send("/co/", address, port);
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        connecter.start();
    }

    public void receiveLevel() {
        levelReceiver = new Thread("LevelReceiver") {
            public void run() {
                int perRequest = 100;

                int width = level.getWidth();
                int height = level.getHeight();
                int masters = level.getMasters().size();

                // Requesting masters
                while (receivedMasters < masters && running) {
                    try {
                        if (receivedCells + perRequest < masters)
                            send("/ma/" + receivedMasters + "x" + (receivedMasters + perRequest), address, port);
                        else
                            send("/ma/" + receivedMasters + "x" + masters, address, port);

                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Requesting cells
                while (receivedCells < width * height && running) {
                    try {
                        if (receivedCells + perRequest < width * height)
                            send("/ce/" + receivedCells + "x" + (receivedCells + perRequest), address, port);
                        else
                            send("/ce/" + receivedCells + "x" + width * height, address, port);

                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                client.statusBar[0] = "";
                level.ready();
                send("/rd/", address, port);
            }
        };

        levelReceiver.start();
    }

    public void turn(final int color) {
        final Owner owner = ((ClientLevel) level).getOwner();
        final ClientLevel level = (ClientLevel) this.level;
        if (owner.getColor() == color) return;
        turnThread = new Thread("TurnManager") {
            public void run() {
                if (color != 0) {
                    // Sending our turn
                    level.setCanMakeATurn(false);
                    turnSuccessfulO = false;
                    while (!turnSuccessfulO && running) {
                        send("/tu/" + color, address, port);

                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Receiving enemy`s turn
                turnSuccessfulE = false;
                while (!turnSuccessfulE && running) {
                    send("/te/", address, port);

                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                level.setCanMakeATurn(true);
            }
        };
        turnThread.start();
    }

}
