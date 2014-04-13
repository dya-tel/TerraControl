package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.Server;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.util.Util;

import java.util.Random;

public class ServerLevel extends Level {

    private Server server;

    private Random random = Util.getRandom();

    private boolean generated = false;
    private boolean captured = false;

    private long genStart;

    public ServerLevel(int width, int height, int cellSize, boolean fastGeneration, Server server) {
        super(cellSize, Debug.serverDebug);

        this.server = server;

        keyboard = server.getKeyboard();
        mouse = server.getMouse();
        mouse.setLevel(this);

        this.width = width;
        this.height = height;

        cells = new Cell[width * height];

        colors = Server.colors;

        genStart = System.currentTimeMillis();
        if (fastGeneration) {
            debug.println("Using fast generation...");
            // Fill field with masters
            for (int i = 0; i < cells.length; i++) {
                new Cell(i % width, i / width, new CellMaster(this));
            }
        } else {
            debug.println("Using standard generation...");
            addMasters(width * height * 4 / 5, width * height * 5 / 5); // Standard generation
        }
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

    public void update() {
        // Updating key delay, getting key state
        if (keyDelay > -1) keyDelay--;
        boolean[] keys = keyboard.getKeys();

        // Updating mouse
        mouseX = Math.min(mouse.getX() / (getCellSize() + 1), mouse.getX());
        mouseY = Math.min(mouse.getY() / (getCellSize() + 1), mouse.getY());
        mouseLX = Math.min((mouse.getX() + xOff) / (getCellSize() + 1), mouse.getX());
        mouseLY = Math.min((mouse.getY() + yOff) / (getCellSize() + 1), mouse.getY());

        Cell currentCell = getCell(mouseLX, mouseLY);
        if (currentCell != null) {
            server.statusBar[1] = String.valueOf(currentCell.getMaster().getID());
        } else {
            server.statusBar[1] = "null";
        }
        server.statusBar[2] = mouseLX + " " + mouseLY;

        // Updating tick rate
        if (keys[7] && keyDelay == -1) {
            keyDelay = 10;
            delay++;
        }
        if (keys[8] && keyDelay == -1) {
            keyDelay = 10;
            if (delay > 1) delay--;
        }

        // Updating offset if needed
        if (keys[10]) xOff -= scrollRate;
        if (keys[11]) yOff -= scrollRate;
        if (keys[12]) xOff += scrollRate;
        if (keys[13]) yOff += scrollRate;

        if (xOff < 0) xOff = 0;
        if (xOff + server.getWidth() > width * (getCellSize() + 1) - 1)
            xOff = Math.max(width * (getCellSize() + 1) - 1 - server.getWidth(), 0);
        if (yOff < 0) yOff = 0;
        if (yOff + server.getFieldHeight() > height * (getCellSize() + 1) - 1)
            yOff = Math.max(height * (getCellSize() + 1) - 1 - server.getFieldHeight(), 0);

        // Updating all the things
        while (needUpdate.size() > 0) {
            Updatable u = needUpdate.get(0);
            if (!u.isRemoved()) {
                u.update();
            } else {
                if (u instanceof CellMaster) masters.remove(u);
            }
            needUpdate.remove(0);
        }

        // Checking and showing generation progress
        if (!generated) {
            int gen = 0;
            for (int i = 0; i < width * height; i++) {
                if (cells[i] != null) gen++;
            }
            if (gen != width * height) {
                server.statusBar[0] = "Generated: " + gen * 100 / (width * height) + "%";
            } else {
                debug.println("Generated level in " + (System.currentTimeMillis() - genStart) + " ms");
                server.statusBar[0] = "";

                int tempC = 0;
                for (CellMaster master : masters) {
                    tempC += master.getCells().size();
                    master.setID(masters.indexOf(master));
                }
                debug.println("Checking cells... " + ((tempC == width * height) ? "OK" : "Failed: " + tempC + "/" + width * height));

                server.getConnection().createPlayers();

                generated = true;
                ready();
            }
        }

        if (generated && captured) return;

        // Slow generation if needed
        if (timer > 0) timer--;
        if (!generated && timer == 0) {
            for (CellMaster master : masters) master.generate();
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

    public void render(Screen screen) {
        screen.setOffset(xOff, yOff);

        // Render
        int yStart = yOff / (getCellSize() + 1);
        int yEnd = Math.min(yStart + server.getFieldHeight() / ((getCellSize() + 1) - 1) + 1, height); // Restricting max y to height
        for (int y = yStart; y < yEnd; y++) {
            int xStart = xOff / (getCellSize() + 1);
            int xEnd = Math.min(xStart + server.getWidth() / ((getCellSize() + 1) - 1) + 1, width); // Restricting max x to width
            for (int x = xStart; x < xEnd; x++) {
                if (cells[x + y * width] == null) continue; // Return if there is nothing to render
                cells[x + y * width].render(screen, getMaster(x, y).getColor()); // Rendering
            }
        }
    }

    public boolean isGenerated() {
        return generated;
    }

}
