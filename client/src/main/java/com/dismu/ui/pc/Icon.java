package com.dismu.ui.pc;

import com.dismu.logging.Loggers;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Icon extends JLabel {
    private float alpha = 1.0f;

    public Icon(ImageIcon icon) {
        super.setIcon(icon);
    }

    public Icon() {
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        revalidate();
        repaint();
    }

    public void setIcon(ImageIcon icon) {
        super.setIcon(icon);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics2D = (Graphics2D) g;
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        graphics2D.setComposite(alphaComposite);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
    }
}
