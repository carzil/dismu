package com.dismu.ui.pc;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class PlaceholderTextField extends JLabel implements FocusListener, DocumentListener {
    public enum Show {
        ALWAYS,
        FOCUS_GAINED,
        FOCUS_LOST;
    }

    private JTextComponent component;
    private Document document;

    private Show show;
    private boolean showPromptOnce;
    private int focusLost;
    private String placeholder;

    public PlaceholderTextField(String text, JTextComponent component) {
        this(text, component, Show.ALWAYS);
        this.placeholder = text;
    }

    public PlaceholderTextField(String text, JTextComponent component, Show show) {
        this.component = component;
        setShow(show);
        document = component.getDocument();

        setText(text);
        setFont(component.getFont());
        setForeground(component.getForeground());
        setBorder(new EmptyBorder(component.getInsets()));
        setHorizontalAlignment(JLabel.LEADING);

        component.addFocusListener(this);
        document.addDocumentListener(this);

        component.setLayout(new BorderLayout());
        component.add(this);
        checkForPlaceholder();
        this.placeholder = text;
    }

    public void setAlpha(float alpha) {
        setAlpha((int) (alpha * 255));
    }

    public void setAlpha(int alpha) {
        alpha = alpha > 255 ? 255 : alpha < 0 ? 0 : alpha;

        Color foreground = getForeground();
        int red = foreground.getRed();
        int green = foreground.getGreen();
        int blue = foreground.getBlue();

        Color withAlpha = new Color(red, green, blue, alpha);
        super.setForeground(withAlpha);
    }

    public void setStyle(int style) {
        setFont(getFont().deriveFont(style));
    }

    public void setShow(Show show) {
        this.show = show;
    }

    private void checkForPlaceholder() {
        if (document.getLength() > 0) {
            setVisible(false);
            return;
        }

        if (component.hasFocus()) {
            if (show == Show.ALWAYS || show == Show.FOCUS_GAINED)
                setVisible(true);
            else
                setVisible(false);
        } else {
            if (show == Show.ALWAYS || show == Show.FOCUS_LOST)
                setVisible(true);
            else
                setVisible(false);
        }
    }

    public void focusGained(FocusEvent e) {
        checkForPlaceholder();
    }

    public void focusLost(FocusEvent e) {
        focusLost++;
        checkForPlaceholder();
    }

    public void insertUpdate(DocumentEvent e) {
        checkForPlaceholder();
    }

    public void removeUpdate(DocumentEvent e) {
        checkForPlaceholder();
    }

    public void changedUpdate(DocumentEvent e) {
        checkForPlaceholder();
    }
}
