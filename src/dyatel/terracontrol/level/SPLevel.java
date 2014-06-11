package dyatel.terracontrol.level;

import dyatel.terracontrol.level.button.Button;
import dyatel.terracontrol.level.button.ButtonController;
import dyatel.terracontrol.level.button.TurnButton;
import dyatel.terracontrol.level.generation.GeneratableLevel;
import dyatel.terracontrol.level.generation.Generator;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.util.Color;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Util;
import dyatel.terracontrol.window.GameWindow;
import dyatel.terracontrol.window.Screen;

import java.util.Random;

public class SPLevel extends BasicLevel implements GeneratableLevel, TurnableLevel {

    // state: -1 - no state, 0 - generating, 1 - placing players, 2 - playing, 3 - won, 4 - lost, 5 - draw

    private Random random = Util.getRandom(); // Random

    private Generator generator; // Level generator

    private int placedPlayers = 0; // How many players are placed
    private int currentPlayer; // Player that is making turn

    private boolean endAt50; // Game will end when someone captures more than a half of field if true

    private ButtonController buttons; // Buttons for making turns
    private int currentColor; // Chosen color
    private int currentColorID; // Chosen color array index

    private int colorFading = 0; // Number to subtract from color for fading

    public SPLevel(DataArray data, GameWindow window) {
        super(window);

        init(data);
    }

    protected void preInit(DataArray data) {
        width = data.getInteger("levelWidth");
        height = data.getInteger("levelHeight");
        cells = new Cell[width * height];

        // Getting colors from data
        colors = new int[data.getInteger("colors")];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = data.getInteger("color" + i);
        }

        // Finding generator
        generator = Generator.parseGenerator(data.getString("generatorType"), this);

        players = new Player[data.getInteger("players")];
        endAt50 = data.getBoolean("endAt50");

        // Adding buttons
        buttons = new ButtonController();
        for (int i = 0; i < colors.length; i++) {
            int buttonSpacing = Button.getSize() + (Button.getHoveringSize() - Button.getSize() + 1) * 2;
            int offset = (window.getHeight() - window.getFieldHeight()) / 2;
            new TurnButton(offset + buttonSpacing * i, window.getFieldHeight() + offset, colors[i], buttons, this);
        }

        state = 0;
    }

    protected void sideUpdate() {
        // Resetting chosen color
        currentColor = 0;
        currentColorID = -1;
        buttons.update(mouseX, mouseY); // Updating buttons

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

        if (state > 2) {
            if (colorFading < 0xff) colorFading += 4;
            return;
        }

        // Level generation
        if (state == 0) generator.generate();

        if (state == 2) {
            // Checking if player has available turns
            if (!players[0].haveAvailableTurns()) {
                players[0].incrementTurns();
                currentPlayer = nextPlayer();
            }

            // AI turn
            if (currentPlayer != 0) {
                if (players[currentPlayer].haveAvailableTurns()) {
                    // Choosing best available turn
                    int max = -1;
                    int turn = -1;
                    for (int i = 0; i < colors.length; i++) {
                        int willAdd = players[currentPlayer].canCapture(i);
                        if (willAdd > max) {
                            max = willAdd;
                            turn = i;
                        }
                    }

                    // Making turn
                    players[currentPlayer].addTurn(turn);
                }
                players[currentPlayer].incrementTurns();
                currentPlayer = nextPlayer();
            }

            // Calculating number of cells that we can capture
            int availableCells = players[0].canCapture(currentColorID);
            window.statusBar[4] = String.valueOf(players[0].getMaster().getCells().size() + (availableCells > 0 ? "(+" + availableCells + ")" : "") + " cells");

            // Checking if level is captured
            int cCells = 0; // Captured cells
            for (Player player : players) {
                int cells = player.getMaster().getCells().size();
                if ((endAt50 && cells > width * height / 2) || (cCells += cells) == width * height) {
                    debug.println("Captured level!");
                    findWinner();
                    break;
                }
            }
        }

        // Managing mouse click
        if (mouse.isClicked()) {
            if (state == 1) {
                // Placing players
                CellMaster currentMaster = getMaster(mouseLX, mouseLY);
                if (currentMaster != null && currentMaster.getOwner() == null) {
                    players[placedPlayers++] = new Player(currentMaster, placedPlayers - 1, window.getConnection());
                    if (placedPlayers == players.length) {
                        currentPlayer = random.nextInt(players.length);
                        state = 2;
                    }
                }
            } else if (state == 2) {
                // Making turns
                if (currentPlayer == 0 && currentColorID != -1 && players[0].canCapture(currentColorID) > 0) {
                    players[0].addTurn(currentColorID);
                    currentPlayer = nextPlayer();
                }
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
        Player winner = null;
        for (Player player : players) {
            int cells = player.getMaster().getCells().size();
            if (cells >= max) {
                if (winner != null) winner.setIsWinner(false);
                winner = player;
                winner.setIsWinner(true);

                if (cells > max) {
                    max = cells;
                    same = 0;
                } else if (cells == max) same++;
            }
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

    public boolean isTurnAvailable(int color) {
        return players[0] != null && players[0].canCapture(getColorID(color)) > 0;
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
                if (currentColorID == -1 || state != 2) {
                    if (state > 2 && (master.getOwner() == null || !master.getOwner().isWinner())) {
                        color = Color.subtract(colors[master.getColorID()], colorFading, colorFading, colorFading);
                    } else {
                        color = colors[master.getColorID()];
                    }
                } else if (players[0] != null && (master.getOwner() == players[0] || (master.getOwner() == null && players[0].getMaster().isNeighbor(master) && master.getColorID() == currentColorID))) {
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
