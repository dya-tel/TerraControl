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

    private ServerLevel level; // Our level

    private Player[] players; // Array of players, copy of ServerLevel players
    private int connected = 0; // How many players are connected
    private int ready = 0; // How many players are ready to play

    private Thread turnManager; // Thread asking for turns
    private int currentPlayer; // ID of player that is making turn
    private int state = -1; // -1 - waiting connections, 0 - playing, 1 - end

    public ServerConnection(int port, Server server) throws Exception {
        super(port, server);

        level = server.getLevel();

        start(); // Starting receiver
    }

    protected void process(final DatagramPacket packet) {
        // Get message
        final byte code = packet.getData()[0];
        final String message = new String(packet.getData()).trim();

        // Get sender
        final InetAddress address = packet.getAddress();
        final int port = packet.getPort();
        Player player = findPlayer(address, port);

        // Get data from message
        final String[] dataR = message.split("x");

        //debug.println(code + " " + message);

        if (code == CODE_CONNECT) {
            if (level.isGenerated() && players != null) {
                debug.println("Connection request from " + packet.getAddress() + ":" + packet.getPort() + "");

                // If this player isn`t connected and we have place for players
                if (player == null) {
                    if (connected < players.length) {
                        player = players[connected++];
                        player.connect(address, port);
                    } else return;
                }

                // Putting level and player data
                String data = level.getWidth() + "x" + level.getHeight() + "x" + level.getMasters().size() + "x" + players.length + "x" + player.getID();
                for (Player p : players) {
                    data += "x" + p.getMaster().getID();
                }

                // Putting colors into message
                data += "x" + level.getColors().length;
                for (int i = 0; i < level.getColors().length; i++) {
                    data += "x" + level.getColors()[i];
                }

                player.send(CODE_DATA, data);
            }
        } else if (code == CODE_MASTERS) {
            ArrayList<CellMaster> masters = level.getMasters();

            // Adding as much color IDs as possible
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

            // Adding as much masters as possible
            int start = Integer.parseInt(dataR[0]);
            String data = String.valueOf(start);
            int i = start;
            while (i < cells && (data + "x" + level.getMaster(i % width, i / width).getID()).length() <= MESSAGE_SIZE) {
                data += "x" + level.getMaster(i % width, i / width).getID();
                i++;
            }
            send(CODE_CELLS, data, address, port);
        } else if (code == CODE_READY) {
            if (player != null && !player.isReady()) {
                player.ready();

                if (++ready == players.length) {
                    level.setState(3);
                    sendEveryoneExcluding(CODE_STATE, "0", -1); // Sending to everyone
                    startTurnManager();
                }
            }
        } else if (code == CODE_TURN) {
            if (player != null) {
                int turn = Integer.parseInt(dataR[0]);
                int colorID = Integer.parseInt(dataR[1]);

                if (player.getTurns() == turn - 1) {
                    player.addTurn(colorID);
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

    private Player findPlayer(InetAddress address, int port) {
        if (players == null) return null;
        for (Player player : players) if (player.equals(address, port)) return player;
        return null; // If did not find this player
    }

    private void sendEveryoneExcluding(byte code, String message, int exclude) {
        for (int i = 0; i < players.length; i++) if (i != exclude) players[i].send(code, message);
    }

    public void gameOver() {
        // Find winner
        int max = -1; // Max captured cells
        int same = 0; // Needed to determine draw
        for (Player player : players) {
            int cells = player.getMaster().getCells().size();
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
        state = 1;
    }

    private int nextPlayer() {
        return currentPlayer == players.length - 1 ? 0 : currentPlayer + 1;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    private void startTurnManager() {
        state = 0;
        turnManager = new Thread() {
            public void run() {
                int lastPlayer = -1; // Last player that we asked for a turn
                currentPlayer = Util.getRandom().nextInt(players.length); // First player
                while (running && state == 0) {
                    String message = "";

                    // Adding enemies turns
                    for (Player player : players) {
                        message += "x" + player.getTurns() + "x" + player.getLastTurn();
                    }

                    if (lastPlayer != currentPlayer)
                        sendEveryoneExcluding(CODE_ENEMY_TURNS, message.substring(1), currentPlayer); // Sending everyone excluding current player turns

                    // Asking current player`s turn
                    players[currentPlayer].send(CODE_TURN, (players[currentPlayer].getTurns() + 1) + message);
                    lastPlayer = currentPlayer;
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
