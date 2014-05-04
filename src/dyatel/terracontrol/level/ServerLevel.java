package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.level.generation.GeneratableLevel;
import dyatel.terracontrol.level.generation.Generator;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.network.ServerConnection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.window.GameWindow;

public class ServerLevel extends BasicLevel implements GeneratableLevel {

    private Generator generator;

    private int state = -1; // -1 - no state, 0 - generating, 1 - placing players, 2 - playing, 3 - end

    private Player[] players;
    private int placedPlayers = 0;

    private boolean endAt50;

    public ServerLevel(DataArray data, GameWindow window) {
        super(data.getInteger("cellSize"), window);

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

        generator = Generator.parseGenerator(data.getString("generatorType"), this);

        players = new Player[data.getInteger("players")];
        endAt50 = data.getBoolean("endAt50");

        state = 0;

        initialized = true;
    }

    protected void sideUpdate() {
        Cell currentCell = getCell(mouseLX, mouseLY);
        if (currentCell != null) {
            window.statusBar[1] = String.valueOf(currentCell.getMaster().getID());
        } else {
            window.statusBar[1] = "null";
        }
        window.statusBar[2] = mouseLX + " " + mouseLY;

        // Printing current state
        switch (state) {
            case -1:
                window.statusBar[1] = "Waiting...";
                break;
            // case 0 is managed in level generation for now
            case 1:
                window.statusBar[1] = "Placing players: " + placedPlayers + "/" + players.length;
                break;
            case 2:
                window.statusBar[1] = "Current player: " + ((ServerConnection) window.getConnection()).getCurrentPlayer();
                break;
            case 3:
                window.statusBar[1] = "Game end.";
                break;
        }

        // Level generation
        if (state == 0) {
            window.statusBar[1] = "Generated: " + generator.getGeneratedPercent() + "%";
            generator.generate(cells);
        }

        if (mouse.isClicked() && state == 1) {
            CellMaster currentMaster = getMaster(mouseLX, mouseLY);
            if (currentMaster != null && currentMaster.getOwner() == null) {
                players[placedPlayers++] = new Player(currentMaster, placedPlayers - 1, window.getConnection());
                window.statusBar[0] = "Place players: " + placedPlayers + "/" + players.length;

                if (placedPlayers == players.length) {
                    ((ServerConnection) window.getConnection()).createPlayers(players);
                    state = 2;
                }
            }
        }

        if (state != 2) return;

        // Checking if level is captured
        int cCells = 0; // Captured cells
        for (Player player : players) {
            int cells = player.getMaster().getCells().size();
            if ((endAt50 && cells > width * height / 2) || (cCells += cells) == width * height) {
                debug.println("Captured level!");
                ((ServerConnection) window.getConnection()).gameOver();
                state = 3;
                return;
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

    public void render(Screen screen) {
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

}
