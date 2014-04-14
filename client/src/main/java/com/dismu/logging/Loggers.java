package com.dismu.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loggers {
    public static final Logger packetLogger = LoggerFactory.getLogger("Dismu.packet");
    public static final Logger serverLogger = LoggerFactory.getLogger("Dismu.server");
    public static final Logger clientLogger = LoggerFactory.getLogger("Dismu.client");
    public static final Logger playerLogger = LoggerFactory.getLogger("Dismu.player");
    public static final Logger uiLogger = LoggerFactory.getLogger("Dismu.ui");
    public static final Logger miscLogger = LoggerFactory.getLogger("Dismu.misc");
}
