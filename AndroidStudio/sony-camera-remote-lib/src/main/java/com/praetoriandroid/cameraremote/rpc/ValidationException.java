package com.praetoriandroid.cameraremote.rpc;

import com.praetoriandroid.cameraremote.RpcException;

class ValidationException extends RpcException {
    private static final long serialVersionUID = -2592332852759567070L;

    ValidationException() {
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }
}
