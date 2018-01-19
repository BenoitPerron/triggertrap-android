package com.praetoriandroid.cameraremote.rpc;

class StopMovieRecRequest extends BaseRequest<Void, StopMovieRecResponse> {
    public StopMovieRecRequest() {
        super(StopMovieRecResponse.class, RpcMethod.stopMovieRec);
    }
}
