package com.dismu.ui.pc;

import com.dismu.logging.Loggers;

import java.awt.Graphics;
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
        g.drawImage(image, 0, 0, null);
    }

}
