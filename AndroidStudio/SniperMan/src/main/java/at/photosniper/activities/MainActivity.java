package at.photosniper.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.praetoriandroid.cameraremote.LiveViewFetcher;
import com.praetoriandroid.cameraremote.rpc.BaseResponse;

import java.util.ArrayList;
import java.util.List;

import at.photosniper.PhotoSniperApp;
import at.photosniper.R;
import at.photosniper.fragments.BrampingFragment;
import at.photosniper.fragments.DistanceLapseFragment;
import at.photosniper.fragments.GettingStartedFragment;
import at.photosniper.fragments.HdrFragment;
import at.photosniper.fragments.HdrTimeLapseFragment;
import at.photosniper.fragments.LightSensorFragment;
import at.photosniper.fragments.NdCalculatorFragment;
import at.photosniper.fragments.PhotoSniperBaseFragment;
import at.photosniper.fragments.PlaceHolderFragment;
import at.photosniper.fragments.PressHoldFragment;
import at.photosniper.fragments.PulseSequenceFragment;
import at.photosniper.fragments.QuickReleaseFragment;
import at.photosniper.fragments.ScriptExecuteFragment;
import at.photosniper.fragments.SelfTimerFragment;
import at.photosniper.fragments.SimpleReleaseFragment;
import at.photosniper.fragments.SoundSensorFragment;
import at.photosniper.fragments.StarTrailFragment;
import at.photosniper.fragments.StartStopFragment;
import at.photosniper.fragments.StartStopFragment.StartStopListener;
import at.photosniper.fragments.SunriseSunsetFragment;
import at.photosniper.fragments.TimeLapseOldFragment;
import at.photosniper.fragments.TimeWarpFragment;
import at.photosniper.fragments.TimedFragment;
import at.photosniper.fragments.dialog.ErrorPlayServicesFragment;
import at.photosniper.fragments.dialog.RunningActionDialog;
import at.photosniper.fragments.handler.DrawerFragmentHandler;
import at.photosniper.location.PhotoSniperLocationService;
import at.photosniper.service.PhotoSniperService;
import at.photosniper.service.PhotoSniperService.PhotoSniperServiceBinder;
import at.photosniper.service.PhotoSniperService.PhotoSniperServiceListener;
import at.photosniper.util.AppRater;
import at.photosniper.util.DialpadManager;
import at.photosniper.util.GattClient;
import at.photosniper.util.SonyWiFiRPC;
import at.photosniper.util.WarningMessageManager;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


public class MainActivity extends Activity implements PulseSequenceFragment.PulseSequenceListener, PhotoSniperServiceListener, TimedFragment.TimedListener, StartStopListener, SoundSensorFragment.SoundSensorListener, LightSensorFragment.LightSensorListener, SelfTimerFragment.SelfTimerListener, PressHoldFragment.PressHoldListener, SimpleReleaseFragment.SimpleModeListener, QuickReleaseFragment.QuickReleaseListener, DialpadManager.InputSelectionListener, DistanceLapseFragment.DistanceLapseListener, SonyWiFiRPC.SonyWiFiConnectionListener, SonyWiFiRPC.ResponseHandler, SonyWiFiRPC.LiveViewCallback, ScriptExecuteFragment.ScriptExecutionListener {

    // Saved instance keys
    public static final String FRAGMENT_STATE = "fragment_state";
    public static final String FRAGMENT_TAG = "fragment_tag";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long SCAN_TIMEOUT_MS = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_LOCATION = 1;
    private final BluetoothLeScannerCompat mScanner = BluetoothLeScannerCompat.getScanner();
    private final Handler mStopScanHandler = new Handler();
    private final ArrayList<String[]> mModes = new ArrayList<>();
    private final ArrayList<String[]> mModesSubText = new ArrayList<>();
    private final ArrayList<TypedArray> mModeIcons = new ArrayList<>();
    private final Bundle mInitialFragmentState = null;
    private Animation mSlideUpFromBottom;
    private Animation mSlideDownToBottom;
    private int mShotsTakenCount = 0;
    private String mAppName;
    private String mSelectedItemName;
    private String[] mModesGroups;
    private ExpandableListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private View mStatusBar;
    private TextView mStatusBarText;
    private PhotoSniperLocationService mLocationService = null;
    private DrawerFragmentHandler mDrawerFragHandler = null;
    private String mInitialFragmentTag = PhotoSniperApp.FragmentTags.GETTING_STARTED;
    // BLE --- start
    // Service params
    private Dialog serviceErrorDialog = null;
    private PhotoSniperService mService;
    private boolean mPhotoSniperServiceBound = false;
    /*
     * Service binding and handling code
     */
    private final ServiceConnection mPhotoSniperServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            Log.d(TAG, "Service connected");
            PhotoSniperServiceBinder binder = (PhotoSniperServiceBinder) service;
            mService = binder.getService();
            Log.d(TAG, "Service connected: " + mService.toString());
            mPhotoSniperServiceBound = true;
            // Make sure the service is stopped just in case we started it in
            // foreground.
            // when we left the Main Activity
            Intent intent = new Intent(MainActivity.this, PhotoSniperService.class);
            mService.goTobackground();
            stopService(intent);
            mService.setListener(MainActivity.this);
            // Check if we need to display the status bar
            displaySatusBar();
            // Check we if need to set the transient state
            setFragmentTransientState();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onService Disconnected ");
            mPhotoSniperServiceBound = false;
        }
    };
    private DialpadManager mDialPadManager = null;
    private WarningMessageManager mWarningMessageManager;
    private boolean mScanning;
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // We scan with report delay > 0. This will never be called.
            Log.i(TAG, "onScanResult: " + result.getDevice().getAddress());
            stopLeScan();
            connectBLE(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            if (!results.isEmpty()) {
                Log.i(TAG, "onBatchScanResults: " + results.toString());
                ScanResult result = results.get(0);
                stopLeScan();
                connectBLE(result.getDevice());
            } else {
                Log.i(TAG, "onBatchScanResults: EMPTY");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "Scan failed: " + errorCode);
            stopLeScan();
        }
    };
    private final Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
            stopLeScan();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup default values of preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        boolean hasBeenRotated = false;
        // Check for a saved instance (Created when the device is rotated)
