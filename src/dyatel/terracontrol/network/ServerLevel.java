package dyatel.terracontrol.network;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.Server;
import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.Level;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.util.Util;

import java.util.Random;

public class ServerLevel extends Level {

    private Server server;

    private Random random = Util.getRandom();

    private boolean generated = false;
    private boolean captured = false;

    private static int minMasters, maxMasters;

    public ServerLevel(int width, int height, int cellSize, boolean fastGeneration, Server server) {
        super(cellSize, Debug.serverDebug);

        this.server = server;

        keyboard = server.getKeyboard();
        mouse = server.getMouse();
        mouse.setLevel(this);

        this.width = width;
        this.height = height;

        cells = new Cell[width * height];

        minMasters = width * height * 4 / 5;
        maxMasters = width * height * 5 / 5;

        addMasters();

        if (fastGeneration) {
            debug.println("Using fast generation...");
            long start = System.currentTimeMillis();
            boolean generated = false;
            while (!generated) {
                int gen = 0;
                for (int i = 0; i < width * height; i++) {
                    if (cells[i] != null) gen++;
                }
                if (gen == width * height) {
                    generated = true;
                } else {
                    generate();
                }
            }
            debug.println("Generated level in " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    private void addMasters() {
        int masters = random.nextInt(maxMasters - minMasters + 1) + minMasters;
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

    public void update() {
        // Updating key delay
        if (keyDelay > -1) keyDelay--;

        // Updating mouse
        mouseX = Math.min(mouse.getX() / (cellSize + 1), mouse.getX());
        mouseY = Math.min(mouse.getY() / (cellSize + 1), mouse.getY());
        mouseLX = Math.min((mouse.getX() + xOff) / (cellSize + 1), mouse.getX());
        mouseLY = Math.min((mouse.getY() + yOff) / (cellSize + 1), mouse.getY());

        Cell currentCell = getCell(mouseLX, mouseLY);
        if (currentCell != null) {
            server.statusBar[1] = String.valueOf(currentCell.getMaster().getID());
        } else {
            server.statusBar[1] = "null";
        }
        server.statusBar[2] = mouseLX + " " + mouseLY;

        // Updating tick rate
        if (keyboard.getKeys()[7] && keyDelay == -1) {
            keyDelay = 10;
            delay++;
        }
        if (keyboard.getKeys()[8] && keyDelay == -1) {
            keyDelay = 10;
            if (delay > 1) delay--;
        }

        // Updating offset if needed
        if (keyboard.getKeys()[10]) xOff -= scrollRate;
        if (keyboard.getKeys()[11]) yOff -= scrollRate;
        if (keyboard.getKeys()[12]) xOff += scrollRate;
        if (keyboard.getKeys()[13]) yOff += scrollRate;

        if (xOff < 0) xOff = 0;
        if (xOff + server.getWidth() > width * (getCellSize() + 1) - 1)
            xOff = Math.max(width * (getCellSize() + 1) - 1 - server.getWidth(), 0);
        if (yOff < 0) yOff = 0;
        if (yOff + server.getFieldHeight() > height * (getCellSize() + 1) - 1)
            yOff = Math.max(height * (getCellSize() + 1) - 1 - server.getFieldHeight(), 0);

        // Checking and showing generation progress
        if (!generated) {
            int gen = 0;
            for (int i = 0; i < width * height; i++) {
                if (cells[i] != null) gen++;
            }
            if (gen != width * height) {
                server.statusBar[0] = "Generated: " + gen * 100 / (width * height) + "%";
            } else {
                server.statusBar[0] = "";
                debug.println("Generated level!");

                int tempC = 0;
                for (CellMaster master : masters) {
                    tempC += master.getCells().size();
                    master.setID(masters.indexOf(master));
                }

                debug.println("Checking cells... " + (tempC == width * height) + ": " + tempC + " of " + width * height);
                if (tempC != width * height) {
                    debug.println(tempC < width * height ? ("Lost " + (width * height - tempC) + " cells.") : ("Found " + (tempC - width * height) + " immigrant cells."));
                }

                server.getConnection().createPlayers();

                generated = true;
                ready();
            }
        }

        if (generated && captured) return;

        // Slow generation if needed
        if (timer > 0) timer--;
        if (!generated && timer == 0) {
            generate();
            timer = delay;
        }

        // Checking if level is captured
        for (Cell cell : cells) {
            if (cell == null || cell.getMaster().getOwner() == null) {
                return;
            }
        }
        debug.println("Captured level!");
        server.getConnection().gameOver();
        captured = true;
    }

    private void generate() {
        for (int i = 0; i < masters.size(); i++) {
            CellMaster master = masters.get(i);
            // Removing removed
            if (master.isRemoved()) {
                masters.remove(master);
                i--;
                continue;
            }
            // Generating level
            master.generate();
        }
    }

    public void render(Screen screen) {
        screen.setOffset(xOff, yOff);
        for (Cell cell : cells) {
            if (cell == null) continue;
            cell.render(screen, cell.getMaster().getColor());
        }
    }

    public boolean isGenerated() {
        return generated;
    }

}
