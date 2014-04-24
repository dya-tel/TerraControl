package dyatel.terracontrol;

import dyatel.terracontrol.input.Keyboard;
import dyatel.terracontrol.input.Mouse;
import dyatel.terracontrol.level.ClientLevel;
import dyatel.terracontrol.network.ClientConnection;
import dyatel.terracontrol.util.Debug;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

public class Client extends Canvas implements Runnable {

    public boolean running;

    private Debug debug = Debug.clientDebug;

    private int width, height;

    private JFrame frame;

    private int ups = 60;

    private Thread thread;

    private ClientConnection connection;

    private Screen screen;
    private Keyboard keyboard;
    private Mouse mouse;
    private ClientLevel level;

    private Font font = new Font("Arial", Font.PLAIN, 14);
    public String[] statusBar = new String[]{"", "", "", "", "", ""};

    public static final int statusBarHeight = 47;

    Client(String address, int serverPort, int width, int height, int cellSize) {
        this.width = width;
        this.height = height;
        setSize(width, height);

        keyboard = new Keyboard();
        mouse = new Mouse();

        frame = new JFrame("TerraControl client");
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

        start(address, serverPort, cellSize);
    }

    public void start(String address, int serverPort, int cellSize) {
        debug.println("Starting client...");
        running = true;
        thread = new Thread(this, "Client");

        // Initialization goes here
        screen = new Screen(width, height);
        level = new ClientLevel(cellSize, this);

        connection = new ClientConnection(address, serverPort, this);

        thread.start();
    }

    public void stop() {
        debug.println("Stopping client...");
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
                frame.setTitle("TerraControl client: " + updates + " ups, " + frames + " fps");
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
        g.drawString(statusBar[2], width - fm.stringWidth(statusBar[2]) - 20, height - 28);
        g.drawString(tx, 2, height - 8);
        g.drawString(statusBar[4], (width - fm.stringWidth(statusBar[4])) / 2, height - 8);
        g.drawString(rx, width - fm.stringWidth(rx) - 2, height - 8);

        g.setColor(new Color(level.currentColor));
        g.fillRect(width - 18, height - 44, 16, 16);

        g.dispose();
        bs.show();
    }

    public int getFieldHeight() {
        return height - statusBarHeight;
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public Mouse getMouse() {
        return mouse;
    }

    public ClientLevel getLevel() {
        return level;
    }

    public static void main(String[] args) {
        new Client(args[0], 8192, 500, 300, 8);
    }

}