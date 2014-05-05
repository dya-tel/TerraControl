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
        // Get message
        final byte code = packet.getData()[0];
        final String message = new String(packet.getData()).trim();

        // Get data from message
        final String[] dataR = message.split("x");

        //debug.println(code + " " + message);

        if (code == CODE_DATA) {
            if (!connected) {
                debug.println("Connected!");
                window.statusBar[0] = "";

                // Placing data into data wrapper
                DataArray data = new DataArray();
                data.fillInteger("levelWidth", dataR[0]);
                data.fillInteger("levelHeight", dataR[1]);
                data.fillInteger("masters", dataR[2]);

                // Placing all masters into array
                int players = Integer.parseInt(dataR[3]);
                data.fillInteger("players", players);
                data.fillInteger("playerID", dataR[4]);
                for (int i = 0; i < players; i++) {
                    data.fillInteger("player" + i, dataR[5 + i]);
                }

                // Placing all colors into array
                int colors = Integer.parseInt(dataR[5 + players]);
                data.fillInteger("colors", colors);
                for (int i = 0; i < colors; i++) {
                    data.fillInteger("color" + i, dataR[6 + players + i]);
                }

                // Initializing level with all data
                level.init(data);

                // Requesting level
                receiveLevel();

                connected = true;
            }
        } else if (code == CODE_MASTERS) {
            ArrayList<CellMaster> masters = level.getMasters();

            int start = Integer.parseInt(dataR[0]);
            if (start == receivedMasters) {
                for (int i = start; i < start + dataR.length - 1; i++) {
                    int masterColor = Integer.parseInt(dataR[1 + i - start]);
                    CellMaster master = masters.get(i);
                    master.setColorID(masterColor);
                    master.setID(i);
                    receivedMasters++;
                }
            } else {
                debug.println("Received wrong masters, ignoring (received from " + start + ", need from " + receivedMasters + ")");
            }

            window.statusBar[0] = "Masters: " + receivedMasters * 100 / masters.size() + "%";
        } else if (code == CODE_CELLS) {
            ArrayList<CellMaster> masters = level.getMasters();
            int width = level.getWidth();
            int cells = width * level.getHeight();

            int start = Integer.parseInt(dataR[0]);
            if (start == receivedCells) {
                for (int i = start; i < start + dataR.length - 1; i++) {
                    int masterID = Integer.parseInt(dataR[1 + i - start]);
                    new Cell(i % width, i / width, masters.get(masterID));
                    receivedCells++;
                }
            } else {
                debug.println("Received wrong cells, ignoring (received from " + start + ", need from " + receivedCells + ")");
            }

            window.statusBar[0] = "Cells: " + receivedCells * 100 / cells + "%";
        } else if (code == CODE_TURN) {
            int turn = Integer.parseInt(dataR[0]);
            // Making out turn
            if (turn == level.getClientPlayer().getTurns()) {
                send(CODE_TURN, turn + "x" + level.getClientPlayer().getLastTurn());
            } else {
                level.needTurn();
            }

            // Getting enemies turns
            for (int i = 0; i < level.getPlayers(); i++) {
                int turns = Integer.parseInt(dataR[1 + i * 2]);
                int colorID = Integer.parseInt(dataR[2 + i * 2]);
                if (colorID != -1 && turns == level.getPlayer(i).getTurns() + 1) level.getPlayer(i).addTurn(colorID);
            }
        } else if (code == CODE_STATE) {
            int state = Integer.parseInt(message);
            level.setState(state);
        } else debug.println("Unknown code " + code);
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
                        send(CODE_CONNECT, "");
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
                int masters = level.getMasters().size();
                int cells = level.getWidth() * level.getHeight();

                // Requesting masters
                while (receivedMasters < masters && running) {
                    try {
                        send(CODE_MASTERS, String.valueOf(receivedMasters));
                        sleep(100);
                    } catch (InterruptedException e) {
                        ErrorLogger.add(e);
                    }
                }

                // Requesting cells
                while (receivedCells < cells && running) {
                    try {
                        send(CODE_CELLS, String.valueOf(receivedCells));
                        sleep(100);
                    } catch (InterruptedException e) {
                        ErrorLogger.add(e);
                    }
                }

                window.statusBar[0] = "";
                level.ready();
                send(CODE_READY, "");
            }
        };
        levelReceiver.start();
    }

    public void send(byte code, String message) {
        send(code, message, address, port);
    }

}
