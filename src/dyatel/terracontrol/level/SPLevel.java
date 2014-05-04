package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.level.generation.FillGenerator;
import dyatel.terracontrol.level.generation.GeneratableLevel;
import dyatel.terracontrol.level.generation.Generator;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Util;
import dyatel.terracontrol.window.GameWindow;

import java.util.Random;

public class SPLevel extends BasicLevel implements GeneratableLevel {

    private Random random = Util.getRandom();

    private Generator generator;

    private int state = -1; // -1 - no state, 0 - generating, 1 - placing players, 2 - playing, 3 - won, 4 - lost, 5 - draw

    private Player[] players; // Players
    private int placedPlayers = 0; // How many players are placed
    private int currentPlayer; // Player that is making turn

    private boolean endAt50; // Game will end when someone captures more than a half of field if true

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

        generator = new FillGenerator(this);

        players = new Player[data.getInteger("players")];
        endAt50 = data.getBoolean("endAt50");

        state = 0;

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
            window.statusBar[1] = "Generated: " + generator.getGeneratedPercent() + "%";
            generator.generate(cells);
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
                        currentPlayer = random.nextInt(players.length);
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
    }

    public void onLevelGenerated() {
        state = 1;
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
        // Determining result
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