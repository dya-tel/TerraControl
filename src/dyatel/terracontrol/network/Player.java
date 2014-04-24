package dyatel.terracontrol.network;

import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.Level;

import java.net.InetAddress;

public class Player {

    private int id;

    private Level level;

    private CellMaster master;

    private InetAddress address;
    private int port;

    private boolean connected;
    private boolean ready;

    private int turns = 0;
    private int lastTurn = -1;

    public Player(int x, int y, int id, Level level) {
        this(level.getCell(x, y).getMaster(), id);
    }

    public Player(CellMaster master, int id) {
        this.id = id;
        level = master.getLevel();

        if (master.getOwner() == null) {
            level.getDebug().println("Creating player at master " + master);
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

    public int getColor() {
        return master.getColor();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void connect(InetAddress address, int port) {
        this.address = address;
        this.port = port;

        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public void ready() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    public void addTurn(int colorID) {
        master.setColor(level.getColors()[colorID]);
        level.needUpdate(master);

        lastTurn = colorID;
        turns++;
    }

    public int getTurns() {
        return turns;
    }

    public int getLastTurn() {
        return lastTurn;
    }

    public boolean equals(InetAddress address, int port) {
        return address.equals(this.address) && port == this.port;
    }

}
