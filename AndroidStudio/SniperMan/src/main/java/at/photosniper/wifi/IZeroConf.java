package at.photosniper.wifi;

import java.util.ArrayList;

public interface IZeroConf {

    void watch();

    void unwatch();

    void registerMaster();

    void unregisterMaster();

    void disconnectSlaveFromMaster(String uniqueSlaveName);

    ArrayList<TTSlaveInfo> getConnectedSlaves();

    void close();
}