//        if (savedInstanceState != null) {
////            Log.d(TAG, "onCreate Saved Instance state:" + savedInstanceState);
//            mInitialFragmentTag = savedInstanceState.getString(FRAGMENT_TAG);
//            mInitialFragmentState = savedInstanceState.getBundle(FRAGMENT_STATE);
//
//            hasBeenRotated = true;
//        }


        // For some reason launching from the history does not clear previous
        // intent
        // So we have to check this and ignore any intent extras.
        boolean isLauchedFromHistory = false;
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            Log.d(TAG, "FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY");
            isLauchedFromHistory = true;
        }

        // Check if this activity was passed an intent with extras
        // This would happen when we re-launch the App' from the notification
        // bar
        Bundle extras = getIntent().getExtras();
        if (extras != null && !isLauchedFromHistory && !hasBeenRotated) {

            String activeFragTag = extras.getString(FRAGMENT_TAG);
            Log.d(TAG, "Got extras from Intent setting initial Fragment: " + activeFragTag);
            if (activeFragTag != null) {
                if (!activeFragTag.equals(PhotoSniperApp.FragmentTags.NONE)) {
                    mInitialFragmentTag = activeFragTag;
                }
            }
        } else {
            mInitialFragmentTag = PhotoSniperApp.getInstance(getApplicationContext()).getLastFragmentTag();
        }

        Log.d(TAG, "onCreate mInitialFragmentState: " + mInitialFragmentState);
        Log.d(TAG, "onCreate Back stack count: " + getFragmentManager().getBackStackEntryCount());

        setContentView(R.layout.activity_main);

        setUpStatusBar();
        mAppName = getResources().getString(R.string.app_name);

        setUpFragmentHandler(hasBeenRotated);

        setUpModes();
        setUpNavigationDrawer();
        setUpDailPad();

        // Setup the warning message manager
        mWarningMessageManager = new WarningMessageManager(getApplicationContext(), findViewById(R.id.warningMessage));
        mWarningMessageManager.startListening();

        // Initialise App settings;
        PhotoSniperApp.getInstance(this);


        // Initialise Location service
        mLocationService = new PhotoSniperLocationService(this);

        if (!mPhotoSniperServiceBound) {
            Intent intent = new Intent(this, PhotoSniperService.class);
            bindService(intent, mPhotoSniperServiceConnection, Activity.BIND_AUTO_CREATE);
        }

        // getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION);

        mSlideUpFromBottom = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_bottom);
        mSlideUpFromBottom.setInterpolator(new OvershootInterpolator(0.5f));
        mSlideUpFromBottom.setStartOffset(300);
        mSlideDownToBottom = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom);

        if (!hasBeenRotated) {
            AppRater.appLaunched(getApplicationContext(), this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        PhotoSniperApp.getInstance(this).setSonyWiFiRpc(new SonyWiFiRPC());
        PhotoSniperApp.getInstance(this).getSonyWiFiRpc().registerInitCallback(this);

        attemptSonyConnect();

        // Bind to the PhotoSniper service when the actvity is shown.
        Log.d(TAG, "Service bound is: " + mPhotoSniperServiceBound);

        Intent intent = new Intent(MainActivity.this, PhotoSniperService.class);
        if (mService != null) {
            mService.goTobackground();
            stopService(intent);
            // Do we need to set the transientState of the Fragment
            setFragmentTransientState();
        }


    }

    private void attemptSonyConnect() {


        if (!PhotoSniperApp.getInstance(this).isSonyRPCAvailable()) {
            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {

                    PhotoSniperApp.getInstance(MainActivity.this).getSonyWiFiRpc().connect();

                    return "";
                }

            };
            task.execute();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

//        outState.putString(FRAGMENT_TAG,
//                mDrawerFragHandler.getCurrentFragmentTag());
//
//        // Get the current visible Fragment and save the transient state
//        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
//        Fragment fragment = getFragmentManager().findFragmentByTag(
//                currentFragTag);
//        Bundle fragmentState = null;
//        if (fragment != null && fragment.isVisible()) {
//            if (fragment instanceof PhotoSniperBaseFragment) {
//                PhotoSniperBaseFragment ttFragment = (PhotoSniperBaseFragment) fragment;
//                fragmentState = ttFragment.getStateBundle();
//            }
//        }
//        outState.putBundle(FRAGMENT_STATE, fragmentState);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (PhotoSniperApp.getInstance(this).getSonyWiFiRpc() != null) {
            PhotoSniperApp.getInstance(this).getSonyWiFiRpc().unregisterInitCallback(this);
            PhotoSniperApp.getInstance(this).getSonyWiFiRpc().stopLiveView();
        }

        // Save the last shown Fragment Tag
        PhotoSniperApp.getInstance(this).setLastFragmentTag(mDrawerFragHandler.getCurrentFragmentTag());
        PhotoSniperApp.getInstance(this).setLastActionBarLabel(mSelectedItemName);
        PhotoSniperApp.getInstance(this).setLastListItemChecked(mDrawerList.getCheckedItemPosition());

        if (isFinishing()) {
            Log.d(TAG, "MainActivity is Finishing");
        }
        Log.d(TAG, "Stopping Activity, isChangingConfigurations: " + isChangingConfigurations() + " Service state: " + ((mService != null) ? mService.getState() : "NULL"));
        // Make sure we reset the intent data
        setIntent(new Intent());

        // if (mService.getState() == PhotoSniperService.State.IN_PROGRESS ) {
        // Keep the service alive we are just rotating.
        if (isChangingConfigurations()) {
            Log.d(TAG, "Starting in progress service to keep it alive");
            Intent intent = new Intent(this, PhotoSniperService.class);
            startService(intent);
        }

        // If not changing configuration, Activity is not visible so run service
        // in foreground
        if (mService != null) {
            if (!isChangingConfigurations() && mService.getState() == PhotoSniperService.State.IN_PROGRESS) {
                Intent intent = new Intent(this, PhotoSniperService.class);
                startService(intent);
                mService.goToForeground();
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Flushing Mixpanel");
        //AnalyticTracker.getInstance(this).flush();
        Log.d(TAG, "Destroying activity");
        // Only unbind from the service if the activity has been destroyed
        if (mPhotoSniperServiceBound) {
            Log.d(TAG, "Unbinding service");
            // mService.setListener(null);
            unbindService(mPhotoSniperServiceConnection);
            mPhotoSniperServiceBound = false;
        }

        // BLE
        if (PhotoSniperApp.getInstance(this).getBLEgattClient() != null) {
            PhotoSniperApp.getInstance(this).getBLEgattClient().onDestroy();
            PhotoSniperApp.getInstance(this).setBLEgattClient(null);
        }

        mWarningMessageManager.stopListening();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDialPadManager.getDialPadState() == DialpadManager.DialPadState.SHOWING) {
            mDialPadManager.deactiveInput();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Returning request code: " + requestCode);
        Log.d(TAG, "Returning result code: " + resultCode);

        switch (requestCode) {

            case PhotoSniperApp.OnGoingAction.SCRIPT:
                // this is the response of filechooser
                if (resultCode == RESULT_OK) {
                    Uri selectedFile = data.getData();

                    ScriptExecuteFragment scriptFragment = (ScriptExecuteFragment) getFragmentManager().findFragmentByTag(PhotoSniperApp.FragmentTags.SCRIPT);
                    scriptFragment.setScriptFile(selectedFile);
                }

                break;

            case REQUEST_ENABLE_BT:

                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "You must turn Bluetooth on, to use this app", Toast.LENGTH_LONG).show();
//                    finish();  // ?? sure ?!
                } else {
                    prepareForScan();
                }
                break;

            // Handle the result of checking the Location service.
            case PhotoSniperLocationService.CONNECTION_FAILURE_RESOLUTION_REQUEST:
                if (serviceErrorDialog != null) {
                    serviceErrorDialog.dismiss();
                    serviceErrorDialog = null;
                }
            /*
             * If the result code is Activity.RESULT_OK, try to connect again
			 */
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Call servicesConnected again to check that all is ok now.
                        DistanceLapseFragment distanceFragment = (DistanceLapseFragment) getFragmentManager().findFragmentByTag(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE);
                        if (mLocationService.servicesConnected(serviceErrorDialog)) {
                            if (mService != null) {
                                mService.setTTLocationService(mLocationService);
                            }
                            if (distanceFragment != null && distanceFragment.isVisible()) {
                                distanceFragment.setDistanceLapseState();
                            }
                        }
                        break;

                    case Activity.RESULT_CANCELED:
                        // Display dialog: TT uses Google Play Location services to get
                        // location information, you cannot use distance lapse without a
                        // valid Google
                        // play account or Google Play installed.
                        ErrorPlayServicesFragment errorPlayServices = new ErrorPlayServicesFragment();
                        errorPlayServices.show(this);

                        break;
                }

        }

    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    public void stopRunningAction() {
        mService.stopCurrentAction();
        displaySatusBar();
    }

    public boolean checkInProgressState() {
        return mService.checkInProgressState();
    }

    private String getNotifcationText(int onGoingAction) {
        String[] notifcations = getResources().getStringArray(R.array.ps_notifications);
        return notifcations[onGoingAction];
    }

    private void setUpStatusBar() {

        mStatusBar = findViewById(R.id.status);
        mStatusBarText = (TextView) findViewById(R.id.status_bar_text);

        View stopRunningAction = findViewById(R.id.stopRunningAction);
        stopRunningAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStatusBar.startAnimation(mSlideDownToBottom);
                stopRunningAction();
            }
        });
    }

    private void setUpFragmentHandler(boolean hasBeenRotated) {
        mDrawerFragHandler = new DrawerFragmentHandler(getFragmentManager(), R.id.content_frame);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.PLACEHOLDER, PlaceHolderFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.GETTING_STARTED, GettingStartedFragment.class, mInitialFragmentState);

        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.SIMPLE, SimpleReleaseFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.QUICK_RELEASE, QuickReleaseFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.PRESS_AND_HOLD, PressHoldFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.PRESS_TO_START, StartStopFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.TIMED, TimedFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.SELF_TIMER, SelfTimerFragment.class, mInitialFragmentState);

        // mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.TIMELAPSE, TimeLapseOldFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.TIMEWARP, TimeWarpFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.STARTRAIL, StarTrailFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.BRAMPING, BrampingFragment.class, mInitialFragmentState);

        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.BANG, SoundSensorFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.BANG2, LightSensorFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE, DistanceLapseFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.SCRIPT, ScriptExecuteFragment.class, mInitialFragmentState);

        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.HDR, HdrFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.HDR_LAPSE, HdrTimeLapseFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.SUNRISESUNSET, SunriseSunsetFragment.class, mInitialFragmentState);
        mDrawerFragHandler.addDrawerPane(PhotoSniperApp.FragmentTags.ND_CALCULATOR, NdCalculatorFragment.class, mInitialFragmentState);

        mDrawerFragHandler.onDrawerSelected(this, mInitialFragmentTag, false, hasBeenRotated);

        // if(PhotoSniperApp.getInstance(this).isFirstStarted()) {
        // mDrawerFragHandler.addBackstackFragment(this, WelcomeFragment.class);
        // }

    }

    private void setUpModes() {

        mModesGroups = getResources().getStringArray(R.array.ps_mode_groups);

        mModes.add(getResources().getStringArray(R.array.ps_welcome_modes));
        mModes.add(getResources().getStringArray(R.array.ps_simple_modes));
        mModes.add(getResources().getStringArray(R.array.ps_timer_modes));
        mModes.add(getResources().getStringArray(R.array.ps_sensor_modes));
        mModes.add(getResources().getStringArray(R.array.ps_hdr_modes));
//        mModes.add(remoteTriggerModes);
        mModes.add(getResources().getStringArray(R.array.ps_calculator_modes));
//        mModes.add(getResources().getStringArray(R.array.ps_settings_modes));

        mModesSubText.add(getResources().getStringArray(R.array.ps_welcome_modes_sub_text));
        mModesSubText.add(getResources().getStringArray(R.array.ps_cable_modes_sub_text));
        mModesSubText.add(getResources().getStringArray(R.array.ps_timer_modes_sub_text));
        mModesSubText.add(getResources().getStringArray(R.array.ps_sensor_modes_sub_text));
        mModesSubText.add(getResources().getStringArray(R.array.ps_hdr_modes_sub_text));

//        mModesSubText.add(getResources().getStringArray(
//                R.array.ps_remote_trigger_modes_sub_text));
        mModesSubText.add(getResources().getStringArray(R.array.ps_calculators_sub_text));
//        mModesSubText.add(getResources().getStringArray(
//                R.array.ps_settings_modes_sub_text));

        // Setup icons
        mModeIcons.add(getResources().obtainTypedArray(R.array.ps_welcome_mode_icons));
        mModeIcons.add(getResources().obtainTypedArray(R.array.ps_cable_mode_icons));
        mModeIcons.add(getResources().obtainTypedArray(R.array.ps_timer_mode_icons));
        mModeIcons.add(getResources().obtainTypedArray(R.array.ps_sensor_mode_icons));
        mModeIcons.add(getResources().obtainTypedArray(R.array.ps_hdr_mode_icons));
//        mModeIcons.add(getResources().obtainTypedArray(
//                R.array.ps_remote_trigger_mode_icons));
        mModeIcons.add(getResources().obtainTypedArray(R.array.ps_calculators_mode_icons));
//        mModeIcons.add(getResources().obtainTypedArray(
//                R.array.ps_settings_mode_icons));
    }

    private void setUpNavigationDrawer() {
        DrawerExpandableListAdapter drawListAdapter = new DrawerExpandableListAdapter();
        mDrawerList = (ExpandableListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(drawListAdapter);
        mDrawerList.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true; // This way the expander cannot be collapsed
            }
        });

        // Make sure all the Expandable List Groups are open
        int count = drawListAdapter.getGroupCount();
        for (int position = 1; position <= count; position++) {
            mDrawerList.expandGroup(position - 1);
        }

        mDrawerList.setOnChildClickListener(new DrawerItemClickListener());
        int index = PhotoSniperApp.getInstance(this).getLastListItemChecked();
        mDrawerList.setItemChecked(index, true);
        mSelectedItemName = PhotoSniperApp.getInstance(getApplicationContext()).getLastActionBarLabel();
        setTitle(mSelectedItemName);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.nav_drawer_open, R.string.nav_drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mSelectedItemName);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
                // changeFragment();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                dismissFragmentError();
                getActionBar().setTitle(mAppName);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                dismissFragmentError();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }

    private void setUpDailPad() {
        View dialPad = findViewById(R.id.dialPad);
        mDialPadManager = new DialpadManager(this, dialPad);
        dialPad.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Do nothing just comsume the click.

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        dismissFragmentError();
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.action_ble: {
                Intent intent = new Intent(this, BLEDeviceScanActivity.class);
                startActivity(intent);
                break;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectItem(int group, int position, int listItemIndex) {
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(listItemIndex, true);
        mSelectedItemName = mModes.get(group)[position];
        setTitle(mSelectedItemName);
        if (mDialPadManager.getDialPadState() == DialpadManager.DialPadState.SHOWING) {
            mDialPadManager.deactiveInput();
        }
        changeFragment(group, position, listItemIndex);
        // setWifiState();
        setFragmentTransientState();
        displaySatusBar();
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    private void changeFragment(int group, int position, int listItemIndex) {
        // Only need this for getting rid of the welcome fragment at the moment.
        // FragmentManager fm = getFragmentManager();
        // fm.popBackStack();
        switch (group) {
            case DrawerGroups.WELCOME:
                if (position == 0) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.GETTING_STARTED, true, false);
//                } else {
//                    mDrawerFragHandler.onDrawerSelected(this,
//                            PhotoSniperApp.FragmentTags.BUY_DONGLE, true, false);
                }
                break;
            case DrawerGroups.SIMPLE_MODES:
                if (position == 0) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.SIMPLE, true, false);
                } else if (position == 1) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.QUICK_RELEASE, true, false);
                } else if (position == 2) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.PRESS_AND_HOLD, true, false);
                } else if (position == 3) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.PRESS_TO_START, true, false);
                } else if (position == 4) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.TIMED, true, false);
                } else if (position == 5) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.SELF_TIMER, true, false);
                }
                break;
            case DrawerGroups.TIME_MODE:

                if (position == 0) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.TIMEWARP, true, false);
                } else if (position == 1) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.STARTRAIL, true, false);
                } else if (position == 2) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.BRAMPING, true, false);
                }

                break;
            case DrawerGroups.SENSOR_MODES:
                if (position == 0) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.BANG, true, false);
                } else if (position == 1) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.BANG2, true, false);
                } else if (position == 2) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.DISTANCE_LAPSE, true, false);
                    if (mLocationService.servicesConnected(serviceErrorDialog)) {
                        if (mService != null) {
                            mService.setTTLocationService(mLocationService);
                        }
                        DistanceLapseFragment distanceFragment = (DistanceLapseFragment) getFragmentManager().findFragmentByTag(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE);
                        if (distanceFragment != null) {
                            distanceFragment.setDistanceLapseState();
                        }
                    }
                } else if (position == 3) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.SCRIPT, true, false);
                }

                break;

            case DrawerGroups.HDR_MODES:
                if (position == 0) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.HDR, true, false);
                } else if (position == 1) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.HDR_LAPSE, true, false);
                }
                break;

            case DrawerGroups.CALCULATORS:
                if (position == 0) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.SUNRISESUNSET, true, false);
                } else if (position == 1) {
                    mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.ND_CALCULATOR, true, false);
                }
                break;
            default:
                mDrawerFragHandler.onDrawerSelected(this, PhotoSniperApp.FragmentTags.PLACEHOLDER, true, false);
        }
    }

    private void dismissFragmentError() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
        if (fragment != null) {
            PhotoSniperBaseFragment ttFragment = (PhotoSniperBaseFragment) fragment;
            ttFragment.dismissError();

        }
    }

    /**
     * The Fragment may be running an Action we need to check with Service
     */
    private void setFragmentTransientState() {
        Log.d(TAG, "Setting Transitent state");
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
        if (fragment != null) {
            PhotoSniperBaseFragment ttFragment = (PhotoSniperBaseFragment) fragment;
            ttFragment.setActionState(mService.isFragmentActive(currentFragTag));

            // If we are showing the sound sensor make sure we are running the
            // MicVolumeMonitor
            if (ttFragment instanceof SoundSensorFragment) {
                onStartSoundSensor();
            }

            // If we have an inactive distance lapse make sure the Location
            // service is register and running
            if (!(mService.isFragmentActive(currentFragTag) && ttFragment instanceof DistanceLapseFragment)) {
                if (mLocationService.servicesConnected(serviceErrorDialog)) {
                    if (mService != null) {
                        Log.d(TAG, "Setting Location Service");
                        mService.setTTLocationService(mLocationService);
                    }
                    DistanceLapseFragment distanceFragment = (DistanceLapseFragment) getFragmentManager().findFragmentByTag(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE);
                    if (distanceFragment != null) {
                        distanceFragment.setDistanceLapseState();
                    }
                }

            }

            // If we have distance lapse makes sure we update its progress
            // appropriately
            if (mService.isFragmentActive(currentFragTag) && ttFragment instanceof DistanceLapseFragment) {
                DistanceLapseFragment disrFrag = (DistanceLapseFragment) ttFragment;
                disrFrag.onDistanceLapseUpdate(mService.getAccumulativeDistance(), mService.getSpeed());
            }

            displaySatusBar();
        }
    }

    private void displaySatusBar() {
        if (mService.getState() == PhotoSniperService.State.IN_PROGRESS) {
            Log.d(TAG, "Service State is IN_PROGRESS");
            String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                if (fragment instanceof PhotoSniperBaseFragment) {
                    PhotoSniperBaseFragment photosniperFragment = (PhotoSniperBaseFragment) fragment;
                    if (photosniperFragment.getRunningAction() != mService.getOnGoingAction()) {
                        mStatusBar.setVisibility(View.VISIBLE);
                        mStatusBarText.setText(getNotifcationText(mService.getOnGoingAction()));
                        mStatusBar.startAnimation(mSlideUpFromBottom);
                        // //Animating the status bar in
                        // mStatusBar.setRotationX(90);
                        // mStatusBar.setPivotY(100);
                        // mStatusBar.setPivotX(100);
                        // mStatusBar.setAlpha(0.5f);
                        // mStatusBar.animate().rotationX(0).alpha(1.0f).setDuration(500).setInterpolator(new
                        // OvershootInterpolator(5.0f));
                    } else {
                        // Animating the status bar out
                        // mStatusBar.setPivotY(100);
                        // mStatusBar.setPivotX(100);
                        // mStatusBar.animate().rotationX(90).alpha(0.5f).setDuration(800);
                        mStatusBar.startAnimation(mSlideDownToBottom);
                        mStatusBar.setVisibility(View.INVISIBLE);
                    }
                }
            }
        } else {
            Log.d(TAG, "Service State is IDLE");
            // Animating the status bar out
            // mStatusBar.setPivotY(100);
            // mStatusBar.setPivotX(100);
            // mStatusBar.animate().rotationX(90).alpha(0.5f).setDuration(800);
            mStatusBar.setVisibility(View.INVISIBLE);
        }
    }

    private void removeStatusBar() {
        mStatusBar.startAnimation(mSlideDownToBottom);
        mStatusBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and
     * onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }



    /*
     * Listener for PulseSequence Fragments
     */

    /*
     * Listener for Script Execution
     */
    @Override
    public void onExecuteScript(final String cmdSequence) {

        if (!mService.runBatchInsteadPulse(cmdSequence)) {
            Toast.makeText(this, "BLE Cmd Parsing failed!", Toast.LENGTH_LONG).show();
        }

    }

    /*
     * Listener for Simple mode
     */
    @Override
    public void onPressSimple() {
        mService.startSimple();
    }

    @Override
    public void onRunBatchInsteadPulse(final String cmdSequence) {
        mService.runBatchInsteadPulse(cmdSequence);
    }

    @Override
    public void onPulseSequenceCreated(int onGoingAction, long[] sequence, boolean repeat) {
        if (mPhotoSniperServiceBound) {
            Log.d(TAG, "Starting Pulse sequence: Action:" + onGoingAction);
            mService.startPulseSequence(onGoingAction, sequence, repeat);
            mService.resetSequenceStartStopTime();
        }
    }

    @Override
    public void onPulseSequenceCancelled() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);

        switch (mService.getOnGoingAction()) {
            case PhotoSniperApp.OnGoingAction.TIMEWARP:

                TimeWarpFragment timewarpFrag = (TimeWarpFragment) fragment;
                if (timewarpFrag != null) {
                    timewarpFrag.onPulseStop();

//                    long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

                }
                break;
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                StarTrailFragment startFrag = (StarTrailFragment) fragment;
                if (startFrag != null) {

//                    long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

                }

                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
                BrampingFragment brampFrag = (BrampingFragment) fragment;
                if (brampFrag != null) {
                    brampFrag.onPulseStop();

//                    long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

                }

                break;
            case PhotoSniperApp.OnGoingAction.HDR:
                HdrFragment hdrFragment = (HdrFragment) fragment;
                if (hdrFragment != null) {
//                    long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

                }

                break;
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:

                HdrTimeLapseFragment hdrTimeLapseFragment = (HdrTimeLapseFragment) fragment;
                if (hdrTimeLapseFragment != null) {
//                    long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

                }
                break;

            case PhotoSniperApp.OnGoingAction.TIMELAPSE:

//                long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

//                TimeLapseOldFragment timeLapseFragment = (TimeLapseOldFragment) fragment;


                break;
        }

        if (mPhotoSniperServiceBound) {
            mService.stopSequence();
        }
    }

    /*
     * Listener for TimedFragment Fragments
     */
    @Override
    public void onTimedStarted(long time) {
        mService.startTimedMode(time);
    }

    @Override
    public void onTimedStopped() {
        mService.stopTimedMode();

    }

    /**
     * SelfTimer Listener functions
     */

    @Override
    public void onSelfTimerStarted(long time) {
        mService.startSelfTimer(time);
    }

    @Override
    public void onSelfTimerStopped() {
        //Cancel timer
        mService.stopSelfTimerMode();
    }

    /*
     * Listener for StartStop Fragment
     */
    @Override
    public void onStopwatchStarted() {
        mService.startStopwatch();

    }

    @Override
    public void onStopwatchStopped() {
        mService.stopStopWatch();

    }

    /*
     * Listener for press hold fragment Fragment
     */
    @Override
    public void onPressStarted() {
        mService.onStartPress();
    }

    @Override
    public void onPressStopped() {
        mService.onStopPress();
    }

    /*
     * Listener for SoundSensor (Bang) Fragment
     */
    @Override
    public void onStartSoundSensor() {
        if (mService != null) {
            mService.startSoundSensor();
            mService.resetSequenceStartStopTime();
        }

    }

    @Override
    public void onStopSoundSensor() {
        if (mService != null) {
            mService.stopSoundSensor();
        }
    }

    @Override
    public void onEnableSoundThreshold() {
        mService.enableSoundThreshold();

    }

    @Override
    public void onDisableSoundThreshold() {
        mService.disableSoundThreshold();

//        long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

        mShotsTakenCount = 0;

    }

    @Override
    public void onSetMicSensitivity(int sensitivity) {
        if (mService != null) {
            mService.setMicSensitivity(sensitivity);
        }
    }

    @Override
    public void onSetSoundThreshold(int threshold) {
        if (mService != null) {
            mService.setSoundThreshold(threshold);
        }
    }

    /*
     * Listener for LightSensor (Bang2) Fragment
     */
    @Override
    public void onStartLightSensor() {
        if (mService != null) {
            mService.startLightSensor();
            mService.resetSequenceStartStopTime();
        }

    }

    @Override
    public void onStopLightSensor() {
        if (mService != null) {
            mService.stopLightSensor();
        }
    }

    @Override
    public void onEnableLightThreshold() {
        mService.enableLightThreshold();

    }

    @Override
    public void onDisableLightThreshold() {
        mService.disableLightThreshold();

//        long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

        mShotsTakenCount = 0;

    }

    @Override
    public void onSetLightSensorSensitivity(int sensitivity) {
        if (mService != null) {
            mService.setLightSensitivity(sensitivity);
        }
    }

    @Override
    public void onSetLightSensorThreshold(int threshold) {
        if (mService != null) {
            mService.setLightThreshold(threshold);
        }
    }

    /*
     * Listener for DistanceLapse Fragment
     */
    @Override
    public void onStartDistanceLapse(int distance) {
        if (mService != null) {
            mService.startLocationUpdates(distance);
            mService.resetSequenceStartStopTime();
        }
    }

    @Override
    public void onStopDistanceLapse() {
        if (mService != null) {
            mService.stopLocationUpdates();

//            Fragment fragment = getFragmentManager().findFragmentByTag(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE);

//            DistanceLapseFragment distanceLapsefragment = (DistanceLapseFragment) fragment;

//            long sequenceMillis = Calendar.getInstance().getTimeInMillis() - mService.getSequenceStartStopTime().getTimeInMillis();

        }
    }

    /**
     * Listeners for the Triggertrap service
     */
    @Override
    public void onServiceActionRunning(String action) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        PhotoSniperBaseFragment ttFragment = (PhotoSniperBaseFragment) getFragmentManager().findFragmentByTag(currentFragTag);
        if (ttFragment != null) {
            ttFragment.setActionState(false);
        }
        RunningActionDialog runningActionDialog = new RunningActionDialog();
        runningActionDialog.show(this, action);
    }
    /*
    * Listener for quick release Fragment
     */

    @Override
    public void onServiceStartSimple() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();

    }

    public void onQuickPressStarted(final String command) {
        mService.onQuickPressStart(command);
    }

    public void onQuickPressStopped(final String command) {
        mService.onQuickPressStop(command);
    }

    @Override
    public void onServicePressStart() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.PRESS_AND_HOLD) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.PRESS_AND_HOLD) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            PressHoldFragment pressHoldFragment = (PressHoldFragment) fragment;
            pressHoldFragment.startStopwatch();
        } else if (currentFragTag.equals(PhotoSniperApp.FragmentTags.QUICK_RELEASE) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.QUICK_RELEASE) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            QuickReleaseFragment quickReleaseFragment = (QuickReleaseFragment) fragment;
            quickReleaseFragment.startStopwatch();

        }
    }

    @Override
    public void onServicePressUpdate(long time) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();

        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.PRESS_AND_HOLD) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.PRESS_AND_HOLD) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                PressHoldFragment pressHoldFragment = (PressHoldFragment) fragment;
                pressHoldFragment.updateStopwatch(time);
            }
        } else if (currentFragTag.equals((PhotoSniperApp.FragmentTags.QUICK_RELEASE))) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                QuickReleaseFragment quickReleaseFragmentFragment = (QuickReleaseFragment) fragment;
                quickReleaseFragmentFragment.updateStopwatch(time);
            }

        }

    }

    @Override
    public void onServicePressStop() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();

        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.PRESS_AND_HOLD)) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                PressHoldFragment pressHoldFragment = (PressHoldFragment) fragment;
                pressHoldFragment.stopStopwatch();

            }
            // Check if we need to remove the status bar
            // displaySatusBar();
        } else if (currentFragTag.equals((PhotoSniperApp.FragmentTags.QUICK_RELEASE))) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                QuickReleaseFragment quickReleaseFragmentFragment = (QuickReleaseFragment) fragment;
                quickReleaseFragmentFragment.stopStopwatch();

            }
        }
    }

    @Override
    public void onServiceStopwatchStart() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.PRESS_TO_START) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.PRESS_START_STOP) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            StartStopFragment startFragment = (StartStopFragment) fragment;
            startFragment.startStopwatch();
        }

    }

    @Override
    public void onServiceStopwatchUpdate(long time) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.PRESS_TO_START) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.PRESS_START_STOP) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                StartStopFragment startStopFrag = (StartStopFragment) fragment;
                startStopFrag.updateStopwatch(time);
            }
        }

    }

    @Override
    public void onServiceStopwatchStop() {
        String startStopFragTag = PhotoSniperApp.FragmentTags.PRESS_TO_START;
        Fragment fragment = getFragmentManager().findFragmentByTag(startStopFragTag);
        if (fragment != null) {
            StartStopFragment startStopFrag = (StartStopFragment) fragment;
            startStopFrag.stopStopwatch();

        }
        // Check if we need to remove the status bar
        // displaySatusBar();

    }

    @Override
    public void onServiceTimedStart(long time) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMED) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.TIMED) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            TimedFragment timedFrag = (TimedFragment) fragment;
            timedFrag.startTimer(time);
        } else if (currentFragTag.equals(PhotoSniperApp.FragmentTags.SELF_TIMER) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.SELF_TIMER) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            SelfTimerFragment timedFrag = (SelfTimerFragment) fragment;
            timedFrag.startTimer(time);
        }

    }

    @Override
    public void onServiceTimedUpdate(long time) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMED) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.TIMED) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                TimedFragment timedFrag = (TimedFragment) fragment;
                timedFrag.updateTimer(time);
            }
        } else if (currentFragTag.equals(PhotoSniperApp.FragmentTags.SELF_TIMER) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.SELF_TIMER) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                SelfTimerFragment timerFrag = (SelfTimerFragment) fragment;
                timerFrag.updateTimer(time);
            }
        }

    }

    @Override
    public void onServiceTimedStop() {
        // Just make sure the TimedFragment has its state reset

        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);

        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMED)) {

            if (fragment != null) {
                TimedFragment timedFrag = (TimedFragment) fragment;
                timedFrag.stopTimer();

            }


        } else if (currentFragTag.equals(PhotoSniperApp.FragmentTags.SELF_TIMER)) {


            String selfTimerFragTag = PhotoSniperApp.FragmentTags.SELF_TIMER;
            fragment = getFragmentManager().findFragmentByTag(selfTimerFragTag);

            if (fragment != null) {
                SelfTimerFragment timerFrag = (SelfTimerFragment) fragment;
                timerFrag.stopTimer();

            }
        }

        if (!currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMED) && !currentFragTag.equals(PhotoSniperApp.FragmentTags.SELF_TIMER)) {
            removeStatusBar();
        }

    }

    @Override
    public void onSoundVolumeUpdate(int amplitude) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.BANG)) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                SoundSensorFragment soundFrag = (SoundSensorFragment) fragment;
                soundFrag.onVolumeUpdate(amplitude);
            }
        }

    }

    @Override
    public void onSoundExceedThreshold(int amplitude) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.BANG) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.BANG) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                SoundSensorFragment soundFrag = (SoundSensorFragment) fragment;
                soundFrag.onExceedVolumeThreshold(amplitude);

                mShotsTakenCount++;
            }
        }

    }

    @Override
    public void onLightUpdate(int amplitude) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.BANG2)) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                LightSensorFragment lightFrag = (LightSensorFragment) fragment;
                lightFrag.onLightUpdate(amplitude);
            }
        }

    }

    @Override
    public void onLightExceedThreshold(int amplitude) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.BANG2) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.BANG2) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                LightSensorFragment lightFrag = (LightSensorFragment) fragment;
                lightFrag.onExceedLightThreshold(amplitude);

                mShotsTakenCount++;
            }
        }

    }

    @Override
    public void onDistanceUpdated(float distanceTraveled, float speed) {
        Log.d(TAG, "onDistanceUpdated: " + distanceTraveled);
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE) && mService.getOnGoingAction() == PhotoSniperApp.OnGoingAction.DISTANCE_LAPSE) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                DistanceLapseFragment disrFrag = (DistanceLapseFragment) fragment;
                disrFrag.onDistanceLapseUpdate(distanceTraveled, speed);
            }
        }

    }

    public void ignoreGPS() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        if (currentFragTag.equals(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE)) {
            Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
            if (fragment != null) {
                DistanceLapseFragment disrFrag = (DistanceLapseFragment) fragment;
                disrFrag.ignoreGPS(true);
            }
        }
    }

    @Override
    public void onPulseStart(int exposures, int totalExposures, long timeToNextExposure, long timeRemaining) {

        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
        if (fragment == null) {
            return;
        }
        switch (mService.getOnGoingAction()) {
            case PhotoSniperApp.OnGoingAction.TIMELAPSE:
                // Is the timelapse fragment currently active?
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMELAPSE)) {
                    TimeLapseOldFragment timeLapseFrag = (TimeLapseOldFragment) fragment;
                    timeLapseFrag.onPulseStarted(exposures, timeToNextExposure);
                }
                break;
            case PhotoSniperApp.OnGoingAction.TIMEWARP:
                // Is the timelapse fragment currently active?
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMEWARP)) {
                    TimeWarpFragment timewarpFrag = (TimeWarpFragment) fragment;
                    timewarpFrag.onPulseStarted(exposures, totalExposures, (int) timeToNextExposure, timeRemaining);
                }
                break;
            case PhotoSniperApp.OnGoingAction.HDR:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.HDR)) {
                    HdrFragment hdrFrag = (HdrFragment) fragment;
                    hdrFrag.onPulseStarted(exposures, totalExposures, (int) timeToNextExposure, timeRemaining);
                }
                break;
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.HDR_LAPSE)) {
                    HdrTimeLapseFragment hdrFrag = (HdrTimeLapseFragment) fragment;
                    hdrFrag.onPulseStarted(exposures, totalExposures, (int) timeToNextExposure, timeRemaining);
                }
                break;
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.STARTRAIL)) {
                    StarTrailFragment startFrag = (StarTrailFragment) fragment;
                    startFrag.onPulseStarted(exposures, totalExposures, (int) timeToNextExposure, timeRemaining);
                }
                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.BRAMPING)) {
                    BrampingFragment brampFrag = (BrampingFragment) fragment;
                    brampFrag.onPulseStarted(exposures, totalExposures, (int) timeToNextExposure, timeRemaining);
                }

            default:
                // Do nothing we can't identify current fragment with ongoing
                // action.

        }


    }

    @Override
    public void onPulseUpdate(long[] sequence, int exposures, long timeToNext, long remainingPulseTime, long remainingSequenceTime) {

        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);

        //If we don't have a Fragment yet we don't need to update anything.
        if (fragment == null) {
            return;
        }

        switch (mService.getOnGoingAction()) {
            case PhotoSniperApp.OnGoingAction.TIMELAPSE:
                // Is the timelapse fragment currently active?
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMELAPSE)) {
                    TimeLapseOldFragment timeLapseFrag = (TimeLapseOldFragment) fragment;
                    timeLapseFrag.onPulseUpdate(exposures, timeToNext, remainingPulseTime);
                }
                break;
            case PhotoSniperApp.OnGoingAction.TIMEWARP:
                // Is the timelapse fragment currently active?
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMEWARP)) {
                    TimeWarpFragment timeWarpFrag = (TimeWarpFragment) fragment;
                    timeWarpFrag.onPulseUpdate(sequence, exposures, timeToNext, remainingPulseTime, remainingSequenceTime);
                }
                break;
            case PhotoSniperApp.OnGoingAction.HDR:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.HDR)) {
                    HdrFragment hdrFrag = (HdrFragment) fragment;
                    hdrFrag.onPulseUpdate(sequence, exposures, timeToNext, remainingPulseTime, remainingSequenceTime);
                }
                break;
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.HDR_LAPSE)) {
                    HdrTimeLapseFragment hdrFrag = (HdrTimeLapseFragment) fragment;
                    hdrFrag.onPulseUpdate(sequence, exposures, timeToNext, remainingPulseTime, remainingSequenceTime);
                }
                break;
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.STARTRAIL)) {
                    StarTrailFragment startFrag = (StarTrailFragment) fragment;
                    startFrag.onPulseUpdate(sequence, exposures, timeToNext, remainingPulseTime, remainingSequenceTime);
                }
                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.BRAMPING)) {
                    BrampingFragment brampFrag = (BrampingFragment) fragment;
                    brampFrag.onPulseUpdate(sequence, exposures, timeToNext, remainingPulseTime, remainingSequenceTime);
                }
                break;
            default:
                // Do nothing we can't identify current fragment with ongoing
                // action.
        }

    }

    public void onPulseFinished() {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();

        switch (mService.getOnGoingAction()) {
            case PhotoSniperApp.OnGoingAction.TIMELAPSE:
                Log.i(TAG, "&&TImelapse finish");
                break;
            case PhotoSniperApp.OnGoingAction.TIMEWARP:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.TIMEWARP)) {
                    Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
                    TimeWarpFragment timewarpFrag = (TimeWarpFragment) fragment;
                    if (timewarpFrag != null) {
                        timewarpFrag.onPulseStop();
                    }
                } else {
                    removeStatusBar();
                }
                break;
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.STARTRAIL)) {
                    Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
                    StarTrailFragment startFrag = (StarTrailFragment) fragment;
                    if (startFrag != null) {
                        startFrag.onPulseStop();
                    }
                } else {
                    removeStatusBar();
                }
                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.BRAMPING)) {
                    Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
                    BrampingFragment brampFrag = (BrampingFragment) fragment;
                    if (brampFrag != null) {
                        brampFrag.onPulseStop();
                    }
                } else {
                    removeStatusBar();
                }
                break;
            case PhotoSniperApp.OnGoingAction.HDR:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.HDR)) {
                    Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
                    HdrFragment startFrag = (HdrFragment) fragment;
                    if (startFrag != null) {
                        startFrag.onPulseStop();
                    }
                } else {
                    removeStatusBar();
                }
                break;
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.HDR_LAPSE)) {
                    Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
                    HdrTimeLapseFragment startFrag = (HdrTimeLapseFragment) fragment;
                    if (startFrag != null) {
                        startFrag.onPulseStop();
                    }
                } else {
                    removeStatusBar();
                }
                break;
            default:

        }
    }

    @Override
    public void onPulseSequenceIterate(long[] sequence) {
        String currentFragTag = mDrawerFragHandler.getCurrentFragmentTag();
        switch (mService.getOnGoingAction()) {
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                if (currentFragTag.equals(PhotoSniperApp.FragmentTags.HDR_LAPSE)) {
                    Fragment fragment = getFragmentManager().findFragmentByTag(currentFragTag);
                    HdrTimeLapseFragment startFrag = (HdrTimeLapseFragment) fragment;
                    if (startFrag != null) {
                        startFrag.onPulseSequenceIterated(sequence);
                    }
                }
        }
    }

    @Override
    public void onInputSelected(DialpadManager.DialPadInput dialPadInput) {
        Log.d(TAG, "onInputSelected");
        mDialPadManager.setActiveInput(dialPadInput);
        // trying to show down/forward key instead of back in the nav bar not
        // working..
        // getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION);
    }

    @Override
    public void onInputDeSelected() {
        mDialPadManager.deactiveInput();

    }


	/*
     * Listener for Wifi Master Fragment
	 */

    @Override
    public void inputSetSize(int height, int width) {
        mDialPadManager.setKeyboardDimensions(height, width);
    }


    // S O N Y           --------------> Connection  Stuff <--------------

    @Override
    public void onSonyWiFiConnected() {

        PhotoSniperApp.getInstance(getApplicationContext()).setSonyRPCAvailable(true);

    }

    @Override
    public void onSonyWiFiConnectionFailed(Throwable e) {

        PhotoSniperApp.getInstance(getApplicationContext()).setSonyRPCAvailable(false);
        PhotoSniperApp.getInstance(getApplicationContext()).getSonyWiFiRpc().unregisterInitCallback(this);
        PhotoSniperApp.getInstance(getApplicationContext()).setSonyWiFiRpc(null);
    }

    @Override
    public void onSuccess(BaseResponse response) {

    }

    @Override
    public void onFail(Throwable e) {

    }

    @Override
    public void onNextFrame(LiveViewFetcher.Frame frame) {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission accepted");
        } else {
            Toast.makeText(this, "You must grant the location permission.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    // BLE        --------------> Connection  Stuff <--------------

    @Override
    protected void onResume() {
        super.onResume();
        prepareForScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLeScan();

    }

    private void prepareForScan() {

        if (isBleSupported() && (PhotoSniperApp.getInstance(this).getBLEgattClient() == null)) {

            // Ensures Bluetooth is enabled on the device
            BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();

            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return;
            }

            if (btAdapter.isEnabled()) {

                startLeScan();

                // Prompt for runtime permission
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                        startLeScan();
//                    } else {
//                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
//                    }
//                }
            }
//            else {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            }
        } else {
            Toast.makeText(this, "BLE is not enabled/supported", Toast.LENGTH_LONG).show();
        }
    }


    private boolean isBleSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private void startLeScan() {

        mScanning = true;

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build();
        List<ScanFilter> filters = new ArrayList<>();
//        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(GattClient.SERVICE_UUID)).build());

        // get profile here
        String ourBLEDevice = "";
        try {
            SharedPreferences sharedPref = getSharedPreferences("BLE", Context.MODE_PRIVATE);
            ourBLEDevice = sharedPref.getString(getString(R.string.BLE_Device), "");

        } catch (Exception x) {
            Log.i(TAG, "no BLE Device stored ");
        }

        if ((ourBLEDevice != null) && (ourBLEDevice.length() > 1)) {
            filters.add(new ScanFilter.Builder().setDeviceAddress(ourBLEDevice).build());

            if (isBleSupported()) {
                mScanner.startScan(filters, settings, mScanCallback);
                // Stops scanning after a pre-defined scan period.
                mStopScanHandler.postDelayed(mStopScanRunnable, SCAN_TIMEOUT_MS);
            }

        } else {
//            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID_SERVICE)).build());

            Intent intent = new Intent(this, BLEDeviceScanActivity.class);
            startActivity(intent);

        }


//        invalidateOptionsMenu();
    }

    private void stopLeScan() {
        if (mScanning) {
            mScanning = false;

            try {
                mScanner.stopScan(mScanCallback);
                mStopScanHandler.removeCallbacks(mStopScanRunnable);
                Log.d(TAG, "BLE Scanning stopped");
            } catch (NullPointerException exception) {
                Log.e(TAG, "Can't stop BLE scan. Unexpected NullPointerException", exception);
            }


//            invalidateOptionsMenu();
        }
    }

    private void connectBLE(BluetoothDevice device) {

        final GattClient mGattClient = new GattClient();

        mGattClient.onCreate(this, device.getAddress(), new GattClient.OnCharacteristicReadListener() {

            //            @Override
            public void onBLECharacteristicRead(final String value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Characteristic read: " + value);

//                        Toast.makeText(MainActivity.this, value, Toast.LENGTH_LONG).show();

                    }
                });
            }


            @Override
            public void onBLEConnected(final boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!success) {
                            Toast.makeText(MainActivity.this, R.string.BLEConnectionError, Toast.LENGTH_LONG).show();
                            if (mGattClient != null) {
                                mGattClient.onDestroy();
                            }
                            PhotoSniperApp.getInstance(MainActivity.this).setBLEgattClient(null);
                            // start to listen again ?
                        } else {
                            PhotoSniperApp.getInstance(MainActivity.this).setBLEgattClient(mGattClient);
                        }

                    }
                });
            }
        });

