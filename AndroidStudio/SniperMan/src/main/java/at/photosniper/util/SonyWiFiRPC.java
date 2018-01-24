package at.photosniper.util;

import android.util.Log;

import com.google.common.primitives.Ints;
import com.praetoriandroid.cameraremote.DeviceDescription;
import com.praetoriandroid.cameraremote.LiveViewDisconnectedException;
import com.praetoriandroid.cameraremote.LiveViewFetcher;
import com.praetoriandroid.cameraremote.ParseException;
import com.praetoriandroid.cameraremote.RpcClient;
import com.praetoriandroid.cameraremote.RpcException;
import com.praetoriandroid.cameraremote.SsdpClient;
import com.praetoriandroid.cameraremote.rpc.BaseRequest;
import com.praetoriandroid.cameraremote.rpc.BaseResponse;
import com.praetoriandroid.cameraremote.rpc.GetAvailableSelfTimerRequest;
import com.praetoriandroid.cameraremote.rpc.GetAvailableSelfTimerResponse;
import com.praetoriandroid.cameraremote.rpc.StartLiveViewRequest;
import com.praetoriandroid.cameraremote.rpc.StartLiveViewResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by johannes on 29.12.2017.
 */

public class SonyWiFiRPC {

    private static final String RPC_NETWORK = "SonyWiFiRPC network";
    private static final int SSDP_TIMEOUT = 1000;

    private final Set<SonyWiFiConnectionListener> sonyWiFiConnectionListeners = new HashSet<>();
    private final Map<Object, ResponseHandler<?>> responseHandlers = new HashMap<>();
    private RpcClient rpcClient;
    private Throwable initializationError;
    private boolean initialized;
    private final LiveViewFetcher liveViewFetcher = new LiveViewFetcher();
    private volatile boolean liveViewInProgress;
    private List<Integer> availableSelfTimers = Collections.emptyList();

    public SonyWiFiRPC() {
        liveViewFetcher.setConnectionTimeout(RpcClient.CONNECTION_TIMEOUT);
    }

    public List<Integer> getAvailableSelfTimers() {
        return availableSelfTimers;
    }

    //    @Background (serial = RPC_NETWORK)
    public void connect() {
        try {
            initialized = false;
            initializationError = null;
            SsdpClient ssdpClient = new SsdpClient();
            ssdpClient.setSearchTimeout(RpcClient.CONNECTION_TIMEOUT);
            String deviceDescriptionUrl = ssdpClient.getDeviceDescriptionUrl();
            DeviceDescription description = new DeviceDescription.Fetcher().setConnectionTimeout(RpcClient.CONNECTION_TIMEOUT).fetch(deviceDescriptionUrl);
            final String cameraServiceUrl = description.getServiceUrl(DeviceDescription.CAMERA);
            rpcClient = new RpcClient(cameraServiceUrl);
            rpcClient.setConnectionTimeout();
            rpcClient.sayHello();
            GetAvailableSelfTimerResponse selfTimers = rpcClient.send(new GetAvailableSelfTimerRequest());
            if (selfTimers.isOk()) {
                availableSelfTimers = Ints.asList(selfTimers.getAvailableTimers());
            }

            onSonyWifiConnected(cameraServiceUrl);

        } catch (Exception e) {
            onConnectionFailed(e);
        }
    }

    //    @UiThread
    private void onSonyWifiConnected(String cameraServiceUrl) {
        initialized = true;
        for (SonyWiFiConnectionListener callback : sonyWiFiConnectionListeners) {
            callback.onSonyWiFiConnected();
        }
    }

    //    @UiThread
    private void onConnectionFailed(Throwable e) {
        Log.e("@@@@@", "SonyWiFiRPC connect failed", e);
        initialized = true;
        initializationError = e;
        for (SonyWiFiConnectionListener callback : sonyWiFiConnectionListeners) {
            callback.onSonyWiFiConnectionFailed(e);
        }
        Log.i("@@@@@", "SonyWiFiRPC connect failed - not a problem..");
    }

