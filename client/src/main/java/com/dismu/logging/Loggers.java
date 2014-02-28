package com.dismu.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loggers {
    public static final Logger packetLogger = LoggerFactory.getLogger("packet");
    public static final Logger serverLogger = LoggerFactory.getLogger("server");
    public static final Logger clientLogger = LoggerFactory.getLogger("client");
    public static final Logger playerLogger = LoggerFactory.getLogger("player");
}