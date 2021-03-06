package dyatel.terracontrol.level;

import dyatel.terracontrol.input.Keyboard;
import dyatel.terracontrol.input.Mouse;
import dyatel.terracontrol.network.Player;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.window.GameWindow;
import dyatel.terracontrol.window.Screen;

import java.util.ArrayList;

public abstract class BasicLevel implements Level {

    protected GameWindow window; // Main window
    protected Debug debug; // Output

    protected int state = -1; // See values in inherited classes

    protected boolean initialized = false; // Is level initialized

    protected int xOff, yOff; // Level offset
    protected int scrollRate = 10; // Pixels per update

    protected int cellSize; // Cell side in pixels
    protected double zoom; // Zoom
    protected double zoomStep = 0.5; // Zoom change

    protected int width, height; // Level size in cells

    protected Keyboard keyboard; // Keyboard listener
    protected int keyDelay; // Timer that restricts pressing a key every update (60 times per second!)
    protected boolean[] keys; // Keyboard keys state

    protected Mouse mouse; // Mouse listener
    protected int mouseX, mouseY; // Real mouse coordinates based on position in window
    protected int mouseLX, mouseLY; // Mouse coordinates used on level

    protected ArrayList<CellMaster> masters; // List of masters
    protected ArrayList<Updatable> needUpdate; // List of updatable objects that want update

    protected Cell[] cells; // Field

    protected Player[] players; // Players

    protected int[] colors; // Available colors for cells

    protected BasicLevel(GameWindow window) {
        this.window = window;
        debug = window.getDebug();

        // Getting and initializing input
        keyboard = window.getKeyboard();
        mouse = window.getMouse();
        mouse.setLevel(this);

        masters = new ArrayList<CellMaster>();
        needUpdate = new ArrayList<Updatable>();
    }

    public final void init(DataArray data) {
        preInit(data);

        // Zooming level to match window size
        cellSize = 2;
        zoom = zoomStep;
        while (getFieldWidth() <= window.getWidth() && getFieldHeight() <= window.getFieldHeight()) {
            zoom += zoomStep;
        }
        if (zoom > zoomStep) zoom -= zoomStep;

        // Finding right offset
        correctOffset();

        initialized = true;
    }

    protected abstract void preInit(DataArray data);

    public final void update() {
        // Updating input
        // Updating key delay, getting key state
        if (keyDelay > -1) keyDelay--;
        keys = keyboard.getKeys();

        // Updating mouse coordinates and printing them
        mouseX = mouse.getX();
        mouseY = mouse.getY();
        if (mouseX > -1 && mouseY > -1 && mouseY < window.getFieldHeight()) {
            mouseLX = (mouseX + xOff) / (getCellSize() + 1);
            mouseLY = (mouseY + yOff) / (getCellSize() + 1);
        } else {
            // If mouse is out of bounds
            mouseLX = -1;
            mouseLY = -1;
        }
        window.statusBar[2] = mouseLX + " " + mouseLY;

        // Updating offset if needed
        if (keys[Keyboard.KEY_LEFT]) changeXOff(-scrollRate);
        if (keys[Keyboard.KEY_UP]) changeYOff(-scrollRate);
        if (keys[Keyboard.KEY_RIGHT]) changeXOff(scrollRate);
        if (keys[Keyboard.KEY_DOWN]) changeYOff(scrollRate);

        // Changing zoom by keyboard
        if (keys[Keyboard.KEY_PLUS]) changeZoom(1);
        if (keys[Keyboard.KEY_MINUS]) changeZoom(-1);

        if (!initialized) return;

        // Update-on-demand
        while (needUpdate.size() > 0) {
            Updatable u = needUpdate.get(0);
            if (u != null && !u.isRemoved()) {
                u.update();
            } else {
                if (u instanceof CellMaster) masters.remove(u);
            }
            needUpdate.remove(0);
        }

        // Server/client update
        sideUpdate();
    }

    // One-side update, specific for client and server
    protected abstract void sideUpdate();

    public abstract void preRender(Screen screen);

    public abstract void postRender(Screen screen);

    public boolean canSetCell(int x, int y) {
        // If this coordinates belong to level and there is no cell, returning true
        return x >= 0 && x < width && y >= 0 && y < height && getCell(x, y) == null;
    }

    public void setCell(Cell cell) {
        cells[cell.getX() + cell.getY() * width] = cell;
    }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null; // If out of bounds
        }
        return cells == null ? null : cells[x + y * width];
    }

    public CellMaster getMaster(int x, int y) {
        // Safe method to get masters without checking cells
        if (getCell(x, y) == null) return null;
        return getCell(x, y).getMaster();
    }

    public void add(CellMaster u) {
        masters.add(u);
    }

    public void needUpdate(Updatable u) {
        if (!needUpdate.contains(u)) needUpdate.add(u);
    }

    public int getCellSize() {
        return (int) (cellSize * zoom);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ArrayList<CellMaster> getMasters() {
        return masters;
    }

    public int[] getColors() {
        return colors;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getColorID(int color) {
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == color) return i;
        }
        return -1;
    }

    private int getFieldWidth() {
        return width * (getCellSize() + 1) - 1; // Returns field width in pixels
    }

    private int getFieldHeight() {
        return height * (getCellSize() + 1) - 1; // Returns field height in pixels
    }

    public void changeXOff(int dx) {
        // Changing offset without going out of bounds
        xOff += dx;
        correctOffset();
    }

    public void changeYOff(int dy) {
        // Changing offset without going out of bounds
        yOff += dy;
        correctOffset();
    }

    private void correctOffset() {
        // Checking bounds and if field fits on the screen, centring it
        int minXOff = Math.min((window.getWidth() - getFieldWidth()) / -2, 0);
        int maxXOff = Math.max(getFieldWidth() - window.getWidth(), minXOff);
        if (xOff < minXOff) xOff = minXOff;
        if (xOff > maxXOff) xOff = maxXOff;

        int minYOff = Math.min((window.getFieldHeight() - getFieldHeight()) / -2, 0);
        int maxYOff = Math.max(getFieldHeight() - window.getFieldHeight(), minYOff);
        if (yOff < minYOff) yOff = minYOff;
        if (yOff > maxYOff) yOff = maxYOff;
    }

    public void changeZoom(int n) {
        // Checking if field fits and we don`t need to zoom it further
        if (n == -1 && getFieldWidth() <= window.getWidth() && getFieldHeight() <= window.getFieldHeight()) return;

        double pZoom = zoom; // Saving previous zoom
        zoom += n * zoomStep;

        // Checking if zoomed too much
        if (getCellSize() < 1) {
            zoom = pZoom;
            return;
        }

        double diff = ((cellSize * zoom) - (cellSize * pZoom)); // Cell size change
        // How many cells changed their size
        int cellsX = (int) ((xOff + window.getWidth() / 2) / ((cellSize * pZoom) + 1));
        int cellsY = (int) ((yOff + window.getFieldHeight() / 2) / ((cellSize * pZoom) + 1));
        changeXOff((int) (cellsX * diff)); // Centring x offset
        changeYOff((int) (cellsY * diff)); // Centring y offset
    }

    public GameWindow getWindow() {
        return window;
    }

    public Debug getDebug() {
        return debug;
    }

}
