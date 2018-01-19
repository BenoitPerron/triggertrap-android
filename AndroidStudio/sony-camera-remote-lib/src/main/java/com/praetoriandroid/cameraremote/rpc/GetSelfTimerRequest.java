package com.praetoriandroid.cameraremote.rpc;

class GetSelfTimerRequest extends BaseRequest<Void, SimpleResponse> {
    public GetSelfTimerRequest() {
        super(SimpleResponse.class, RpcMethod.getSelfTimer);
    }
}
