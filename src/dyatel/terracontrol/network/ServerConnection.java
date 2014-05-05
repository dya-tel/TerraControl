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
        final byte code = packet.getData()[0];
        final String message = new String(packet.getData()).trim();

        // Get sender
        final InetAddress address = packet.getAddress();
        final int port = packet.getPort();
        int player = findPlayer(address, port);

        // Get data from message
        final String[] dataR = message.split("x");

        //debug.println(code + " " + message);

        if (code == CODE_CONNECT) {
            if (level.isGenerated() && players != null) {
                debug.println("Connection request from " + packet.getAddress() + ":" + packet.getPort() + "");

                // If this player isn`t connected and we have place for players
                if (player == -1 && connected < players.length) {
                    player = connected;
                    players[connected++].connect(address, port);
                }

                // Putting level data
                String data = level.getWidth() + "x" + level.getHeight() + "x" + level.getMasters().size();
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

                players[player].send(CODE_DATA, data);
            }
        } else if (code == CODE_MASTERS) {
            ArrayList<CellMaster> masters = level.getMasters();

            int start = Integer.parseInt(dataR[0]);
            String data = String.valueOf(start);
            int i = start;
            while (i < masters.size() && (data + "x" + masters.get(i).getColorID()).length() <= MESSAGE_SIZE) {
                data += "x" + masters.get(i++).getColorID();
            }
            send(CODE_MASTERS, data, address, port);
        } else if (code == CODE_CELLS) {
            int width = level.getWidth();
            int cells = width * level.getHeight();

            int start = Integer.parseInt(dataR[0]);
            String data = String.valueOf(start);
            int i = start;
            while (i < cells && (data + "x" + level.getMaster(i % width, i / width).getID()).length() <= MESSAGE_SIZE) {
                data += "x" + level.getMaster(i % width, i / width).getID();
                i++;
            }
            send(CODE_CELLS, data, address, port);
        } else if (code == CODE_READY) {
            if (player != -1 && !players[player].isReady()) {
                players[ready++].ready();

                if (ready == players.length) {
                    level.setState(3);
                    sendEveryoneExcluding(CODE_STATE, "0", -1);
                    startTurnManager();
                }
            }
        } else if (code == CODE_TURN) {
            if (player != -1) {
                int turn = Integer.parseInt(dataR[0]);
                int colorID = Integer.parseInt(dataR[1]);

                if (players[player].getTurns() == turn - 1) {
                    players[player].addTurn(colorID);
                    currentPlayer = nextPlayer();
                }
            }
        } else debug.println("Unknown code " + code);
    }

    protected void waitForThreads() throws InterruptedException {
        if (turnManager != null) turnManager.join();
    }

    public void createPlayers(Player[] players) {
        this.players = players;
    }

    private int findPlayer(InetAddress address, int port) {
        if (players == null) return -1;
        for (int i = 0; i < players.length; i++) if (players[i].equals(address, port)) return i;
        return -1;
    }

    private void sendEveryoneExcluding(byte code, String message, int exclude) {
        for (int i = 0; i < players.length; i++) if (i != exclude) players[i].send(code, message);
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
                player.send(CODE_STATE, "2");
            } else if (cells == max) {
                if (same == 0)
                    player.send(CODE_STATE, "1");
                else
                    player.send(CODE_STATE, "3");
            }
        }
    }

    private int nextPlayer() {
        return currentPlayer == players.length - 1 ? 0 : currentPlayer + 1;
    }

    private int previousPlayer() {
        return currentPlayer == 0 ? players.length - 1 : currentPlayer - 1;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    private void startTurnManager() {
        state = 0;
        turnManager = new Thread() {
            public void run() {
                currentPlayer = Util.getRandom().nextInt(players.length); // First player
                while (running && state == 0) {
                    String message = String.valueOf(players[currentPlayer].getTurns() + 1); // Player turn ID

                    // Adding enemies turns
                    for (int i = 0; i < players.length; i++) {
                        message += "x" + players[i].getTurns() + "x" + players[i].getLastTurn();
                    }

                    // Sending
                    players[currentPlayer].send(CODE_TURN, message);
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
