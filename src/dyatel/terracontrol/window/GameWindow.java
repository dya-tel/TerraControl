package dyatel.terracontrol.window;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.input.Keyboard;
import dyatel.terracontrol.input.Mouse;
import dyatel.terracontrol.level.Level;
import dyatel.terracontrol.network.Connection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.util.ErrorLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public abstract class GameWindow extends Canvas implements Runnable {

    protected Thread thread; // Main game thread
    protected boolean running; // If false, the game will close

    protected Debug debug; // Output

    protected JFrame frame; // Actual window
    protected int width, height; // Sizes of window
    protected final String title; // What will be placed in title after "TerraControl", space is needed

    protected Screen screen; // Renderer
    protected Keyboard keyboard; // Keyboard input manager
    protected Mouse mouse; // Mouse input manager
    protected Level level; // Level
    protected Connection connection; // Connection manager

    public String[] statusBar = {"", "", "", "", "", ""}; // Output on bottom panel

    protected static final int ups = 30; // Updates per second
    protected static final int statusBarHeight = 47; // Vertical size of bottom panel
    protected static final Font font = new Font("Arial", Font.PLAIN, 14); // Font we are using to print text

    protected GameWindow(int width, int height, String title) {
        this.width = width;
        this.height = height;
        setSize(width, height);
        requestFocus();

        // Creating window
        this.title = title;
        frame = new JFrame("TerraControl" + title);
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);

        // Creating input managers
        keyboard = new Keyboard();
        mouse = new Mouse();

        // Adding all kind of listeners
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
        addKeyListener(keyboard);
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        // Showing window
        frame.setVisible(true);
    }

    protected abstract void start(DataArray data);

    protected void stop() {
        debug.println("Stopping" + title + "...");
        running = false;

        if (connection != null) connection.stop();
        try {
            if (thread != null) thread.join();
        } catch (InterruptedException e) {
            ErrorLogger.add(e);
        }
    }

    public final void run() {
        long lastNanoTime = System.nanoTime();
        long currentNanoTime;
        long timer = System.currentTimeMillis();
        double delta = 0;
        int updates = 0, frames = 0;

        while (running) {
            currentNanoTime = System.nanoTime();
            delta += (currentNanoTime - lastNanoTime) / 1000000000.0 * ups;
            lastNanoTime = currentNanoTime;
            try {
                while (delta >= 1) {
                    update();
                    updates++;
                    delta--;
                }
                render();
                frames++;
            } catch (Exception e) {
                ErrorLogger.add(e);
            }

            if (System.currentTimeMillis() - timer >= 1000) {
                timer = System.currentTimeMillis();
                frame.setTitle("TerraControl" + title + ": " + updates + " ups, " + frames + " fps");
                updates = 0;
                frames = 0;
            }
        }
    }

    protected abstract void update();

    protected abstract void render();

    public int getFieldHeight() {
        return height - statusBarHeight;
    }

    public Debug getDebug() {
        return debug;
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public Mouse getMouse() {
        return mouse;
    }

    public abstract Level getLevel();

    public abstract Connection getConnection();

}
