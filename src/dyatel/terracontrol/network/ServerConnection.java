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

    private Player[] players;
    private int connected = 0;
    private int ready = 0;

    private Thread turnManager;
    private int currentPlayer;
    private int state = -1; // -1 - waiting connections, 0 - playing, 1 - end

    public ServerConnection(int port, Server server) throws Exception {
        super(port, server);

        level = server.getLevel();

        start();
    }

    protected void process(final DatagramPacket packet) {
        // Get message
        final String message = new String(packet.getData()).trim();

        // Get sender
        final InetAddress address = packet.getAddress();
        final int port = packet.getPort();
        int player = findPlayer(address, port);

        //debug.println(message);

        // Get data from message
        final String prefix = message.substring(0, 4);
        final String[] dataR = message.substring(4).split("x");

        if (prefix.equals("/co/")) {
            if (level.isGenerated() && players != null) {
                debug.println("Connection request from " + packet.getAddress() + ":" + packet.getPort() + "");

                // If this player isn`t connected and we have place for players
                if (player == -1 && connected < players.length) {
                    player = connected;
                    players[connected++].connect(address, port);
                    window.statusBar[0] = "Waiting for players: " + connected + "/" + players.length;

                    if (connected == players.length) {
                        window.statusBar[0] = "Players are receiving levels: 0/" + players.length;
                    }
                }

                // Putting level data
                String data = "/da/" + level.getWidth() + "x" + level.getHeight() + "x" + level.getMasters().size();

                // Putting players
                data += "x" + players.length + "x" + player;
                for (int i = 0; i < players.length; i++) {
                    data += "x" + players[i].getMaster().getID();
                }

                // Putting colors into message
                data += "x" + level.getColors().length;
                for (int i = 0; i < level.getColors().length; i++) {
                    data += "x" + level.getColors()[i];
                }

                players[player].send(data);
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
                        int color = masters.get(i).getColorID();
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
            if (player != -1 && !players[player].isReady()) {
                players[ready++].ready();
                window.statusBar[0] = "Players are receiving levels: " + ready + "/" + players.length;

                if (ready == players.length) {
                    sendEveryoneExcluding("/st/0", -1);
                    startTurnManager();
                }
            }
        } else if (prefix.equals("/to/")) {
            if (player != -1) {
                int turn = Integer.parseInt(dataR[0]);
                int colorID = Integer.parseInt(dataR[1]);

                if (players[player].getTurns() == turn - 1) {
                    players[player].addTurn(colorID);
                    currentPlayer = nextPlayer();
                }
            }
        } else debug.println("Unknown prefix " + prefix);
    }

    protected void waitForThreads() throws InterruptedException {
        if (turnManager != null) turnManager.join();
    }

    public void createPlayers(Player[] players) {
        window.statusBar[0] = "Waiting for players: 0/" + players.length;
        this.players = players;
    }

    private int findPlayer(InetAddress address, int port) {
        if (players == null) return -1;
        for (int i = 0; i < players.length; i++) if (players[i].equals(address, port)) return i;
        return -1;
    }

    private void sendEveryoneExcluding(String message, int exclude) {
        for (int i = 0; i < players.length; i++) if (i != exclude) players[i].send(message);
    }

    public void gameOver() {
        // Find winner
        int max = -1; // Max captured cells
        int same = 0; // Needed to determine draw
        for (int i = 0; i < players.length; i++) {
            int cells = players[i].getMaster().getCells().size();
            if (cells > max) {
                max = cells;
                same = 0;
            } else if (cells == max) same++;
        }
        // Send result to every player
        for (Player player : players) {
            int cells = player.getMaster().getCells().size();
            if (cells < max) {
                player.send("/st/2");
            } else if (cells == max) {
                if (same == 0)
                    player.send("/st/1");
                else
                    player.send("/st/3");
            }
        }
    }

    private int nextPlayer() {
        return currentPlayer == players.length - 1 ? 0 : currentPlayer + 1;
    }

    private int previousPlayer() {
        return currentPlayer == 0 ? players.length - 1 : currentPlayer - 1;
    }

    private void startTurnManager() {
        state = 0;
        turnManager = new Thread() {
            public void run() {
                currentPlayer = Util.getRandom().nextInt(players.length); // First player
                while (running && state == 0) {
                    String message = "/tu/" + (players[currentPlayer].getTurns() + 1); // Player turn ID

                    // Adding enemies turns
                    for (int i = 0; i < players.length; i++) {
                        message += "x" + players[i].getTurns() + "x" + players[i].getLastTurn();
                    }

                    // Sending
                    players[currentPlayer].send(message);
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
