package dyatel.terracontrol.network;

import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.ServerLevel;
import dyatel.terracontrol.util.ErrorLogger;
import dyatel.terracontrol.util.Util;
import dyatel.terracontrol.window.Server;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;

public class ServerConnection extends Connection {

    private ServerLevel level;

    private Player[] players = new Player[2];

    private Thread turnManager;
    private int currentPlayer;
    private int state = -1; // -1 - waiting connections, 0 - playing, 1 - end

    public ServerConnection(int port, Server server) throws Exception {
        super(port, server);

        level = server.getLevel();

        start();
    }

    protected void process(final DatagramPacket packet) {
        final String message = new String(packet.getData()).trim();
        final InetAddress address = packet.getAddress();
        final int port = packet.getPort();
        //debug.println(message);

        // Get data from message
        final String prefix = message.substring(0, 4);
        final String[] dataR = message.substring(4).split("x");

        if (prefix.equals("/co/")) {
            if (level.isGenerated()) {
                debug.println("Connection request from " + packet.getAddress() + ":" + packet.getPort() + "");

                String data = "/da/" + level.getWidth() + "x" + level.getHeight() + "x" + level.getMasters().size() + "x";

                if (!players[0].isConnected()) {
                    players[0].connect(address, port);
                    data += players[0].getMaster().getID() + "x" + players[0].getID() + "x" + players[1].getMaster().getID() + "x" + players[1].getID();
                } else {
                    players[1].connect(address, port);
                    data += players[1].getMaster().getID() + "x" + players[1].getID() + "x" + players[0].getMaster().getID() + "x" + players[0].getID();
                }

                // Put colors into data
                data += "x" + level.getColors().length;
                for (int i = 0; i < level.getColors().length; i++) {
                    data += "x" + level.getColors()[i];
                }

                send(data, address, port);
            }
        } else if (prefix.equals("/ma/")) {
            new Thread("MasterSender") {
                public void run() {
                    int start = Integer.parseInt(dataR[0]);
                    int end = Integer.parseInt(dataR[1]);

                    ArrayList<CellMaster> masters = level.getMasters();

                    if (end >= masters.size()) end = masters.size() - 1;

                    String data = "";
                    String temp;
                    int messageStart = start;

                    for (int i = start; i <= end; i++) {
                        int color = level.getColorID(masters.get(i).getColor());
                        temp = "/ma/" + messageStart + "x";
                        if ((temp + i + data + "x" + color).length() > Connection.BUFFER_SIZE) {
                            send(temp + (i - 1) + data, address, port);
                            messageStart = i;
                            data = "";
                        }
                        data += "x" + color;
                    }
                    send("/ma/" + messageStart + "x" + end + data, address, port);
                }
            }.start();
        } else if (prefix.equals("/ce/")) {
            new Thread("LevelSender") {
                public void run() {
                    int start = Integer.parseInt(dataR[0]);
                    int end = Integer.parseInt(dataR[1]);

                    int width = level.getWidth();
                    int height = level.getHeight();
                    if (start > width * height) return;
                    if (end >= width * height) end = width * height - 1;

                    String data = "";
                    String temp;
                    int messageStart = start;

                    for (int i = start; i <= end; i++) {
                        int id = level.getMaster(i % width, i / width).getID();
                        temp = "/ce/" + messageStart + "x";
                        if ((temp + i + data + "x" + id).length() > Connection.BUFFER_SIZE) {
                            send(temp + (i - 1) + data, address, port);
                            messageStart = i;
                            data = "";
                        }
                        data += "x" + id;
                    }
                    send("/ce/" + messageStart + "x" + end + data, address, port);
                }
            }.start();
        } else if (prefix.equals("/rd/")) {
            if (players[0].equals(address, port)) {
                players[0].ready();
            } else {
                players[1].ready();
            }

            if (players[0].isReady() && players[1].isReady()) {
                send("/st/0", players[0].getAddress(), players[0].getPort());
                send("/st/0", players[1].getAddress(), players[1].getPort());
                startTurnManager();
            }
        } else if (prefix.equals("/to/")) {
            int turn = Integer.parseInt(dataR[0]);
            int colorID = Integer.parseInt(dataR[1]);

            if (players[0].equals(address, port)) {
                if (players[0].getTurns() == turn - 1) {
                    players[0].addTurn(colorID);
                    currentPlayer = 1;
                }
            } else {
                if (players[1].getTurns() == turn - 1) {
                    players[1].addTurn(colorID);
                    currentPlayer = 0;
                }
            }
        } else if (prefix.equals("/te/")) {
            if (players[0].equals(address, port)) {
                if (players[1].getLastTurn() != -1)
                    send("/te/" + players[1].getTurns() + "x" + players[1].getLastTurn(), players[0].getAddress(), players[0].getPort());
            } else {
                if (players[0].getLastTurn() != -1)
                    send("/te/" + players[0].getTurns() + "x" + players[0].getLastTurn(), players[1].getAddress(), players[1].getPort());
            }
        }
    }

    protected void waitForThreads() throws InterruptedException {
        if (turnManager != null) turnManager.join();
    }

    public void createPlayers() {
        players[0] = new Player(0, 0, 0, level);
        players[1] = new Player(level.getWidth() - 1, level.getHeight() - 1, 1, level);
    }

    public void gameOver() {
        int p1 = players[0].getMaster().getCells().size();
        int p2 = players[1].getMaster().getCells().size();
        if (p1 > p2) {
            send("/st/1", players[0].getAddress(), players[0].getPort());
            send("/st/2", players[1].getAddress(), players[1].getPort());
        } else if (p1 < p2) {
            send("/st/2", players[0].getAddress(), players[0].getPort());
            send("/st/1", players[1].getAddress(), players[1].getPort());
        } else {
            send("/st/3", players[0].getAddress(), players[0].getPort());
            send("/st/3", players[1].getAddress(), players[1].getPort());
        }

        state = 2;
    }

    private void startTurnManager() {
        state = 0;
        turnManager = new Thread() {
            public void run() {
                currentPlayer = Util.getRandom().nextInt(players.length);

                while (running && state == 0) {
                    int otherPlayer = currentPlayer == 0 ? 1 : 0;
                    send("/tu/" + (players[currentPlayer].getTurns() + 1) + "x" + players[otherPlayer].getTurns() + "x" + players[otherPlayer].getLastTurn(), players[currentPlayer].getAddress(), players[currentPlayer].getPort());
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        ErrorLogger.add(e);
                    }
                }
            }
        };
        turnManager.start();
    }

}
