package at.photosniper.wifi;

import android.bluetooth.BluetoothAdapter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import at.photosniper.service.TriggertrapService;

public class ZeroConfJmdns implements IZeroConf, MasterServer.ServerConnectionListener {
    private static final String TAG = ZeroConfJmdns.class.getSimpleName();
    private static final String TT_SERVER_NAME = "TT_Master_device";
    private static final String TT_SERVICE_TYPE = "_triggertrap._tcp.local.";
    WifiManager.MulticastLock lock;
    private final TriggertrapService mParentService;
    private JmDNS jmdnsMaster = null;
    private JmDNS jmdnsWatcher = null;
    private final ServiceListener listener;


    private final MasterServer masterServer = MasterServer.getInstance();
    private int portNumber;
    private String serverName;
    private String masterIP;

    private final ArrayList<ServiceInfo> masters = new ArrayList<>();

    public ZeroConfJmdns(TriggertrapService parentService) {
        mParentService = parentService;
        listener = new ServiceListener() {

            public void serviceResolved(ServiceEvent ev) {
                Log.d("ZeroConf", "Resolved");

                for (String url : ev.getInfo().getURLs()) {
                    Log.d(TAG, "URL is: " + url);
                }
                if (jmdnsMaster != null) {
                    if (ev.getInfo().getName().equals(serverName)) {
                        Log.d(TAG, "Resolved own Master: " + serverName + " ingnoring");
                        return;
                    }
                }
                masterAdded(ev.getInfo());
            }

            public void serviceRemoved(ServiceEvent ev) {
                Log.d("ZeroConf", "Removed " + this.toString());

                masterRemoved(ev.getInfo());
            }

            public void serviceAdded(ServiceEvent event) {
                Log.d("ZeroConf", "Added " + this.toString());

                // Force serviceResolved to be called again
                jmdnsWatcher.requestServiceInfo(event.getType(), event.getName(), 5000);

            }
        };
    }

    private static JSONObject jsonifyService(ServiceInfo info) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("application", info.getApplication());
            obj.put("domain", info.getDomain());
            obj.put("port", info.getPort());
            obj.put("name", info.getName());
            obj.put("server", info.getServer());
            obj.put("description", info.getNiceTextString());
            obj.put("protocol", info.getProtocol());
            obj.put("qualifiedname", info.getQualifiedName());
            obj.put("type", info.getType());

            JSONArray addresses = new JSONArray();
            String[] add = info.getHostAddresses();
            for (String anAdd : add) {
                addresses.put(anAdd);
            }
            obj.put("addresses", addresses);
            JSONArray urls = new JSONArray();

