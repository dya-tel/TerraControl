package dyatel.terracontrol.network;

import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.Level;

import java.net.InetAddress;
import java.util.ArrayList;

public class Player {

    private int id; // ID

    private Level level; // Our level

    private CellMaster master; // Master we are controlling

    private Connection connection; // Connection to send messages
    private InetAddress address; // Client address
    private int port; // Client port

    private boolean connected; // Do we have a client
    private boolean ready; // Is client ready to play

    private int turns = 0; // How many turns we made
    private int lastTurn = -1; // Last color ID

    public Player(CellMaster master, int id, Connection connection) {
        this.id = id;
        this.connection = connection;
        level = master.getLevel();

        // Adding on level
        if (master.getOwner() == null) {
            this.master = master;
            master.setOwner(this);
        } else {
            level.getDebug().println("Failed creating player " + this + " on master " + master + ": someone is already owning this master!");
        }
    }

    public int getID() {
        return id;
    }

    public CellMaster getMaster() {
        return master;
    }

    public void connect(InetAddress address, int port) {
        this.address = address;
        this.port = port;

        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public void send(byte code, String message) {
        connection.send(code, message, address, port);
    }

    public void ready() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    public void incrementTurns() {
        turns++;
    }

    public void addTurn(int colorID) {
        master.setColorID(colorID);

        lastTurn = colorID;
        turns++;
    }

    public int getTurns() {
        return turns;
    }

    public int getLastTurn() {
        return lastTurn;
    }

    public int canCapture(int colorID) {
        int availableCells = 0;
        if (colorID != -1) {
            ArrayList<CellMaster> neighbors = master.getNeighbors();
            for (CellMaster neighbor : neighbors) {
                if (neighbor.getColorID() == colorID && neighbor.getOwner() == null) {
                    availableCells += neighbor.getCells().size();
                }
            }
        }
        return availableCells;
    }

    public boolean haveAvailableTurns() {
        for (int i = 0; i < level.getColors().length; i++) {
            if (canCapture(i) > 0) return true;
        }
        return false;
    }

    public boolean equals(InetAddress address, int port) {
        return address.equals(this.address) && port == this.port;
    }

}