    //    @UiThread (propagation = UiThread.Propagation.REUSE)
    public void registerInitCallback(SonyWiFiConnectionListener callback) {
        if (initialized) {
            if (initializationError == null) {
                callback.onSonyWiFiConnected();
            } else {
                callback.onSonyWiFiConnectionFailed(initializationError);
            }
        }
        sonyWiFiConnectionListeners.add(callback);
    }

    //    @UiThread (propagation = UiThread.Propagation.REUSE)
    public void unregisterInitCallback(SonyWiFiConnectionListener callback) {
        sonyWiFiConnectionListeners.remove(callback);
    }

    //    @UiThread (propagation = UiThread.Propagation.REUSE)
    public <Response extends BaseResponse<?>> void sendRequest(BaseRequest<?, Response> request, Object tag) {
        if (!initialized) {
            throw new IllegalStateException();
        }
        sendRequestInt(request, tag);
    }

    //    @UiThread (propagation = UiThread.Propagation.REUSE)
    public <Response extends BaseResponse<?>> void sendRequest(BaseRequest<?, Response> request, Object tag, ResponseHandler<Response> responseHandler) {
        if (!initialized) {
            throw new IllegalStateException();
        }
        responseHandlers.put(tag, responseHandler);
        sendRequestInt(request, tag);
    }

    //    @UiThread
    public void cancelRequest(Object tag) {
        responseHandlers.remove(tag);
    }

    //    @Background (serial = RPC_NETWORK)
    private <Response extends BaseResponse<?>> void sendRequestInt(BaseRequest<?, Response> request, Object tag) {
        try {
            Response response = rpcClient.send(request);
            if (response.isOk()) {
                onResponseSuccess(tag, response);
            } else {
                throw new ErrorResponseException(response.getErrorCode());
            }
        } catch (RpcException e) {
            onResponseFail(tag, e);
        }
    }

    //    @UiThread
    private <Response extends BaseResponse<?>> void onResponseSuccess(Object tag, Response response) {
        @SuppressWarnings("unchecked") ResponseHandler<Response> handler = (ResponseHandler<Response>) responseHandlers.get(tag);
        if (handler != null) {
            handler.onSuccess(response);
        }
    }

    //    @UiThread
    private <Response extends BaseResponse<?>> void onResponseFail(Object tag, Throwable e) {
        @SuppressWarnings("unchecked") ResponseHandler<Response> handler = (ResponseHandler<Response>) responseHandlers.get(tag);
        if (handler != null) {
            handler.onFail(e);
        }
    }

    public void startLiveView(final LiveViewCallback callback) {
        liveViewInProgress = true;
        sendRequest(new StartLiveViewRequest(), liveViewFetcher, new ResponseHandler<StartLiveViewResponse>() {
            @Override
            public void onSuccess(StartLiveViewResponse response) {
                onLiveViewStarted(response.getUrl(), callback);
            }

            @Override
            public void onFail(Throwable e) {
                callback.onError(e);
            }
        });
    }

    public void stopLiveView() {
        try {
            liveViewInProgress = false;
            liveViewFetcher.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onLiveViewStarted(final String url, final LiveViewCallback callback) {
        new Thread() {
            @Override
            public void run() {
                try {
                    liveViewFetcher.connect(url);
                    while (liveViewInProgress) {
                        LiveViewFetcher.Frame frame = liveViewFetcher.getNextFrame();
                        callback.onNextFrame(frame);
                    }
                } catch (IOException | ParseException e) {
                    callback.onError(e);
                } catch (LiveViewDisconnectedException ignored) {
                }
            }
        }.start();
    }

    public interface SonyWiFiConnectionListener {
        void onSonyWiFiConnected();

        void onSonyWiFiConnectionFailed(Throwable e);
    }

    public interface ResponseHandler<Response extends BaseResponse<?>> {
        void onSuccess(Response response);

        void onFail(Throwable e);
    }

    public interface LiveViewCallback {
        void onNextFrame(LiveViewFetcher.Frame frame);

        void onError(Throwable e);
    }

    public static class ErrorResponseException extends RpcException {
        private final int errorCode;

        public ErrorResponseException(int errorCode) {
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

}
