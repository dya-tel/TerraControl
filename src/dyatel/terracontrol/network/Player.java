package dyatel.terracontrol.network;

import dyatel.terracontrol.level.Owner;

import java.net.InetAddress;

public class Player {

    private int id;

    private Owner owner;

    private InetAddress address;
    private int port;

    private boolean connected;
    private boolean ready;

    private int lastTurn;

    public Player(Owner owner) {
        this.owner = owner;

        id = owner.getID();
    }

    public int getID() {
        return id;
    }

    public Owner getOwner() {
        return owner;
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

    public void setTurn(int color) {
        lastTurn = color;
    }

    public int getLastTurn() {
        return lastTurn;
    }

    public boolean equals(InetAddress address, int port) {
        return address.equals(this.address) && port == this.port;
    }

}
