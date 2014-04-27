package dyatel.terracontrol.network;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.ClientLevel;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.ErrorLogger;
import dyatel.terracontrol.window.Client;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;

public class ClientConnection extends Connection {

    private ClientLevel level;

    private InetAddress address;
    private int port;

    private Thread connecter;
    private boolean connected = false;

    private Thread levelReceiver;
    private int receivedMasters = 0;
    private int receivedCells = 0;

    public ClientConnection(String address, int port, Client client) throws Exception {
        super(client);

        this.address = InetAddress.getByName(address);
        this.port = port;

        level = client.getLevel();

        start();
        connect();
    }

    protected void process(DatagramPacket packet) {
        final String message = new String(packet.getData()).trim();
        //debug.println(message);

        // Get data from message
        final String prefix = message.substring(0, 4);
        final String[] dataR = message.substring(4).split("x");

        if (prefix.equals("/da/")) {
            if (!connected) {
                debug.println("Connected!");
                window.statusBar[0] = "";

                // Placing data into data wrapper
                DataArray data = new DataArray();
                data.fillInteger("levelWidth", dataR[0]);
                data.fillInteger("levelHeight", dataR[1]);
                data.fillInteger("masters", dataR[2]);
                data.fillInteger("masterID", dataR[3]);
                data.fillInteger("ownerID", dataR[4]);
                data.fillInteger("enemyMaster", dataR[5]);
                data.fillInteger("enemyOwner", dataR[6]);

                // Placing all colors into array
                int n = Integer.parseInt(dataR[7]);
                data.fillInteger("colors", n);
                for (int i = 0; i < n; i++) {
                    data.fillInteger("color" + i, dataR[8 + i]);
                }

                // Initializing level with all data
                level.init(data);

                // Requesting level
                receiveLevel();

                connected = true;
            }
        } else if (prefix.equals("/ma/")) {
            ArrayList<CellMaster> masters = level.getMasters();

            int start = Integer.parseInt(dataR[0]);
            int end = Integer.parseInt(dataR[1]);

            if (start == receivedMasters) {
                for (int i = start; i <= end; i++) {
                    int masterColor = Integer.parseInt(dataR[i - start + 2]);
                    CellMaster master = masters.get(i);
                    master.setColorID(masterColor);
                    master.setID(i);
                    receivedMasters++;
                }
            } else {
                debug.println("Received wrong masters, ignoring (received from " + start + ", need from " + receivedMasters + ")");
            }

            window.statusBar[0] = "Masters: " + receivedMasters * 100 / masters.size() + "%";
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

            window.statusBar[0] = "Cells: " + receivedCells * 100 / (width * height) + "%";
        } else if (prefix.equals("/tu/")) {
            int turn = Integer.parseInt(dataR[0]);
            int enemyTurn = Integer.parseInt(dataR[1]);
            int enemyColor = Integer.parseInt(dataR[2]);

            // Getting enemy`s turn
            if (enemyColor != -1 && enemyTurn == level.getEnemy().getTurns() + 1) level.getEnemy().addTurn(enemyColor);

            // Making out turn
            if (turn == level.getOwner().getTurns()) {
                send("/to/" + turn + "x" + level.getOwner().getLastTurn());
            } else {
                level.needTurn();
            }
        } else if (prefix.equals("/te/")) {
            // Receiving enemy`s move
            int turn = Integer.parseInt(dataR[0]);
            int colorID = Integer.parseInt(dataR[1]);

            if (turn == level.getEnemy().getTurns() + 1) level.getEnemy().addTurn(colorID);
        } else if (message.startsWith("/st/")) {
            int state = Integer.parseInt(message.substring(4));
            level.changeState(state);
        }
    }

    protected void waitForThreads() throws InterruptedException {
        if (connecter != null) connecter.join();
        if (levelReceiver != null) levelReceiver.join();
    }

    public void connect() {
        connecter = new Thread("Connecter") {
            public void run() {
                while (!connected && running) {
                    try {
                        send("/co/");
                        sleep(1000);
                    } catch (InterruptedException e) {
                        ErrorLogger.add(e);
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
                        ErrorLogger.add(e);
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
                        ErrorLogger.add(e);
                    }
                }

                window.statusBar[0] = "";
                level.ready();
                send("/rd/");
            }
        };
        levelReceiver.start();
    }

    public void send(String message) {
        send(message, address, port);
    }

}
