package dyatel.terracontrol.network;

import dyatel.terracontrol.Client;
import dyatel.terracontrol.Screen;
import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.Level;
import dyatel.terracontrol.level.Owner;
import dyatel.terracontrol.util.Color;
import dyatel.terracontrol.util.Debug;

import java.util.ArrayList;

public class ClientLevel extends Level {

    private Client client;
    private ClientConnection connection;

    private boolean initialized = false;

    private Owner owner;
    private Owner enemy;

    private boolean canMakeATurn;
    private int state = -1; // -1 - waiting, 0 - playing, 1 - won, 2 - lost, 3 - draw

    private Cell currentCell; // Cell player is pointing on
    public int currentColor;

    public ClientLevel(int cellSize, Client client) {
        super(cellSize, Debug.clientDebug);

        this.client = client;

        keyboard = client.getKeyboard();
        mouse = client.getMouse();

        mouse.setLevel(this);
    }

    public void init(int width, int height, int masters, int masterID, int ownerID, int enemyMaster, int enemyOwner, ClientConnection connection) {
        this.width = width;
        this.height = height;

        this.connection = connection;

        cells = new Cell[width * height];

        // Creating masters
        this.masters = new ArrayList<CellMaster>();
        for (int i = 0; i < masters; i++) {
            new CellMaster(0xffffffff, this);
        }

        // Placing owners
        owner = new Owner(this.masters.get(masterID), ownerID, this);
        enemy = new Owner(this.masters.get(enemyMaster), enemyOwner, this);

        canMakeATurn = ownerID == 0;
        if (!canMakeATurn) connection.turn(0);

        // Requesting level
        connection.receiveLevel();

        initialized = true;
    }

    public void update() {
        // Updating key delay
        if (keyDelay > -1) keyDelay--;

        // Updating mouse coordinates, printing them
        mouseX = mouse.getX();
        mouseY = mouse.getY();
        mouseLX = Math.min((mouseX + xOff) / (getCellSize() + 1), mouseX);
        mouseLY = Math.min((mouseY + yOff) / (getCellSize() + 1), mouseY);
        if (mouseY > client.getFieldHeight()) {
            mouseLX = -1;
            mouseLY = -1;
        }
        client.statusBar[2] = mouseLX + " " + mouseLY;

        // Getting cell and color under mouse
        currentCell = getCell(mouseLX, mouseLY);
        if (currentCell != null) {
            currentColor = currentCell.getMaster().getColor();
        } else {
            currentColor = 0;
        }

        if (!initialized) return;

        // Updating offset if needed
        if (keyboard.getKeys()[10]) xOff -= scrollRate;
        if (keyboard.getKeys()[11]) yOff -= scrollRate;
        if (keyboard.getKeys()[12]) xOff += scrollRate;
        if (keyboard.getKeys()[13]) yOff += scrollRate;

        if (xOff < 0) xOff = 0;
        if (xOff + client.getWidth() > width * (getCellSize() + 1) - 1)
            xOff = Math.max(width * (getCellSize() + 1) - 1 - client.getWidth(), 0);
        if (yOff < 0) yOff = 0;
        if (yOff + client.getFieldHeight() > height * (getCellSize() + 1) - 1)
            yOff = Math.max(height * (getCellSize() + 1) - 1 - client.getFieldHeight(), 0);

        // Printing current state
        switch (state) {
            case -1:
                client.statusBar[1] = "Waiting...";
                break;
            case 0:
                client.statusBar[1] = canMakeATurn ? "Your move!" : "Wait...";
                break;
            case 1:
                client.statusBar[1] = "You won!";
                break;
            case 2:
                client.statusBar[1] = "You lost...";
                break;
            case 3:
                client.statusBar[1] = "Draw.";
                break;
        }

        // Calculating number of cells that we can capture
        int availableCells = 0;
        if (currentColor != 0) {
            ArrayList<CellMaster> neighbors = owner.getMaster().getNeighbors();
            for (CellMaster master : neighbors) {
                if (master.getColor() == currentColor && master != enemy.getMaster()) {
                    availableCells += master.getCells().size();
                }
            }
        }
        client.statusBar[4] = String.valueOf(owner.getMaster().getCells().size() + (availableCells > 0 ? "(+" + availableCells + ")" : "") + " cells");

        // Making a turn if needed
        if (!canMakeATurn || state != 0) return;
        if (mouse.isClicked() && availableCells > 0) {
            connection.turn(currentColor);
        }
    }

    public void ready() {
        // Updating all masters to find borders and etc
        for (int i = 0; i < masters.size(); i++) {
            CellMaster master = masters.get(i);
            if (master.isRemoved() || master == null) {
                masters.remove(master);
                i--;
                continue;
            }
            master.update();
        }

        ready = true;
    }

    public Owner getOwner() {
        return owner;
    }

    public Owner getEnemy() {
        return enemy;
    }

    public void setCanMakeATurn(boolean state) {
        canMakeATurn = state;
    }

    public void changeState(int state) {
        this.state = state;
    }

    public void render(Screen screen) {
        if (!initialized) return;
        screen.setOffset(xOff, yOff);
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null) return; // Return if there is nothing to render
            CellMaster master = cells[i].getMaster();

            // Calculating color
            int color = Color.subtract(master.getColor(), 0xaa, 0xaa, 0xaa);
            if (currentCell == null) {
                color = master.getColor();
            } else if (master.getOwner() == owner || (master.getOwner() != enemy && owner.getMaster().isNeighbor(master) && master.getColor() == currentColor)) {
                color = currentColor;
            }

            cells[i].render(screen, color); // Rendering
        }
    }

}
