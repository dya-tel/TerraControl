package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.util.Color;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.window.GameWindow;

import java.util.ArrayList;

public class ClientLevel extends BasicLevel {

    // state: -1 - waiting, 0 - playing, 1 - won, 2 - lost, 3 - draw

    protected int playerID; // Client`s player ID

    protected Cell currentCell; // Cell player is pointing on
    protected int currentColorID; // Color ID of currentCell
    public int currentColor; // Color of currentCell

    protected boolean needToMakeATurn = false; // True if it is client`s turn

    public ClientLevel(int cellSize, GameWindow window) {
        super(cellSize, window);
    }

    public void init(DataArray data) {
        width = data.getInteger("levelWidth");
        height = data.getInteger("levelHeight");
        cells = new Cell[width * height];

        // Creating masters
        masters = new ArrayList<CellMaster>();
        for (int i = 0; i < data.getInteger("masters"); i++) {
            new CellMaster(0xffffffff, this);
        }

        // Placing players
        players = new Player[data.getInteger("players")];
        playerID = data.getInteger("playerID");
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(masters.get(data.getInteger("player" + i)), i, window.getConnection());
        }

        // Getting colors from data
        colors = new int[data.getInteger("colors")];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = data.getInteger("color" + i);
        }

        initialized = true;
    }

    protected void sideUpdate() {
        // Getting cell and color under mouse
        currentCell = getCell(mouseLX, mouseLY);
        if (currentCell != null) {
            currentColorID = currentCell.getMaster().getColorID();
            currentColor = colors[currentCell.getMaster().getColorID()];
        } else {
            currentColorID = -1;
            currentColor = 0;
        }
        window.statusBar[2] = mouseLX + " " + mouseLY;

        // Changing zoom by keyboard
        if (keys[7]) changeZoom(1);
        if (keys[8]) changeZoom(-1);

        // Printing current state
        switch (state) {
            case -1:
                window.statusBar[1] = "Waiting...";
                break;
            case 0:
                window.statusBar[1] = needToMakeATurn ? "Your move!" : "Wait...";
                break;
            case 1:
                window.statusBar[1] = "You won!";
                break;
            case 2:
                window.statusBar[1] = "You lost...";
                break;
            case 3:
                window.statusBar[1] = "Draw.";
                break;
        }

        // Calculating number of cells that we can capture
        int availableCells = willCapture(players[playerID], currentColorID);
        window.statusBar[4] = String.valueOf(players[playerID].getMaster().getCells().size() + (availableCells > 0 ? "(+" + availableCells + ")" : "") + " cells");

        // Making a turn if needed
        if (needToMakeATurn && state == 0 && mouse.isClicked() && availableCells > 0) {
            players[playerID].addTurn(getColorID(currentColor));
            needToMakeATurn = false;
        }
    }

    public void ready() {
        for (CellMaster master : masters) needUpdate(master); // Updating all masters to find borders and etc
    }

    public Player getClientPlayer() {
        return players[playerID];
    }

    public Player getPlayer(int id) {
        return players[id];
    }

    public int getPlayers() {
        return players.length;
    }

    public void needTurn() {
        needToMakeATurn = true;
    }

    public void render(Screen screen) {
        if (!initialized) return;
        screen.setOffset(xOff, yOff);

        // Render
        int yStart = yOff / (getCellSize() + 1);
        int yEnd = Math.min(yStart + window.getFieldHeight() / ((getCellSize() + 1) - 1) + 1, height); // Restricting max y to height
        for (int y = yStart; y < yEnd; y++) {
            int xStart = xOff / (getCellSize() + 1);
            int xEnd = Math.min(xStart + window.getWidth() / ((getCellSize() + 1) - 1) + 1, width); // Restricting max x to width
            for (int x = xStart; x < xEnd; x++) {
                if (cells[x + y * width] == null) continue; // Return if there is nothing to render
                CellMaster master = getMaster(x, y);

                // Calculating color
                int color = Color.subtract(colors[master.getColorID()], 0xaa, 0xaa, 0xaa);
                if (currentCell == null) {
                    color = colors[master.getColorID()];
                } else if (master.getOwner() == players[playerID] || (master.getOwner() == null && players[playerID].getMaster().isNeighbor(master) && master.getColorID() == currentColorID)) {
                    color = currentColor;
                }

                cells[x + y * width].render(screen, color); // Rendering
            }
        }
    }

}
