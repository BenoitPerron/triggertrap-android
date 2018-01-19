package com.praetoriandroid.cameraremote;

class ServiceNotSupportedException extends RpcException {
    private static final long serialVersionUID = -4740335873344202486L;

    public ServiceNotSupportedException(String serviceType) {
        super(serviceType);
    }
}
