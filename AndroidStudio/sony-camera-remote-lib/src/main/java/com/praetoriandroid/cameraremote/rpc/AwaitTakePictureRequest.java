package com.praetoriandroid.cameraremote.rpc;

class AwaitTakePictureRequest extends BaseRequest<Void, ActTakePictureResponse> {
    public AwaitTakePictureRequest() {
        super(ActTakePictureResponse.class, RpcMethod.awaitTakePicture);
    }
}
