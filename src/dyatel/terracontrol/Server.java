package dyatel.terracontrol;

import dyatel.terracontrol.input.Keyboard;
import dyatel.terracontrol.input.Mouse;
import dyatel.terracontrol.level.ServerLevel;
import dyatel.terracontrol.network.ServerConnection;
import dyatel.terracontrol.util.Debug;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

public class Server extends Canvas implements Runnable {

    public boolean running;

    private Debug debug = Debug.serverDebug;

    private int width, height;

    private JFrame frame;

    private int ups = 60;

    private Thread thread;

    private ServerConnection connection;

    private Screen screen;
    private Keyboard keyboard;
    private Mouse mouse;
    private ServerLevel level;

    private Font font = new Font("Arial", Font.PLAIN, 14);
    public String[] statusBar = new String[]{"", "", "", "", "", ""};

    public static final int statusBarHeight = 47;

    Server(int port, int width, int height, int levelWidth, int levelHeight, int cellSize, int[] colors, boolean fastGeneration) {
        this.width = width;
        this.height = height;
        setSize(width, height);

        keyboard = new Keyboard();
        mouse = new Mouse();

        frame = new JFrame("TerraControl server");
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        requestFocus();

        // Listeners go here
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
        addKeyListener(keyboard);
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        frame.setVisible(true);

        start(port, cellSize, levelWidth, levelHeight, colors, fastGeneration);
    }

    public void start(int port, int cellSize, int levelWidth, int levelHeight, int[] colors, boolean fastGeneration) {
        debug.println("Starting server...");
        running = true;
        thread = new Thread(this, "Server");

        // Initialization goes here
        screen = new Screen(width, height);
        level = new ServerLevel(levelWidth, levelHeight, cellSize, colors, fastGeneration, this);

        connection = new ServerConnection(level, port);

        thread.start();
    }

    public void stop() {
        debug.println("Stopping server...");
        running = false;

        connection.stop();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        long lastNanoTime = System.nanoTime();
        long currentNanoTime;
        long timer = System.currentTimeMillis();
        double delta = 0;
        int updates = 0, frames = 0;

        while (running) {
            currentNanoTime = System.nanoTime();
            delta += (currentNanoTime - lastNanoTime) / 1000000000.0 * ups;
            lastNanoTime = currentNanoTime;
            while (delta >= 1) {
                update();
                updates++;
                delta--;
            }
            render();
            frames++;

            if (System.currentTimeMillis() - timer >= 1000) {
                timer = System.currentTimeMillis();
                frame.setTitle("TerraControl server: " + updates + " ups, " + frames + " fps");
                updates = 0;
                frames = 0;
            }
        }
    }

    private void update() {
        level.update();
    }

    private void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        // Draw stuff here
        level.render(screen);
        screen.draw(g);

        g.setColor(Color.WHITE);
        g.fillRect(0, height - statusBarHeight, width, statusBarHeight);

        g.setColor(Color.BLACK);
        g.setFont(font);
        String tx = "Tx: " + connection.getTransmitted();
        String rx = "Rx: " + connection.getReceived();
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        FontMetrics fm = image.getGraphics().getFontMetrics(font);
        g.drawString(statusBar[0], 2, height - 28);
        g.drawString(statusBar[1], (width - fm.stringWidth(statusBar[1])) / 2, height - 28);
        g.drawString(statusBar[2], width - fm.stringWidth(statusBar[2]) - 2, height - 28);
        g.drawString(tx, 2, height - 8);
        g.drawString(statusBar[4], (width - fm.stringWidth(statusBar[4])) / 2, height - 8);
        g.drawString(rx, width - fm.stringWidth(rx) - 2, height - 8);

        g.dispose();
        bs.show();
    }

    public int getFieldHeight() {
        return height - statusBarHeight;
    }

    public ServerConnection getConnection() {
        return connection;
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public Mouse getMouse() {
        return mouse;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public static void main(String[] args) {
        new Server(8192, 500, 300, 55, 28, 8, new int[]{0xff0000, 0x00ff00, 0x0000ff}, false);
    }

}
