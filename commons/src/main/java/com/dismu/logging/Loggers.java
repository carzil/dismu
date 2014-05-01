package com.dismu.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loggers {
    public static final Logger packetLogger = LoggerFactory.getLogger("packet");
    public static final Logger serverLogger = LoggerFactory.getLogger("server");
    public static final Logger clientLogger = LoggerFactory.getLogger("client");
    public static final Logger playerLogger = LoggerFactory.getLogger("player");
    public static final Logger uiLogger = LoggerFactory.getLogger("ui");
    public static final Logger fsLogger = LoggerFactory.getLogger("fs");
    public static final Logger miscLogger = LoggerFactory.getLogger("misc");
}
