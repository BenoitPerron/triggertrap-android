package at.photosniper.wifi;

public interface WifiListener {

    void onWatchService();

    void onUnWatchService();

    void onMasterRegisterMaster();

    void onMasterUnregister();
}
