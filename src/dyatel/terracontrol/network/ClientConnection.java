package dyatel.terracontrol.network;

import dyatel.terracontrol.Client;
import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.ClientLevel;
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

    private Thread eTurnReceiver;

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
        final String message = new String(packet.getData()).trim();
        debug.println(message);

        // Get data from message
        final String prefix = message.substring(0, 4);
        final String[] dataR = message.substring(4).split("x");

        final ClientLevel cLevel = (ClientLevel) level;
        if (prefix.equals("/da/")) {
            debug.println("Connected!");
            client.statusBar[0] = "";

            // Placing all colors into array
            int n = Integer.parseInt(dataR[7]);
            int[] colors = new int[n];
            for (int i = 0; i < n; i++) {
                colors[i] = Integer.parseInt(dataR[8 + i]);
            }
            // Initializing level with all data
            cLevel.init(Integer.parseInt(dataR[0]), Integer.parseInt(dataR[1]), Integer.parseInt(dataR[2]), Integer.parseInt(dataR[3]), Integer.parseInt(dataR[4]), Integer.parseInt(dataR[5]), Integer.parseInt(dataR[6]), colors, this);
            connected = true;
        } else if (prefix.equals("/ma/")) {
            ArrayList<CellMaster> masters = cLevel.getMasters();

            int start = Integer.parseInt(dataR[0]);
            int end = Integer.parseInt(dataR[1]);

            if (start == receivedMasters) {
                for (int i = start; i <= end; i++) {
                    int masterColor = level.getColors()[Integer.parseInt(dataR[i - start + 2])];
                    CellMaster master = masters.get(i);
                    master.setColor(masterColor);
                    master.setID(i);
                    receivedMasters++;
                }
            } else {
                debug.println("Received wrong masters, ignoring (received from " + start + ", need from " + receivedMasters + ")");
            }

            client.statusBar[0] = "Masters: " + receivedMasters * 100 / masters.size() + "%";
        } else if (prefix.equals("/ce/")) {
            int start = Integer.parseInt(dataR[0]);
            int end = Integer.parseInt(dataR[1]);

            int width = level.getWidth();
            int height = level.getHeight();
            ArrayList<CellMaster> masters = level.getMasters();

            if (start == receivedCells) {
                for (int i = start; i <= end; i++) {
                    int masterID = Integer.parseInt(dataR[i - start + 2]);
                    new Cell(i % width, i / width, masters.get(masterID));
                    receivedCells++;
                }
            } else {
                debug.println("Received wrong cells, ignoring (received from " + start + ", need from " + receivedCells + ")");
            }

            client.statusBar[0] = "Cells: " + receivedCells * 100 / (width * height) + "%";
        } else if (prefix.equals("/tu/")) {
            int turn = Integer.parseInt(dataR[0]);

            if (turn == cLevel.getOwner().getTurns()) {
                send("/to/" + turn + "x" + cLevel.getOwner().getLastTurn());
                receiveEnemyTurn();
            } else {
                cLevel.needTurn();
            }
        } else if (prefix.equals("/te/")) {
            // Receiving enemy`s move
            int turn = Integer.parseInt(dataR[0]);
            int colorID = Integer.parseInt(dataR[1]);

            if (turn == cLevel.getEnemy().getTurns() + 1) cLevel.getEnemy().addTurn(colorID);
        } else if (message.startsWith("/st/")) {
            int state = Integer.parseInt(message.substring(4));
            cLevel.changeState(state);
        }
    }

    protected void waitForThreads() throws InterruptedException {
        if (connecter != null) connecter.join();
        if (levelReceiver != null) levelReceiver.join();
        if (eTurnReceiver != null) eTurnReceiver.join();
    }

    public void connect() {
        connecter = new Thread("Connecter") {
            public void run() {
                while (!connected && running) {
                    try {
                        send("/co/");
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
                int perRequest;

                int width = level.getWidth();
                int height = level.getHeight();
                int masters = level.getMasters().size();

                // Requesting masters
                // Calculating the number of max possible masters per request
                perRequest = ((Connection.BUFFER_SIZE - 4) - 2 * (String.valueOf(masters).length()) - 1) / (String.valueOf(level.getColors().length).length() + 1) - 1;
                while (receivedMasters < masters && running) {
                    try {
                        if (receivedCells + perRequest < masters)
                            send("/ma/" + receivedMasters + "x" + (receivedMasters + perRequest));
                        else
                            send("/ma/" + receivedMasters + "x" + masters);

                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Requesting cells
                // Calculating the number of max possible cells per request
                perRequest = ((Connection.BUFFER_SIZE - 4) - 2 * (String.valueOf(width * height).length()) - 1) / (String.valueOf(masters).length() + 1) - 1;
                while (receivedCells < width * height && running) {
                    try {
                        if (receivedCells + perRequest < width * height)
                            send("/ce/" + receivedCells + "x" + (receivedCells + perRequest));
                        else
                            send("/ce/" + receivedCells + "x" + width * height);

                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                client.statusBar[0] = "";
                level.ready();
                send("/rd/");
            }
        };

        levelReceiver.start();
    }

    public void receiveEnemyTurn() {
        if (eTurnReceiver != null) return;
        eTurnReceiver = new Thread("EnemyTurnReceiver") {
            public void run() {
                while (running && !((ClientLevel) level).isMyTurn()) {
                    send("/te/");
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                eTurnReceiver = null;
            }
        };
        eTurnReceiver.start();
    }

    public void send(String message) {
        send(message, address, port);
    }

}
