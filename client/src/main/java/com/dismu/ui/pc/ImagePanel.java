package com.dismu.ui.pc;

import com.dismu.logging.Loggers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {
    private BufferedImage image;

    public ImagePanel(String filename) {
        try {
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream(filename));
            Loggers.uiLogger.debug("loaded image '{}'", filename);
        } catch (IOException ex) {
            Loggers.uiLogger.error("cannot load image '{}'", filename, ex);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.drawImage(image, 0, 0, null);
    }
}
