package com.dismu.exceptions;

public class EmptyPlaylistException extends Exception {
    public EmptyPlaylistException() { super(); }
    public EmptyPlaylistException(String message) { super(message); }
    public EmptyPlaylistException(String message, Throwable cause) { super(message, cause); }
    public EmptyPlaylistException(Throwable cause) { super(cause); }
}