//        PhotoSniperApp.getInstance(this).setBLEgattClient(mGattClient);

//        Intent intent = new Intent(this, InteractActivity.class);
//        intent.putExtra(InteractActivity.EXTRA_DEVICE_ADDRESS, device.getAddress());
//        startActivity(intent);
//        finish();
    }

// BLE --- stop


    private interface DrawerGroups {
        int WELCOME = 0;
        int SIMPLE_MODES = 1;
        int TIME_MODE = 2;
        int SENSOR_MODES = 3;
        int HDR_MODES = 4;
        //        public static int REMOTE_MODES = 5;
//        public static int SETTINGS = 7;
        int CALCULATORS = 5;
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ExpandableListView.OnChildClickListener {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
            parent.setItemChecked(index, true);
            selectItem(groupPosition, childPosition, index);
            return true;
        }
    }

    private class DrawerExpandableListAdapter extends BaseExpandableListAdapter {

        final LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);

        public Object getChild(int groupPosition, int childPosition) {
            return mModes.get(groupPosition)[childPosition];
        }

        String getChildSubText(int groupPosition, int childPosition) {
            return mModesSubText.get(groupPosition)[childPosition];
        }

        int getIcon(int groupPosition, int childPosition) {
            int icon;
            if (groupPosition > (mModeIcons.size() - 1)) {
                icon = R.drawable.icon_happy;
            } else {
                icon = mModeIcons.get(groupPosition).getResourceId(childPosition, -1);
            }
            return icon;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return mModes.get(groupPosition).length;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {

            TextView textView = null;
            TextView subText = null;
            ImageView icon = null;
            if (view == null) {
                view = inflater.inflate(R.layout.drawer_list_item, null);
                textView = (TextView) view.findViewById(R.id.drawListItemTextTitle);
                subText = (TextView) view.findViewById(R.id.drawListItemTextSubtitle);
                textView.setTypeface(PhotoSniperApp.getInstance(getApplicationContext()).SAN_SERIF_LIGHT);
                subText.setTypeface(PhotoSniperApp.getInstance(getApplicationContext()).SAN_SERIF_LIGHT);

                icon = (ImageView) view.findViewById(R.id.drawlist_item_icon);
            }
            if (textView == null) {
                textView = (TextView) view.findViewById(R.id.drawListItemTextTitle);
                subText = (TextView) view.findViewById(R.id.drawListItemTextSubtitle);
                icon = (ImageView) view.findViewById(R.id.drawlist_item_icon);
            }

            textView.setText(getChild(groupPosition, childPosition).toString());
            subText.setText(getChildSubText(groupPosition, childPosition));
            icon.setImageResource(getIcon(groupPosition, childPosition));

            ImageView itemImage = (ImageView) view.findViewById(R.id.drawlist_item_image);

            switch (childPosition) {
                case 0:
                    itemImage.setColorFilter(0x00000000);
                    break;
                case 1:
                    itemImage.setColorFilter(0x19000000);
                    break;
                case 2:
                    itemImage.setColorFilter(0x33000000);
                    break;
                case 3:
                    itemImage.setColorFilter(0x4c000000);
                    break;
                case 4:
                    itemImage.setColorFilter(0x64000000);
                    break;
                case 5:
                    itemImage.setColorFilter(0x7d000000);
                    break;

            }
            return view;
        }

        public Object getGroup(int groupPosition) {
            return mModesGroups[groupPosition];
        }

        public int getGroupCount() {
            return mModesGroups.length;
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent) {

            if (view == null) {
                view = inflater.inflate(R.layout.drawer_list_header_item, null);
            }
            TextView textView = (TextView) view.findViewById(R.id.drawlist_header_item_text);

            textView.setText(getGroup(groupPosition).toString());
            return view;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

    }


}