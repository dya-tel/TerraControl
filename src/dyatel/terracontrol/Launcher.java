package dyatel.terracontrol;

import dyatel.terracontrol.util.Debug;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

public class Launcher extends JFrame {

    Launcher() {
        final Debug debug = Debug.launcherDebug;

        setResizable(false);

        setTitle("TerraControl");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new GridLayout(0, 3));

        final JTextField widthField = new JTextField("500");
        final JTextField heightField = new JTextField("300");

        final JTextField levelWidthField = new JTextField("55");
        final JTextField levelHeightField = new JTextField("28");

        final JTextField addressField = new JTextField("localhost");
        final JTextField portField = new JTextField("8192");

        final JTextField cellSizeField = new JTextField("8");

        final JCheckBox fastGenerationCheck = new JCheckBox();
        fastGenerationCheck.setHorizontalAlignment(JCheckBox.CENTER);
        fastGenerationCheck.setSelected(true);

        final JTextField colorsField = new JTextField("ff0000 00ff00 0000ff");

        final JButton client = new JButton("Client");
        final JButton server = new JButton("Server");

        add(new JLabel("Window size"));
        add(widthField);
        add(heightField);
        add(new JLabel("Level size"));
        add(levelWidthField);
        add(levelHeightField);
        add(new JLabel("Address"));
        add(addressField);
        add(portField);

        add(new JLabel("Cell size"));
        add(new JLabel());
        add(cellSizeField);

        add(new JLabel("Fast generation"));
        add(new JLabel());
        add(fastGenerationCheck);

        add(new JLabel("Colors"));
        add(new JLabel());
        add(colorsField);

        add(client);
        add(new JLabel());
        add(server);
        pack();

        client.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int port = Integer.parseInt(portField.getText());
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    int cellSize = Integer.parseInt(cellSizeField.getText());
                    InetAddress.getByName(addressField.getText());

                    new Client(addressField.getText(), port, width, height, cellSize);
                } catch (Exception ex) {
                    debug.println("Error: wrong input!");
                }
            }

        });

        server.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int port = Integer.parseInt(portField.getText());
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    int levelWidth = Integer.parseInt(levelWidthField.getText());
                    int levelHeight = Integer.parseInt(levelHeightField.getText());
                    int cellSize = Integer.parseInt(cellSizeField.getText());
                    boolean fastGeneration = fastGenerationCheck.isSelected();

                    String[] colorsR = colorsField.getText().split(" ");
                    int[] colors = new int[colorsR.length];
                    for (int i = 0; i < colorsR.length; i++) {
                        colors[i] = Integer.parseInt(colorsR[i], 16);
                    }

                    new Server(port, width, height, levelWidth, levelHeight, cellSize, colors, fastGeneration);
                } catch (NumberFormatException ex) {
                    debug.println("Error: wrong input!");
                }
            }
        });

        setLocationRelativeTo(null);

        setVisible(true);
    }

    public static void main(String[] args) {
        new Launcher();
    }

}
