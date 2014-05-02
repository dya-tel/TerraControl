package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.network.ServerConnection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Util;
import dyatel.terracontrol.window.GameWindow;

import java.util.Random;

public class ServerLevel extends Level {

    private Random random = Util.getRandom();

    private Player[] players;
    private int placedPlayers = 0;

    private boolean endAt50;

    private boolean generated = false;
    private boolean placingPlayers = false;
    private boolean captured = false;

    private long genStart; // Level generation start time

    private int timer = 0; // Update counter
    private int delay = 2; // Skipped updates per generation (slow generator only)

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

        // Choosing generation way
        genStart = System.currentTimeMillis();
        if (data.getBoolean("fastGeneration")) {
            debug.println("Using fast generation...");
            // Fill field with masters
            for (int i = 0; i < cells.length; i++) {
                new Cell(i % width, i / width, new CellMaster(this));
            }
        } else {
            debug.println("Using standard generation...");
            addMasters(width * height * 4 / 5, width * height * 5 / 5); // Standard generation
        }

        players = new Player[data.getInteger("players")];
        endAt50 = data.getBoolean("endAt50");

        initialized = true;
    }

    private void addMasters(int min, int max) {
        int masters = random.nextInt(max - min + 1) + min;
        debug.println("Going to add " + masters + " masters");
        for (int i = 0; i < masters; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (getCell(x, y) == null) {
                new Cell(x, y, new CellMaster(this));
            }
        }
        debug.println("Added " + this.masters.size() + " masters");
    }

    protected void sideUpdate() {
        Cell currentCell = getCell(mouseLX, mouseLY);
        if (currentCell != null) {
            window.statusBar[1] = String.valueOf(currentCell.getMaster().getID());
        } else {
            window.statusBar[1] = "null";
        }
        window.statusBar[2] = mouseLX + " " + mouseLY;

        // Updating tick rate
        if (keys[7] && keyDelay == -1) {
            keyDelay = 10;
            delay++;
        }
        if (keys[8] && keyDelay == -1) {
            keyDelay = 10;
            if (delay > 1) delay--;
        }

        // Level generation
        if (!generated) {
            // Checking progress
            int gen = 0;
            for (int i = 0; i < width * height; i++) {
                if (cells[i] != null) gen++;
            }
            if (gen != width * height) {
                window.statusBar[0] = "Generated: " + gen * 100 / (width * height) + "%";
            } else {
                debug.println("Generated level in " + (System.currentTimeMillis() - genStart) + " ms");

                int tempC = 0;
                for (CellMaster master : masters) {
                    tempC += master.getCells().size();
                    master.setID(masters.indexOf(master));
                }
                debug.println("Checking cells... " + ((tempC == width * height) ? "OK" : "Failed: " + tempC + "/" + width * height));

                window.statusBar[0] = "Place players: 0/" + players.length;
                placingPlayers = true;

                generated = true;
            }

            // Slow generation if needed
            if (timer > 0) timer--;
            if (timer == 0) {
                for (CellMaster master : masters) master.generate();
                timer = delay;
            }
        }

        if (mouse.isClicked() && placingPlayers) {
            CellMaster currentMaster = getMaster(mouseLX, mouseLY);
            if (currentMaster != null && currentMaster.getOwner() == null) {
                players[placedPlayers++] = new Player(currentMaster, placedPlayers - 1, window.getConnection());
                window.statusBar[0] = "Place players: " + placedPlayers + "/" + players.length;

                if (placedPlayers == players.length) {
                    ((ServerConnection) window.getConnection()).createPlayers(players);
                    placingPlayers = false;
                }
            }
        }

        if (!generated || captured) return;

        // Checking if level is captured
        int cCells = 0; // Captured cells
        for (Player player : players) {
            int cells = player.getMaster().getCells().size();
            if ((endAt50 && cells > width * height / 2) || (cCells += cells) == width * height) {
                debug.println("Captured level!");
                ((ServerConnection) window.getConnection()).gameOver();
                captured = true;
                return;
            }
        }
    }

    public boolean isGenerated() {
        return generated;
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
