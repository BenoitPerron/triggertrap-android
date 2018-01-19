package com.praetoriandroid.cameraremote.rpc;

class IllegalResultSizeException extends ValidationException {
    private static final long serialVersionUID = -2132223180028624107L;

    public IllegalResultSizeException(int expectedSize, int actualSize) {
        super("Illegal 'result' size (must be " + expectedSize + "): " + actualSize);
    }
}
