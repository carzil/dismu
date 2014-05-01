package com.dismu.exceptions;

public class TrackNotFoundException extends Exception {
    public TrackNotFoundException() { super(); }
    public TrackNotFoundException(String message) { super(message); }
    public TrackNotFoundException(String message, Throwable cause) { super(message, cause); }
    public TrackNotFoundException(Throwable cause) { super(cause); }
}
