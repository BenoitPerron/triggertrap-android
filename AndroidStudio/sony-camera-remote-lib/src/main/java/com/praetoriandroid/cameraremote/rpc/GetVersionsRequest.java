package com.praetoriandroid.cameraremote.rpc;

class GetVersionsRequest extends BaseRequest<String, GetVersionsResponse> {

    public GetVersionsRequest() {
        super(GetVersionsResponse.class, RpcMethod.getVersions);
    }

}
