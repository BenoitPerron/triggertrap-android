package at.photosniper.util;

import android.annotation.TargetApi;

/**
 * Created by johannes on 13.01.2018.
 */

@TargetApi(21)
class BLECommunicator {

//    private static final String TAG = ScanActivity.class.getSimpleName();
//    private static final long SCAN_TIMEOUT_MS = 10_000;
//    private static final int REQUEST_ENABLE_BT = 1;
//    private static final int REQUEST_PERMISSION_LOCATION = 1;
//
//    private boolean mScanning;
//
//    private final BluetoothLeScannerCompat mScanner = BluetoothLeScannerCompat.getScanner();
//    private final Handler mStopScanHandler = new Handler();
//    private final Runnable mStopScanRunnable = new Runnable() {
//        @Override
//        public void run() {
//              stopLeScan();
//        }
//    };
//
//    private void stopLeScan() {
//        if (mScanning) {
//            mScanning = false;
//
//            mScanner.stopScan(scanCallback);
//            mStopScanHandler.removeCallbacks(mStopScanRunnable);
//
//        }
//    }
//
//    private void startLeScan() {
//        mScanning = true;
//
//        ScanSettings settings = new ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                .setReportDelay(1000)
//                .build();
//        List<ScanFilter> filters = new ArrayList<ScanFilter>();
//        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build());
//        mScanner.startScan(filters, settings, scanCallback);
//
//        // Stops scanning after a pre-defined scan period.
//        mStopScanHandler.postDelayed(mStopScanRunnable, SCAN_TIMEOUT_MS);
//
//    }
//
//    private void prepareForScan() {
//        if (isBleSupported()) {
//            // Ensures Bluetooth is enabled on the device
//            BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//            BluetoothAdapter btAdapter = btManager.getAdapter();
//            if (btAdapter.isEnabled()) {
//                // Prompt for runtime permission
//                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                    startLeScan();
//                } else {
//                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
//                }
//            } else {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            }
//        } else {
//            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_LONG).show();
//            finish();
//        }
//    }
//
//
//    private final ScanCallback scanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            // We scan with report delay > 0. This will never be called.
//            Log.i(TAG, "onScanResult: " + result.getDevice().getAddress());
//        }
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            if (!results.isEmpty()) {
//                ScanResult result = results.get(0);
//
//                BluetoothDevice device = result.getDevice();
//                String deviceAddress = device.getAddress();
//                // Device detected, we can automatically connect to it and stop the scan
//
//                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
//                mGatt = device.connectGatt(mContext, false, mGattCallback);
//
//            }
//        }
//
//        @Override
//        public void onScanFailed(int errorCode) {
//            // Scan error
//        }
//    };
//
//
//
//    boolean init(Context context)
//    {
//        boolean bt_available = false;
//
//        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//
//            BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
//            ScanSettings settings = new ScanSettings.Builder()
//                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000)
//                    .setUseHardwareBatchingIfSupported(false).build();
//
//            List<ScanFilter> filters = new ArrayList<ScanFilter>();
//            filters.add(new ScanFilter.Builder().setServiceUuid(SERVICE_UUID).build());
//            scanner.startScan(filters, settings, scanCallback);
//
//        }
//        else
//        {
//            Toast.makeText(context, R.string.BLE_Not_Supported,Toast.LENGTH_SHORT).show();
//        }
//
//        return bt_available;
//    }
//
//
//    void stop()
//    {
//        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
//        scanner.stopScan(scanCallback);
//    }
//

}
