package dyatel.terracontrol.level;

import dyatel.terracontrol.level.button.Button;
import dyatel.terracontrol.level.button.ButtonController;
import dyatel.terracontrol.level.button.TurnButton;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.util.Color;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.window.GameWindow;
import dyatel.terracontrol.window.Screen;

import java.util.ArrayList;

public class ClientLevel extends BasicLevel implements TurnableLevel {

    // state: -1 - waiting, 0 - playing, 1 - won, 2 - lost, 3 - draw

    protected int playerID; // Client`s player ID

    private ButtonController buttons; // Buttons for making turns
    private int currentColor; // Chosen color
    private int currentColorID; // Chosen color array index

    protected boolean needToMakeATurn = false; // True if it is client`s turn

    private int colorFading = 0; // Number to subtract from color for fading

    public ClientLevel(GameWindow window) {
        super(window);
    }

    protected void preInit(DataArray data) {
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

        // Adding buttons
        buttons = new ButtonController();
        for (int i = 0; i < colors.length; i++) {
            int buttonSpacing = Button.getSize() + (Button.getHoveringSize() - Button.getSize() + 1) * 2;
            int offset = (window.getHeight() - window.getFieldHeight()) / 2;
            new TurnButton(offset + buttonSpacing * i, window.getFieldHeight() + offset, colors[i], buttons, this);
        }
    }

    protected void sideUpdate() {
        // Resetting chosen color
        currentColor = 0;
        currentColorID = -1;
        buttons.update(mouseX, mouseY); // Updating buttons

        // Printing current state
        switch (state) {
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

        if (state > 0) {
            if (colorFading < 0xff) colorFading += 4;
            return;
        }

        // Printing sent/received data in the status bar
        window.statusBar[5] = window.getConnection().getTraffic();

        // Calculating number of cells that we can capture
        int availableCells = players[playerID].canCapture(currentColorID);
        window.statusBar[4] = String.valueOf(players[playerID].getMaster().getCells().size() + (availableCells > 0 ? "(+" + availableCells + ")" : "") + " cells");

        // Making a turn if needed
        if (needToMakeATurn && state == 0 && mouse.isClicked() && availableCells > 0) {
            players[playerID].addTurn(currentColorID);
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
        if (players[playerID].haveAvailableTurns())
            needToMakeATurn = true;
        else
            players[playerID].incrementTurns();
    }

    public boolean isTurnAvailable(int color) {
        return players[playerID] != null && players[playerID].canCapture(getColorID(color)) > 0;
    }

    public void highlightTurn(int color) {
        currentColor = color;
        currentColorID = getColorID(color);
    }

    public void preRender(Screen screen) {
        if (!initialized) return;

        screen.setOffset(xOff, yOff);

        // Render
        int yStart = Math.max(yOff / (getCellSize() + 1), 0); // Restricting min y to 0
        int yEnd = Math.min(yStart + window.getFieldHeight() / ((getCellSize() + 1) - 1) + 1, height); // Restricting max y to height
        for (int y = yStart; y < yEnd; y++) {
            int xStart = Math.max(xOff / (getCellSize() + 1), 0); // Restricting min x to 0
            int xEnd = Math.min(xStart + window.getWidth() / ((getCellSize() + 1) - 1) + 1, width); // Restricting max x to width
            for (int x = xStart; x < xEnd; x++) {
                if (cells[x + y * width] == null) continue; // Return if there is nothing to render
                CellMaster master = getMaster(x, y);

                // Calculating color
                int color = Color.subtract(colors[master.getColorID()], 0xaa, 0xaa, 0xaa);
                if (currentColorID == -1 || state != 0) {
                    if (state > 0 && (master.getOwner() == null || !master.getOwner().isWinner())) {
                        color = Color.subtract(colors[master.getColorID()], colorFading, colorFading, colorFading);
                    } else {
                        color = colors[master.getColorID()];
                    }
                } else if (master.getOwner() == players[playerID] || (master.getOwner() == null && players[playerID].getMaster().isNeighbor(master) && master.getColorID() == currentColorID)) {
                    color = currentColor;
                }

                cells[x + y * width].render(screen, color); // Rendering
            }
        }
    }

    public void postRender(Screen screen) {
        if (!initialized) return;

        buttons.render(screen);
    }
}