            String[] url = info.getURLs();
            for (String anUrl : url) {
                urls.put(anUrl);
            }
            obj.put("urls", urls);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return obj;

    }

    public void watch() {
        Log.d(TAG, "Watching " + TT_SERVICE_TYPE);
        if (jmdnsWatcher == null) {
            setupWatcher();
        } else {
            jmdnsWatcher.addServiceListener(TT_SERVICE_TYPE, listener);
        }

    }

    public void unwatch() {
        Log.d(TAG, "Unwatching " + TT_SERVICE_TYPE);
        if (jmdnsWatcher == null) {
            return;
        }
        jmdnsWatcher.removeServiceListener(TT_SERVICE_TYPE, listener);

    }

    public void registerMaster() {
        if (jmdnsMaster == null) {
            setupMaster();
            try {
                setupServer();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        TTServiceInfo info = new TTServiceInfo(serverName, "", portNumber);
        mParentService.onWifiMasterRegistered(info);
    }

    public void unregisterMaster() {
        if (jmdnsMaster != null) {
            jmdnsMaster.unregisterAllServices();
            masterServer.close();
            jmdnsMaster = null;
        }
        if (mParentService != null) {
            mParentService.onWifiMasterUnregistered();
        }
    }

    public void close() {
        if (jmdnsWatcher != null) {
            try {
                jmdnsWatcher.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnectSlaveFromMaster(String uniqueSlaveName) {
        masterServer.disconnectClient(uniqueSlaveName);
    }

    public ArrayList<TTSlaveInfo> getConnectedSlaves() {
        return masterServer.getConnectedSlaves();
    }

    private void setupServer() throws Exception {

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        serverName = bluetooth.getName();
        // Occasionally bluetooth name might return null, if so just give it a
        // generic name;
        if (serverName == null) {
            serverName = TT_SERVER_NAME;
        }
        portNumber = masterServer.createServer();
        masterServer.setConnectionListener(ZeroConfJmdns.this);
        Log.d("ZeroConf", "Creating service info");
        ServiceInfo service = ServiceInfo.create(TT_SERVICE_TYPE, serverName, portNumber, "tt_beep_service");
        Log.d("ZeroConf", "Created service info " + serverName + " :" + portNumber);
        try {
            jmdnsMaster.registerService(service);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("ZeroConf", "Registered");

    }

    private void setupMaster() {

//        WifiManager wifi = (WifiManager) mParentService.getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
//        lock = wifi.createMulticastLock("ZeroConfPluginLock");
//        lock.setReferenceCounted(true);
//        lock.acquire();
//
//        try {
//            masterIP = Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
//            jmdnsMaster = JmDNS.create(InetAddress.getByName(masterIP));
//
//            Log.d(TAG, "Creating jmDNS for : " + masterIP);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//
    }

    private void setupWatcher() {
        Log.d("ZeroConf", "Setup watcher");
//        WifiManager wifi = (WifiManager) mParentService.getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
//        lock = wifi.createMulticastLock("ZeroConfPluginLock");
//        lock.setReferenceCounted(true);
//        lock.acquire();
//
//        //Need to set up the jmdns instance with IP otherwise could have problems i.e it broadcasts an IPv6 when on IPv4
//        String ip = Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
//        new RetreiveJmDnsWatcher().execute(ip);

    }


    private void masterAdded(ServiceInfo info) {
        Log.d(TAG, "Service info resolved: " + jsonifyService(info));
        masters.add(info);
        String ipAddress = info.getHostAddresses()[0];
        mParentService.wiFiMasterAdded(info.getName(), ipAddress, info.getPort());
    }

    private void masterRemoved(ServiceInfo info) {
        Log.d(TAG, "Service info resolved: " + jsonifyService(info));
        masters.remove(info);
        //String ipAddress = info.getHostAddresses()[0];
        mParentService.wiFiMasterRemoved(info.getName(), "", info.getPort());
    }

    public void onClientConnectionReceived(String clientName, String uniqueName) {
        //sendClientNotifyCallback("added",clientName, uniqueName);
        Log.d(TAG, "onClientConnectionReceived: " + clientName + " " + uniqueName);
        mParentService.onClientConnectionReceived(clientName, uniqueName);
    }

    public void onClientDisconnectReceived(String clientName, String uniqueName) {
        //sendClientNotifyCallback("removed",clientName, uniqueName);
        Log.d(TAG, "onClientDisconnectReceived: " + clientName + " " + uniqueName);
        mParentService.onClientDisconnectionReceived(clientName, uniqueName);
    }

    private class RetreiveJmDnsWatcher extends AsyncTask<String, Void, JmDNS> {

        protected JmDNS doInBackground(String... ips) {
            try {
                Log.d(TAG, "JmDNS version: " + JmDNS.VERSION);
                return jmdnsWatcher = JmDNS.create(InetAddress.getByName(ips[0]));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(JmDNS watcher) {
            jmdnsWatcher = watcher;
            Log.d("ZeroConf", "Watch " + TT_SERVICE_TYPE);
            if (jmdnsWatcher != null) {
                Log.d("ZeroConf", "Name: " + jmdnsWatcher.getName() + " host: " + jmdnsWatcher.getHostName());
                jmdnsWatcher.addServiceListener(TT_SERVICE_TYPE, listener);
            }
        }
    }

}
