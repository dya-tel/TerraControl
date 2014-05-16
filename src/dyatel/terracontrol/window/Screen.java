package dyatel.terracontrol.window;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Screen {

    private int width, height;

    private int xOffset, yOffset;

    private BufferedImage image;
    private int[] pixels;

    public static int emptyPixelColor = 0x333333;

    public Screen(int width, int height) {
        this.width = width;
        this.height = height;

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    public void render(int x1, int y1, int x2, int y2, int color, boolean applyOffset) {
        if (applyOffset) {
            x1 -= xOffset;
            y1 -= yOffset;

            x2 -= xOffset;
            y2 -= yOffset;
        }
        for (int yCur = y1; yCur < y2; yCur++) {
            if (yCur >= height || yCur < 0) {
                continue;
            }
            for (int xCur = x1; xCur < x2; xCur++) {
                if (xCur >= width || xCur < 0) {
                    continue;
                }

                pixels[xCur + yCur * width] = color;
            }
        }
    }

    public void setOffset(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public void draw(Graphics g) {
        g.drawImage(image, 0, 0, null);

        // Clearing buffer
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = emptyPixelColor;
        }
    }

}
