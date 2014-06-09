package dyatel.terracontrol.window;

import dyatel.terracontrol.input.Keyboard;
import dyatel.terracontrol.input.Mouse;
import dyatel.terracontrol.level.BasicLevel;
import dyatel.terracontrol.level.Level;
import dyatel.terracontrol.network.Connection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.util.ErrorLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

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

    protected GameWindow(int width, int height, String title, DataArray data, Debug debug) {
        // Saving all data
        this.width = width;
        this.height = height;
        this.title = title;
        this.debug = debug;

        // Creating input managers
        keyboard = new Keyboard();
        mouse = new Mouse();

        // Initializing
        try {
            start(data);
        } catch (Exception e) {
            // Saying that we were not able to start
            debug.println("Stopping " + title + "!");
            ErrorLogger.add(e);

            // Stopping all we are running
            running = false;
            if (connection != null) connection.stop();

            return;
        }

        // Setting canvas size
        setSize(width, height);

        // Creating window
        frame = new JFrame("TerraControl" + title);
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);

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

        // Running main game loop
        thread.start();
    }

    protected abstract void start(DataArray data) throws Exception;

    protected void stop() {
        debug.println("Stopping" + title + "...");
        running = false;

        if (connection != null) connection.stop();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ErrorLogger.add(e);
        }

        frame.setVisible(false);
    }

    public final void run() {
        long lastNanoTime = System.nanoTime();
        long currentNanoTime;
        long timer = System.currentTimeMillis();
        double delta = 0;
        int updates = 0, frames = 0;

        boolean needRender = false;

        while (running) {
            currentNanoTime = System.nanoTime();
            delta += (currentNanoTime - lastNanoTime) / 1000000000.0 * ups;
            lastNanoTime = currentNanoTime;

            try {
                while (delta >= 1) {
                    update();
                    updates++;
                    delta--;
                    needRender = true;
                }

                if (needRender) {
                    render();
                    frames++;
                    needRender = false;
                }
            } catch (Exception e) {
                ErrorLogger.add(e);
            }

            int freeTime = (int) ((1000000000d / ups - (System.nanoTime() - lastNanoTime)) / 1000000);
            if (freeTime > 0) {
                try {
                    Thread.sleep(freeTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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

    public final void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        level.preRender(screen);

        // Interface background
        screen.render(0, height - statusBarHeight, width, height, 0xffffff, false);

        level.postRender(screen);

        screen.draw(g);

        // Status bar
        g.setColor(Color.BLACK);
        g.setFont(font);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        FontMetrics fm = image.getGraphics().getFontMetrics(font);
        g.drawString(statusBar[0], 2, height - 28);
        g.drawString(statusBar[1], (width - fm.stringWidth(statusBar[1])) / 2, height - 28);
        g.drawString(statusBar[2], width - fm.stringWidth(statusBar[2]) - 2, height - 28);
        g.drawString(statusBar[3], 2, height - 8);
        g.drawString(statusBar[4], (width - fm.stringWidth(statusBar[4])) / 2, height - 8);
        g.drawString(statusBar[5], width - fm.stringWidth(statusBar[5]) - 2, height - 8);

        g.dispose();
        bs.show();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

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

    public abstract BasicLevel getLevel();

    public abstract Connection getConnection();

}
