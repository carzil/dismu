package com.dismu.ui.pc.taskbar;

import com.dismu.logging.Loggers;
import org.bridj.Pointer;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;
import org.bridj.jawt.JAWTUtils;

import javax.swing.*;

public class DismuTaskBar {
    private JFrame frame;
    ITaskbarList3 taskbarList3;

    public void init(JFrame frame) {
        this.frame = frame;
        try {
            taskbarList3 = COMRuntime.newInstance(ITaskbarList3.class);
        } catch (ClassNotFoundException e) {
            Loggers.uiLogger.error("cannot init task bar", e);
        }
    }

    public void setProgressBarValue(int value) {
        long hwndVal = JAWTUtils.getNativePeerHandle(frame);
        Pointer<?> hwnd = Pointer.pointerToAddress(hwndVal);
        taskbarList3.SetProgressValue((Pointer) hwnd, value, 100);
    }
}
