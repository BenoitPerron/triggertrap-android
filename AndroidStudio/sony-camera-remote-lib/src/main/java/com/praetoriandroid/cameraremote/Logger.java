package com.praetoriandroid.cameraremote;

public interface Logger {

    void debug(Object data);

    void debug(String format, Object... args);

    void info(Object data);

    void error(Object object);
}
