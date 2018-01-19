package com.praetoriandroid.cameraremote.rpc;

import java.util.List;

public class GetSupportedSelfTimerResponse extends BaseResponse<List<Integer>> {

    @Override
    public void validate() throws ValidationException {
        super.validate();

        if (isOk()) {
            List<Integer>[] result = getResult();
            if (result.length != 1) {
                throw new IllegalResultSizeException(1, result.length);
            }
        }
    }

    private List<Integer> getTimers() {
        return getResult()[0];
    }

    @Override
    public String toString() {
        return getTimers().toString();
    }
}
