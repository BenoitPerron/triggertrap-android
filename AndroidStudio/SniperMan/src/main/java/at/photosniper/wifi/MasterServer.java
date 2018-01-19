package at.photosniper.wifi;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class MasterServer {


    private static final String TAG = MasterServer.class.getSimpleName();
    private static MasterServer instance = null;
    private final Handler handler = new Handler();
    private ArrayList<Socket> clientSockets = null;
    private ArrayList<String> clientSocketNames = null;
    private ArrayList<String> clientSocketUniqueNames = null;

    private ArrayList<DisconnectListeningThread> disconnectListenerThreads = null;
    private ServerSocket serverSocket;
    private boolean isListening = false;
    private ServerConnectionListener connectionListener = null;

    //Private constructor to enforce singleton pattern.
    private MasterServer() {

    }

    public static MasterServer getInstance() {
        if (instance == null) {
            instance = new MasterServer();
        }
        return instance;
    }

    public int createServer() {
        clientSockets = new ArrayList<>();
        clientSocketNames = new ArrayList<>();
        clientSocketUniqueNames = new ArrayList<>();
        disconnectListenerThreads = new ArrayList<>();

        int serverPort = -1;
        try {
            serverSocket = new ServerSocket(0);
            serverPort = serverSocket.getLocalPort();
            isListening = true;
            startListening();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serverPort;
    }

    private void startListening() {
        class ServerListeningThread implements Runnable {
            public void run() {
                while (isListening) {
                    try {
                        Log.d(TAG, "Listening for client Socket");
                        final Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        final String line = in.readLine();
                        Log.d(TAG, "Recieved Connection from device: " + line);
                        handler.post(new Runnable() {
                            public void run() {
                                gotClientConnection(clientSocket, line);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Thread thread = new Thread(new ServerListeningThread());
        thread.start();
    }

    public void beep() {
        if (isListening) {
            for (Socket clientSocket : clientSockets) {

                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
                    String BEEP_STRING = "BEEP\r\n";
                    out.println(BEEP_STRING);
                    Log.d(TAG, "Wifi sending string: " + BEEP_STRING);
                } catch (IOException e) {
                    Log.d(TAG, "Error writing to socket");
                    e.printStackTrace();
                }
            }
        }
    }

    private void gotClientConnection(Socket clientSocket, String client) {
        clientSockets.add(clientSocket);
        clientSocketNames.add(client);
        Long uuid = UUID.randomUUID().getMostSignificantBits();
        String uniqueName = client + uuid.toString();
        uniqueName = uniqueName.replace(' ', '_');
        clientSocketUniqueNames.add(uniqueName);

        if (connectionListener != null) {
            connectionListener.onClientConnectionReceived(client, uniqueName);
        }
        //start a listener thread for disconnect from peer.
        Thread thread = null;
        try {
            InputStream in = clientSocket.getInputStream();
            DisconnectListeningThread disconnectRunnable = new DisconnectListeningThread(clientSocket, in);
            disconnectListenerThreads.add(disconnectRunnable);
            thread = new Thread(disconnectRunnable);
            thread.start();
        } catch (IOException exp) {
            exp.printStackTrace();
        }

    }

    private void gotClientDisconnection(Socket clientSocket) {
        Log.d(TAG, "gotClientDisconnection");
        int index = clientSockets.indexOf(clientSocket);
        Log.d(TAG, "Disconnect client: " + clientSocketNames.get(index));
        if (connectionListener != null) {
            connectionListener.onClientDisconnectReceived(clientSocketNames.get(index), clientSocketUniqueNames.get(index));
        }
        clientSockets.remove(index);
        clientSocketNames.remove(index);
        clientSocketUniqueNames.remove(index);
    }

    public void disconnectClient(String uniqueName) {
        Log.d(TAG, "disconnectClient " + uniqueName);
        int index = clientSocketUniqueNames.indexOf(uniqueName);
        Socket clientSocket = clientSockets.get(index);
        try {
            clientSocket.close();
        } catch (IOException e) {
            Log.d(TAG, "Unable to close client socket");
            e.printStackTrace();
        }
        clientSockets.remove(index);
        clientSocketNames.remove(index);
        clientSocketUniqueNames.remove(index);
    }

    public ArrayList<PhotoSniperSlaveInfo> getConnectedSlaves() {
        ArrayList<PhotoSniperSlaveInfo> connectedSlaves = new ArrayList<>();
        int index = 0;
        if (clientSocketNames != null) {
            for (String name : clientSocketNames) {
                PhotoSniperSlaveInfo slaveInfo = new PhotoSniperSlaveInfo(name, clientSocketUniqueNames.get(index));
                connectedSlaves.add(slaveInfo);
                index++;
            }
        }
        return connectedSlaves;
    }

    public void close() {
        isListening = false;
        try {

            for (DisconnectListeningThread disconnectListeningThread : disconnectListenerThreads) {
                disconnectListeningThread.stopListening();
            }
            for (Socket clientSocket : clientSockets) {
                clientSocket.close();
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(ServerConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }


    public interface ServerConnectionListener {
        void onClientConnectionReceived(String clientName, String uniqueName);

        void onClientDisconnectReceived(String clientName, String uniqueName);
    }

    class DisconnectListeningThread implements Runnable {
        private boolean isListening = true;

        private final Socket clientSocket;
        private final InputStream inStream;

        public DisconnectListeningThread(Socket clientSocket, InputStream in) {
            inStream = in;
            this.clientSocket = clientSocket;
        }

        public void run() {
            while (isListening) {
                try {
                    Log.d(TAG, "Listening for client Socket");
                    int result = inStream.read();
                    if (result == -1) {
                        isListening = false;
                        handler.post(new Runnable() {
                            public void run() {
                                int threadIndex = disconnectListenerThreads.indexOf(DisconnectListeningThread.this);
                                disconnectListenerThreads.remove(threadIndex);
                                gotClientDisconnection(clientSocket);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    isListening = false;
                }
            }
        }

        public void stopListening() {
            isListening = false;
        }
    }

}
