package at.photosniper.util;

/**
 * Created by johannes on 13.01.2018.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;
import static at.photosniper.service.BluetoothLeService.UUID_DESCRIPTOR;
import static at.photosniper.service.BluetoothLeService.UUID_HM_RX_TX;
import static at.photosniper.service.BluetoothLeService.UUID_SERVICE;

public class GattClient {

    private static final String TAG = GattClient.class.getSimpleName();


    // UUID for the UART BTLE client characteristic which is necessary for notifications.
//    private static final UUID DESCRIPTOR_CONFIG = UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG_DESC); // UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//    public static UUID DESCRIPTOR_USER_DESC = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");

//    public static final UUID SERVICE_UUID = UUID.fromString(GattAttributes.HM_10_CUSTOM_SERVICE); // UUID.fromString("795090c7-420d-4048-a24e-18e60180e23c");
//    private static final UUID CHARACTERISTIC_RXTX_UUID = UUID.fromString(GattAttributes.HM_RX_TX_CUSTOM_CHARACTERISTIC);// UUID.fromString("31517c58-66bf-470c-b662-e352a6c80cba");
//    private static final UUID CHARACTERISTIC_INTERACTOR_UUID = CHARACTERISTIC_RXTX_UUID; //UUID.fromString("0b89d2d4-0ea6-4141-86bb-0c5fb91ab14a");

    private Context mContext;
    private OnCharacteristicReadListener mListener;
    private String mDeviceAddress;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT client. Attempting to start service discovery");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT client");
                mListener.onBLEConnected(false);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                boolean connected = false;

                BluetoothGattService service = gatt.getService(UUID_SERVICE);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_HM_RX_TX);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            connected = gatt.writeDescriptor(descriptor);
                        }
                    }
                }
                mListener.onBLEConnected(connected);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            readCharacteristic(characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (UUID_DESCRIPTOR.equals(descriptor.getUuid())) {
                BluetoothGattCharacteristic characteristic = gatt.getService(UUID_SERVICE).getCharacteristic(UUID_HM_RX_TX);
                gatt.readCharacteristic(characteristic);
            }
        }

        private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (UUID_HM_RX_TX.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();

                String characteristicData = new String(data);

                Log.i(TAG, "Characteristic: " + characteristic.getUuid() + " ->  " + characteristicData);

                mListener.onBLECharacteristicRead(characteristicData);
            }
        }
    };
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startClient();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopClient();
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
    };

    public static byte[] getUserDescription(UUID characteristicUUID) {
        String desc;

        if (UUID_HM_RX_TX.equals(characteristicUUID)) {
            desc = "RX TX I/O";
        } else if (UUID_HM_RX_TX.equals(characteristicUUID)) {
            desc = "same";
        } else {
            desc = "";
        }

        return desc.getBytes(Charset.forName("UTF-8"));
    }

    public void onCreate(Context context, String deviceAddress, OnCharacteristicReadListener listener) throws RuntimeException {
        mContext = context;
        mListener = listener;
        mDeviceAddress = deviceAddress;

        mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            throw new RuntimeException("GATT client requires Bluetooth support");
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothReceiver, filter);

        if (!mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is currently disabled... enabling");
            mBluetoothAdapter.enable();
        } else {
            Log.i(TAG, "Bluetooth enabled... starting client");
            startClient();
        }
    }

    public void onDestroy() {
        mListener = null;

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopClient();
        }

        mContext.unregisterReceiver(mBluetoothReceiver);
    }

    public void writeCommand(String command) {
        BluetoothGattCharacteristic interactor = mBluetoothGatt.getService(UUID_SERVICE).getCharacteristic(UUID_HM_RX_TX);
        interactor.setValue(command.getBytes());
        if (!mBluetoothGatt.writeCharacteristic(interactor)) {
            Log.w(TAG, "writeCharacteristic failed!!");
        } else {
            mBluetoothGatt.setCharacteristicNotification(interactor, true);
        }

    }

    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    private void startClient() {
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback);

        if (mBluetoothGatt == null) {
            Log.w(TAG, "Unable to create GATT client");
        }
    }

    private void stopClient() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = null;
        }
    }

    public interface OnCharacteristicReadListener {
        void onBLECharacteristicRead(String value);

        void onBLEConnected(boolean success);
    }
}