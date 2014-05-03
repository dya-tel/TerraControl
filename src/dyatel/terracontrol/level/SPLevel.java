package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Util;
import dyatel.terracontrol.window.GameWindow;

import java.util.Random;

public class SPLevel extends Level {

    private Random random = Util.getRandom();

    private int state = -1; // -1 - no state, 0 - generating, 1 - placing players, 2 - playing, 3 - won, 4 - lost, 5 - draw

    private Player[] players; // Players
    private int placedPlayers = 0; // How many players are placed
    private int currentPlayer; // Player that is making turn

    private boolean endAt50; // Game will end when someone captures more than a half of field if true

    private long genStart; // Level generation start time

    private int timer = 0; // Update counter
    private int delay = 2; // Skipped updates per generation (slow generator only)

    private Cell currentCell; // Cell player is pointing on
    private int currentColorID; // Its place in array
    public int currentColor; // Its color

    public SPLevel(DataArray data, GameWindow window) {
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

        state = 0;

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
                window.statusBar[1] = currentPlayer == 0 ? "Your move!" : "Wait...";
                break;
            case 3:
                window.statusBar[1] = "You won!";
                break;
            case 4:
                window.statusBar[1] = "You lost...";
                break;
            case 5:
                window.statusBar[1] = "Draw.";
                break;
        }

        // Level generation
        if (state == 0) {
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
                state = 1;
            }

            // Slow generation if needed
            if (timer > 0) timer--;
            if (timer == 0) {
                for (CellMaster master : masters) master.generate();
                timer = delay;
            }
        }

        // Making turns
        if (state == 2 && currentPlayer != 0) {
            // Choosing best available turn
            int max = -1;
            int turn = -1;
            for (int i = 0; i < colors.length; i++) {
                int willAdd = willCapture(players[currentPlayer], i);
                if (willAdd > max) {
                    max = willAdd;
                    turn = i;
                }
            }

            // Making turn
            players[currentPlayer].addTurn(turn);
            currentPlayer = nextPlayer();
        }

        if (mouse.isClicked()) {
            if (state == 1) {
                CellMaster currentMaster = getMaster(mouseLX, mouseLY);
                if (currentMaster != null && currentMaster.getOwner() == null) {
                    players[placedPlayers++] = new Player(currentMaster, placedPlayers - 1, window.getConnection());
                    if (placedPlayers == players.length) {
                        currentPlayer = 0;//random.nextInt(players.length);
                        state = 2;
                    }
                }
            } else if (state == 2) {
                if (currentPlayer == 0 && currentColorID != -1 && willCapture(players[0], currentColorID) > 0) {
                    players[0].addTurn(currentColorID);
                    currentPlayer = nextPlayer();
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
                findWinner();
                return;
            }
        }

        /*
        // Find winner
        int max = -1; // Max captured cells
        int same = 0; // Needed to determine draw
        for (int i = 0; i < players.length; i++) {
            int cells = players[i].getMaster().getCells().size();
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

        */
    }

    private int nextPlayer() {
        return currentPlayer == players.length - 1 ? 0 : currentPlayer + 1;
    }

    private void findWinner() {
        // Find winner
        int max = -1; // Max captured cells
        int same = 0; // Needed to determine draw
        for (int i = 0; i < players.length; i++) {
            int cells = players[i].getMaster().getCells().size();
            if (cells > max) {
                max = cells;
                same = 0;
            } else if (cells == max) same++;
        }
        // Send result to every player

        int cells = players[0].getMaster().getCells().size(); // Player`s cells
        if (cells < max) {
            state = 4;
        } else if (cells == max) {
            if (same == 0)
                state = 3;
            else
                state = 5;
        }

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
