package dyatel.terracontrol.level;

import dyatel.terracontrol.level.generation.GeneratableLevel;
import dyatel.terracontrol.level.generation.Generator;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.network.ServerConnection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.window.GameWindow;
import dyatel.terracontrol.window.Screen;

public class ServerLevel extends BasicLevel implements GeneratableLevel {

    // state: -1 - no state, 0 - generating, 1 - placing players, 2 - waiting for players, 3 - playing, 4 - end

    private Generator generator; // Level generator

    private int placedPlayers = 0; // How many players we have placed already

    private boolean endAt50; // True if game will end when someone captures at least 50% of level

    public ServerLevel(DataArray data, GameWindow window) {
        super(window);

        init(data);
    }

    public void init(DataArray data) {
        width = data.getInteger("levelWidth");
        height = data.getInteger("levelHeight");
        cells = new Cell[width * height];

        // Getting colors from data
        colors = new int[data.getInteger("colors")];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = data.getInteger("color" + i);
        }

        // Finding level generator
        generator = Generator.parseGenerator(data.getString("generatorType"), this);

        players = new Player[data.getInteger("players")];
        endAt50 = data.getBoolean("endAt50");

        state = 0;

        initialized = true;
    }

    protected void sideUpdate() {
        // Finding cell under mouse
        Cell currentCell = getCell(mouseLX, mouseLY);
        if (currentCell != null) {
            window.statusBar[1] = String.valueOf(currentCell.getMaster().getID());
        } else {
            window.statusBar[1] = "null";
        }

        // Printing current state
        switch (state) {
            case -1:
                window.statusBar[1] = "Waiting...";
                break;
            case 0:
                window.statusBar[1] = "Generated: " + generator.getGeneratedPercent() + "%";
                break;
            case 1:
                window.statusBar[1] = "Placing players: " + placedPlayers + "/" + players.length;
                break;
            case 2:
                int connected = 0;
                for (Player player : players) if (player.isConnected()) connected++;
                if (connected < players.length) {
                    window.statusBar[1] = "Waiting for players: " + connected + "/" + players.length;
                } else {
                    window.statusBar[1] = "Players are receiving level";
                }
                break;
            case 3:
                window.statusBar[1] = "Current player: " + ((ServerConnection) window.getConnection()).getCurrentPlayer();
                break;
            case 4:
                window.statusBar[1] = "Game end.";
                break;
        }

        // Level generation
        if (state == 0) generator.generate(cells);

        // Placing players
        if (mouse.isClicked() && state == 1) {
            CellMaster currentMaster = getMaster(mouseLX, mouseLY);
            if (currentMaster != null && currentMaster.getOwner() == null) {
                players[placedPlayers++] = new Player(currentMaster, placedPlayers - 1, window.getConnection());

                if (placedPlayers == players.length) {
                    ((ServerConnection) window.getConnection()).createPlayers(players);
                    state = 2;
                }
            }
        }

        if (state == 3) {
            // Checking if level is captured
            int cCells = 0; // Captured cells
            for (Player player : players) {
                int cells = player.getMaster().getCells().size();
                if ((endAt50 && cells > width * height / 2) || (cCells += cells) == width * height) {
                    debug.println("Captured level!");
                    ((ServerConnection) window.getConnection()).gameOver();
                    state = 4;
                    return;
                }
            }
        }
    }

    public void onLevelGenerated() {
        state = 1;
    }

    public boolean isGenerated() {
        return state > 0;
    }

    public Cell[] getCells() {
        return cells;
    }

    public void preRender(Screen screen) {
        screen.setOffset(xOff, yOff);

        // Render
        int yStart = yOff / (getCellSize() + 1);
        int yEnd = Math.min(yStart + window.getFieldHeight() / ((getCellSize() + 1) - 1) + 1, height); // Restricting max y to height
        for (int y = yStart; y < yEnd; y++) {
            int xStart = xOff / (getCellSize() + 1);
            int xEnd = Math.min(xStart + window.getWidth() / ((getCellSize() + 1) - 1) + 1, width); // Restricting max x to width
            for (int x = xStart; x < xEnd; x++) {
                if (cells[x + y * width] == null) continue; // Return if there is nothing to render
                cells[x + y * width].render(screen, colors[getMaster(x, y).getColorID()]); // Rendering
            }
        }
    }

    public void postRender(Screen screen) {

    }
}
