package at.photosniper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import at.photosniper.fragments.preference.SettingsFragment;
import at.photosniper.util.GattClient;
import at.photosniper.util.SonyWiFiRPC;

public class PhotoSniperApp {

    private static final int TIMELASPE_INTERVAL_DEFAULT = 1000;
    private static final int TIME_MODE_TIME_DEFAULT = 30000;
    private static final int SELF_TIME_MODE_TIME_DEFAULT = 30000;
    private static final int SOUND_SENSOR_THRESHOLD_DEFAULT = 50;
    private static final int SOUND_SENSOR_SENSITIVTY_DEFAULT = 50;
    private static final int STAR_TRAIL_ITERATIONS_DEFAULT = 10;
    private static final long STAR_TRAIL_EXPOSURE_DEFAULT = 90000;
    private static final long STAR_TRAIL_GAP_DEFAULT = 5000;
    private static final long LE_HDR_MIDDLE_EXPOSURE_DEFAULT = 2000;
    private static final int LE_HDR_NUM_EXPOSURES_DEFAULT = 3;
    private static final float LE_HDR_EV_STEP_DEFAULT = 0.5f;
    private static final long LE_HDR_TIMELPASE_MIDDLE_EXPOSURE_DEFAULT = 2000;
    private static final long LE_HDR_TIMELPASE_INTERVAL_DEFAULT = 10000;
    private static final long BRAMPING_START_EXPOSURE_DEFAULT = 2000;
    private static final long BRAMPING_END_EXPOSURE_DEFAULT = 8000;
    public static final int DISTANCELAPSE_DISTANCE_DEFAULT = 25;
    private static final int TIMEWARP_INTERATIONS_DEAFAULT = 100;
    private static final long TIMEWARP_DURATION_DEFAULT = 3600000;
    private static final float TIMEWARP_CONTROL1_X_DEFAULT = 0.5f;
    private static final float TIMEWARP_CONTROL1_Y_DEFAULT = 0.0f;
    private static final float TIMEWARP_CONTROL2_X_DEFAULT = 0.5f;
    private static final float TIMEWARP_CONTROL2_Y_DEFAULT = 1.0f;
    private static final String WIFI_SLAVE_LAST_MASTER_DEFAULT = "";
    private static final Boolean WIFI_MASTER_IS_ON_DEFAULT = false;
    private static final Boolean SHOW_DIALOG_AGAIN_DEFAULT = true;
    private static final int LAUNCH_COUNT_DEFAULT = 0;
    private static final long FIRST_LAUNCH_DEFAULT = 0;
    private static final int ND_FILTER_SHUTTER_SPEED_DEFAULT = 0;
    // Shared preference keys
    private static final String TT_PREFS = "triggertrap_prefs";
    private static final String TT_MODE = "tt_mode";
    private static final String TT_IS_FIRST_LAUNCH = "tt_is_first_launch";
    private static final String TT_LAST_FRAGMENT = "tt_last_fragment";
    private static final String TT_LAST_ACTION_BAR_LABEL = "tt_last_action_bar_label";
    private static final String TT_LAST_LIST_ITEM_CHECKED = "tt_last_list_item_checked";
    private static final String TT_TIMELASPE_INTERVAL = "tt_timelaspe_interval";
    private static final String TT_TIME_MODE_TIME = "tt_time_mode_time";
    private static final String TT_SELF_TIME_MODE_TIME = "tt_self_time_mode_time";
    private static final String TT_SOUND_SENSOR_THRESHOLD = "tt_sound_sensor_threshold";
    private static final String TT_SOUND_SENSOR_SENSITIVTY = "tt_sound_sensor_sensitivty";
    private static final String TT_STAR_TRAIL_ITERATIONS = "tt_star_trail_interations";
    private static final String TT_STAR_TRAIL_EXPOSURE = "tt_star_trail_exposure";
    private static final String TT_STAR_TRAIL_GAP = "tt_star_trail_gap";
    private static final String TT_HDR_MIDDLE_EXP = "tt_hdr_middle_exp";
    private static final String TT_HDR_NUM_EXP = "tt_hdr_num_exp";
    private static final String TT_HDR_EV_STEP = "tt_hdr_ev_step";
    private static final String TT_HDR_TIMELPASE_MIDDLE_EXP = "tt_hdr_timelapse_middle_exp";
    private static final String TT_HDR_TIMELPASE_INTERVAL = "tt_hdr_timelapse_interval";
    private static final String TT_HDR_TIMELPASE_EV_STEP = "tt_hdr_timelapse_ev_step";
    private static final String TT_BRAMPING_ITERATIONS = "tt_bramping_iterations";
    private static final String TT_BRAMPING_INTERVAL = "tt_bramping_interval";
    private static final String TT_BRAMPING_START_EXP = "tt_bramping_start_exp";
    private static final String TT_BRAMPING_END_EXP = "tt_bramping_end_exp";
    private static final String TT_DISTANCELAPSE_DISTANCE = "tt_distancelapse_distance";
    private static final String TT_TIMEWARP_ITERATIONS = "tt_timewarp_iterations";
    private static final String TT_TIMEWARP_DURATION = "tt_timewarp_duration";
    private static final String TT_TIMEWARP_CONTROL1_X = "tt_timewarp_control1_x";
    private static final String TT_TIMEWARP_CONTROL1_Y = "tt_timewarp_control1_y";
    private static final String TT_TIMEWARP_CONTROL2_X = "tt_timewarp_control2_x";
    private static final String TT_TIMEWARP_CONTROL2_Y = "tt_timewarp_control2_y";
    private static final String TT_WIFI_SLAVE_LAST_MASTER = "tt_wifi_slave_last_master";
    private static final String TT_WIFI_MASTER_IS_ON = "tt_wifi_master_is_on";
    private static final String TT_SHOW_DIALOG_AGAIN = "tt_show_dialog_again";
    private static final String TT_LAUNCH_COUNT = "tt_launch_count";
    private static final String TT_FIRST_LAUNCH_DATE = "tt_first_launch_date";
    private static final String TT_DEFAULT_SHUTTER_SPEED = "tt_default_shutter_speed";
    private static final String LAST_FRAGMENT_DEFAULT = FragmentTags.GETTING_STARTED;
    private static final int LAST_LIST_ITEM_CHECKED_DEFAULT = 1;
    private static final long CAMERA_BEEP_LENGTH_DEFAULT = 150;
    private static final float LE_HDR_TIMELPASE_EV_STEP_DEFAULT = 0.5f;
    private static final int BRAMPING_INTERATION_DEFAULT = 360;
    private static final long BRAMPING_INTERVAL_DEFAULT = 10000;
    private static final int UNINITIALIZED = -1;
    private static final String UNINITIALIZED_STRING = null;
    private static final Boolean UNINITIALIZED_BOOL = null;
    // Gap between beeps in millisconds
    private static final long BEEP_GAP = 750;
    private static final long HDR_GAP = 1000;
    private static PhotoSniperApp mInstance;
    private final String LAST_ACTION_BAR_LABEL_DEFAULT;
    // Typefaces
    public Typeface SAN_SERIF_LIGHT = null;
    private Typeface SAN_SERIF_THIN = null;
    private final Context mAppContext;
    private String mLastFragmentTag = UNINITIALIZED_STRING;
    private String mLastActionBarLabel = UNINITIALIZED_STRING;
    private int mLastListItemChecked = UNINITIALIZED;
    private long mBeepLength = UNINITIALIZED;
    // Time lapse interval in milliseconds.
    private long mTimeLapseInterval = UNINITIALIZED;
    // Timed mode time in milliseconds
    private long mTimedModeTime = UNINITIALIZED;
    //Self Timer time in milliseconds
    private long mSelfTimedModeTime = UNINITIALIZED;
    // Sound sensor values
    private int mSensorResetDelay = UNINITIALIZED;
    private int mSensorDelay = UNINITIALIZED;
    private int mSoundSensorThreshold = UNINITIALIZED;
    private int mSoundSensorSensitivity = UNINITIALIZED;
    // Star trial values
    private int mStarTrailInterations = UNINITIALIZED;
    private long mStarTrailExposure = UNINITIALIZED;
    private long mStarTrailGap = UNINITIALIZED;
    // HDR values
    private long mHdrMiddleExposure = UNINITIALIZED;
    private int mHdrNumberExposures = UNINITIALIZED;
    private float mHdrEvStep = UNINITIALIZED;
    // HDR timelapse values
    private long mHdrTimeLapseMiddleExposure = UNINITIALIZED;
    private long mHdrTimeLapseInterval = UNINITIALIZED;
    private float mHdrTimeLapseEvStep = UNINITIALIZED;
    // Bramping values
    private int mBrampingIterations = UNINITIALIZED;
    private long mBrampingInterval = UNINITIALIZED;
    private long mBrampingStartExposure = UNINITIALIZED;
    private long mBrampingEndExposure = UNINITIALIZED;
    // Distancelapse values
    private int mDistanceLapaseDistance = UNINITIALIZED;
    private int mDistanceLapseUnit = UNINITIALIZED;
    private int mDistanceLapseSpeedUnit = UNINITIALIZED;
    // Timewarp values
    private int mTimeWarpInterations = UNINITIALIZED;
    private long mTimeWarpDuration = UNINITIALIZED;
    private float mTimewarpControl1X = UNINITIALIZED;
    private float mTimewarpControl1Y = UNINITIALIZED;
    private float mTimewarpControl2X = UNINITIALIZED;
    private float mTimewarpControl2Y = UNINITIALIZED;
    // App Rating Values
    private Boolean mShowDialogAgain = UNINITIALIZED_BOOL;
    private int mLaunchCount = UNINITIALIZED;
    private long mDateFirstLaunched = UNINITIALIZED;
    // Wifi slave values
    private String mLastConnectedMaster = UNINITIALIZED_STRING;
    // Wif Master values
    private Boolean mIsMasterON = UNINITIALIZED_BOOL;
    // ND Filter Values
    private int mDefaultShutterSpeed = UNINITIALIZED;

    // SONY WLAN Comm
    private SonyWiFiRPC sonyWiFiRpc;
    private boolean sonyRPCAvailable = false;

    //PhotoSniper Box COntrol BLE
    private GattClient BLEgattClient;


    private PhotoSniperApp(Context ctx) {
        mAppContext = ctx.getApplicationContext();
        LAST_ACTION_BAR_LABEL_DEFAULT = mAppContext.getResources().getString(R.string.getting_started);
    }

    public static PhotoSniperApp getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PhotoSniperApp(context);
            mInstance.init();
        }
        return mInstance;
    }

    // SONY + BLE ---------------------------
    public boolean isSonyRPCAvailable() {
        return sonyRPCAvailable;
    }

    public void setSonyRPCAvailable(boolean sonyRPCAvailable) {
        this.sonyRPCAvailable = sonyRPCAvailable;
    }

    public SonyWiFiRPC getSonyWiFiRpc() {
        return sonyWiFiRpc;
    }

    public void setSonyWiFiRpc(SonyWiFiRPC sonyWiFiRpc) {
        this.sonyWiFiRpc = sonyWiFiRpc;
    }

    public GattClient getBLEgattClient() {
        return BLEgattClient;
    }

    public void setBLEgattClient(GattClient BLEgattClient) {
        this.BLEgattClient = BLEgattClient;
    }

// SONY + BLE ---------------------------


    private void init() {
        // Load values store in the shares prefs
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);

        SAN_SERIF_LIGHT = Typeface.createFromAsset(mAppContext.getAssets(), "fonts/Roboto-Light.ttf");
        SAN_SERIF_THIN = Typeface.createFromAsset(mAppContext.getAssets(), "fonts/Roboto-Thin.ttf");
    }

    public boolean isFirstStarted() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(TT_IS_FIRST_LAUNCH, true);
        // Subsequent calls should always return false;
        Editor editor = prefs.edit();
        editor.putBoolean(TT_IS_FIRST_LAUNCH, false);
        editor.apply();
        return isFirstLaunch;
    }

    public String getLastFragmentTag() {
        if (UNINITIALIZED_STRING == mLastFragmentTag) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mLastFragmentTag = prefs.getString(TT_LAST_FRAGMENT, LAST_FRAGMENT_DEFAULT);
        }
        return mLastFragmentTag;
    }

    public void setLastFragmentTag(String fragmentTag) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(TT_LAST_FRAGMENT, fragmentTag);
        editor.apply();
        mLastFragmentTag = fragmentTag;
    }

    public String getLastActionBarLabel() {
        if (UNINITIALIZED_STRING == mLastActionBarLabel) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mLastActionBarLabel = prefs.getString(TT_LAST_ACTION_BAR_LABEL, LAST_ACTION_BAR_LABEL_DEFAULT);
        }
        return mLastActionBarLabel;
    }

    public void setLastActionBarLabel(String label) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(TT_LAST_ACTION_BAR_LABEL, label);
        editor.apply();
        mLastActionBarLabel = label;
    }

    public int getLastListItemChecked() {
        if (mLastListItemChecked == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mLastListItemChecked = prefs.getInt(TT_LAST_LIST_ITEM_CHECKED, LAST_LIST_ITEM_CHECKED_DEFAULT);
        }
        return mLastListItemChecked;
    }

    public void setLastListItemChecked(int listItemIndex) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_LAST_LIST_ITEM_CHECKED, listItemIndex);
        editor.apply();
        mLastListItemChecked = listItemIndex;
    }

    public Boolean getShowAgain() {
        if (mShowDialogAgain == UNINITIALIZED_BOOL) {
            SharedPreferences sharedPref = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mShowDialogAgain = sharedPref.getBoolean(TT_SHOW_DIALOG_AGAIN, SHOW_DIALOG_AGAIN_DEFAULT);
        }
        return mShowDialogAgain;
    }

    public void setShowDialogAgain() {
        mShowDialogAgain = false;

        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putBoolean(TT_SHOW_DIALOG_AGAIN, false);
        editor.apply();
    }

    public int getLaunchCount() {
        if (mLaunchCount == UNINITIALIZED) {
            SharedPreferences sharedPref = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mLaunchCount = sharedPref.getInt(TT_LAUNCH_COUNT, LAUNCH_COUNT_DEFAULT);
        }
        return mLaunchCount;
    }

    public void setLaunchCount(int count) {
        mLaunchCount = count;
        SharedPreferences sharedPref = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = sharedPref.edit();
        editor.putInt(TT_LAUNCH_COUNT, count);
        editor.apply();
    }

    public long getFirstLaunchDate() {
        if (mDateFirstLaunched == UNINITIALIZED) {
            SharedPreferences sharedPref = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mDateFirstLaunched = sharedPref.getLong(TT_FIRST_LAUNCH_DATE, FIRST_LAUNCH_DEFAULT);
        }
        return mDateFirstLaunched;
    }

    public void setFirstLaunchDate(long date) {
        mDateFirstLaunched = date;
        SharedPreferences sharedPref = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = sharedPref.edit();
        editor.putLong(TT_FIRST_LAUNCH_DATE, date);
        editor.apply();
    }

    public long getBeepLength() {
        // Get the Beep Length from the DEFAULT share prefs made with the
        // preferences.xml
        if (mBeepLength == UNINITIALIZED) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
            String pulseLengthStr = sharedPref.getString(SettingsFragment.PULSE_LENGTH_SETTING, "");
            mBeepLength = Long.parseLong(pulseLengthStr);
        }
        return mBeepLength;
    }

    public void setBeepLength(long beepLength) {
        mBeepLength = beepLength;
    }

    public long getHDRGapLength() {
        return HDR_GAP;
    }

    public long getTimeLapseInterval() {
        if (mTimeLapseInterval == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimeLapseInterval = prefs.getLong(TT_TIMELASPE_INTERVAL, TIMELASPE_INTERVAL_DEFAULT);
        }
        return mTimeLapseInterval;
    }

    public void setTimeLapseInterval(long timeLapseInterval) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_TIMELASPE_INTERVAL, timeLapseInterval);
        editor.apply();
        mTimeLapseInterval = timeLapseInterval;
    }

    public long getSelfTimedModeTime() {
        if (mSelfTimedModeTime == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mSelfTimedModeTime = prefs.getLong(TT_SELF_TIME_MODE_TIME, SELF_TIME_MODE_TIME_DEFAULT);
        }
        return mSelfTimedModeTime;
    }

    public void setSelfTimedModeTime(long time) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_SELF_TIME_MODE_TIME, time);
        editor.apply();
        mSelfTimedModeTime = time;
    }

    public long getTimedModeTime() {
        if (mTimedModeTime == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimedModeTime = prefs.getLong(TT_TIME_MODE_TIME, TIME_MODE_TIME_DEFAULT);
        }
        return mTimedModeTime;
    }

    public void setTimedModeTime(long time) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_TIME_MODE_TIME, time);
        editor.apply();
        mTimedModeTime = time;
    }

    public int getSensorResetDelay() {
        // Get the Sensor Reset from the DEFAULT share prefs made with the
        // preferences.xml
        if (mSensorResetDelay == UNINITIALIZED) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
            String resetDelay = sharedPref.getString(SettingsFragment.SENSOR_RESET_DELAY_SETTING, "");
            mSensorResetDelay = Integer.parseInt(resetDelay);
        }
        return mSensorResetDelay;
    }

    public void setSensorResetDelay(int sensorResetDelay) {
        mSensorResetDelay = sensorResetDelay;
    }

    public int getSensorDelay() {
        // Get the Sensor Reset from the DEFAULT share prefs made with the
        // preferences.xml
        if (mSensorDelay == UNINITIALIZED) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
            String resetDelay = sharedPref.getString(SettingsFragment.SENSOR_DELAY_SETTING, "");
            mSensorDelay = Integer.parseInt(resetDelay);
        }
        return mSensorDelay;
    }

    public void setSensorDelay(int sensorDelay) {
        mSensorDelay = sensorDelay;
    }

    public int getSoundSensorThreshold() {
        if (mSoundSensorThreshold == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mSoundSensorThreshold = prefs.getInt(TT_SOUND_SENSOR_THRESHOLD, SOUND_SENSOR_THRESHOLD_DEFAULT);
        }
        return mSoundSensorThreshold;
    }

    public void setSoundSensorThreshold(int amplitude) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_SOUND_SENSOR_THRESHOLD, amplitude);
        editor.apply();
        mSoundSensorThreshold = amplitude;
    }

    public int getSoundSensorSensitivity() {
        if (mSoundSensorSensitivity == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mSoundSensorSensitivity = prefs.getInt(TT_SOUND_SENSOR_SENSITIVTY, SOUND_SENSOR_SENSITIVTY_DEFAULT);
        }
        return mSoundSensorSensitivity;
    }

    public void setSoundSensorSensitivity(int amplitude) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_SOUND_SENSOR_SENSITIVTY, amplitude);
        editor.apply();
        mSoundSensorSensitivity = amplitude;
    }

    public int getStarTrailIterations() {
        if (mStarTrailInterations == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mStarTrailInterations = prefs.getInt(TT_STAR_TRAIL_ITERATIONS, STAR_TRAIL_ITERATIONS_DEFAULT);
        }
        return mStarTrailInterations;
    }

    public void setStarTrailIterations(int iterations) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_STAR_TRAIL_ITERATIONS, iterations);
        editor.apply();
        mStarTrailInterations = iterations;
    }

    public long getStarTrailExposure() {
        if (mStarTrailExposure == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mStarTrailExposure = prefs.getLong(TT_STAR_TRAIL_EXPOSURE, STAR_TRAIL_EXPOSURE_DEFAULT);
        }
        return mStarTrailExposure;
    }

    public void setStarTrailExposure(long exposure) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_STAR_TRAIL_EXPOSURE, exposure);
        editor.apply();
        mStarTrailExposure = exposure;
    }

    public long getStarTrailGap() {
        if (mStarTrailGap == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mStarTrailGap = prefs.getLong(TT_STAR_TRAIL_GAP, STAR_TRAIL_GAP_DEFAULT);
        }
        return mStarTrailGap;
    }

    public void setStarTrailGap(long gap) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_STAR_TRAIL_GAP, gap);
        editor.apply();
        mStarTrailGap = gap;
    }

    public long getHDRMiddleExposure() {
        if (mHdrMiddleExposure == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mHdrMiddleExposure = prefs.getLong(TT_HDR_MIDDLE_EXP, LE_HDR_MIDDLE_EXPOSURE_DEFAULT);
        }
        return mHdrMiddleExposure;
    }

    public void setHDRMiddleExposure(long hdrMiddleExposure) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_HDR_MIDDLE_EXP, hdrMiddleExposure);
        editor.apply();
        mHdrMiddleExposure = hdrMiddleExposure;
    }

    public int getHDRNumExposures() {
        if (mHdrNumberExposures == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mHdrNumberExposures = prefs.getInt(TT_HDR_NUM_EXP, LE_HDR_NUM_EXPOSURES_DEFAULT);
        }
        return mHdrNumberExposures;
    }

    public void setHDRNumExposures(int hdrNumExposures) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_HDR_NUM_EXP, hdrNumExposures);
        editor.apply();
        mHdrNumberExposures = hdrNumExposures;
    }

    public float getHDREvStep() {
        if (mHdrEvStep == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mHdrEvStep = prefs.getFloat(TT_HDR_EV_STEP, LE_HDR_EV_STEP_DEFAULT);
        }
        return mHdrEvStep;
    }

    public void setHDREvStep(float evStep) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putFloat(TT_HDR_EV_STEP, evStep);
        editor.apply();
        mHdrEvStep = evStep;
    }

    public long getHDRTimeLapseMiddleExposure() {
        if (mHdrTimeLapseMiddleExposure == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mHdrTimeLapseMiddleExposure = prefs.getLong(TT_HDR_TIMELPASE_MIDDLE_EXP, LE_HDR_TIMELPASE_MIDDLE_EXPOSURE_DEFAULT);
        }
        return mHdrTimeLapseMiddleExposure;
    }

    public void setHDRTimeLapseMiddleExposure(long hdrTimeLapseMiddleExposure) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_HDR_TIMELPASE_MIDDLE_EXP, hdrTimeLapseMiddleExposure);
        editor.apply();
        mHdrTimeLapseMiddleExposure = hdrTimeLapseMiddleExposure;
    }

    public long getHDRTimeLapseInterval() {
        if (mHdrTimeLapseInterval == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mHdrTimeLapseInterval = prefs.getLong(TT_HDR_TIMELPASE_INTERVAL, LE_HDR_TIMELPASE_INTERVAL_DEFAULT);
        }
        return mHdrTimeLapseInterval;
    }

    public void setHDRTimeLapseInterval(long duration) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_HDR_TIMELPASE_INTERVAL, duration);
        editor.apply();
        mHdrTimeLapseInterval = duration;
    }

    public float getHDRTimeLapseEvStep() {
        if (mHdrTimeLapseEvStep == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mHdrTimeLapseEvStep = prefs.getFloat(TT_HDR_TIMELPASE_EV_STEP, LE_HDR_TIMELPASE_EV_STEP_DEFAULT);
        }
        return mHdrTimeLapseEvStep;
    }

    public void setHDRTimeLapseEvStep(float evStep) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putFloat(TT_HDR_TIMELPASE_EV_STEP, evStep);
        editor.apply();
        mHdrTimeLapseEvStep = evStep;
    }

    public int getBrampingIterations() {
        if (mBrampingIterations == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mBrampingIterations = prefs.getInt(TT_BRAMPING_ITERATIONS, BRAMPING_INTERATION_DEFAULT);
        }
        return mBrampingIterations;
    }

    public void setBrampingIterations(int iterations) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_BRAMPING_ITERATIONS, iterations);
        editor.apply();
        mBrampingIterations = iterations;
    }

    public long getBrampingInterval() {
        if (mBrampingInterval == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mBrampingInterval = prefs.getLong(TT_BRAMPING_INTERVAL, BRAMPING_INTERVAL_DEFAULT);
        }
        return mBrampingInterval;
    }

    public void setBrampingInterval(long duration) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_BRAMPING_INTERVAL, duration);
        editor.apply();
        mBrampingInterval = duration;
    }

    public long getBrampingStartExposure() {
        if (mBrampingStartExposure == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mBrampingStartExposure = prefs.getLong(TT_BRAMPING_START_EXP, BRAMPING_START_EXPOSURE_DEFAULT);
        }
        return mBrampingStartExposure;
    }

    public void setBrampingStartExposure(long brampingStartExposure) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_BRAMPING_START_EXP, brampingStartExposure);
        editor.apply();
        mBrampingStartExposure = brampingStartExposure;
    }

    public long getBrampingEndExposure() {
        if (mBrampingEndExposure == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mBrampingEndExposure = prefs.getLong(TT_BRAMPING_END_EXP, BRAMPING_END_EXPOSURE_DEFAULT);
        }
        return mBrampingEndExposure;
    }

    public void setBrampingEndExposure(long brampingEndExposure) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_BRAMPING_END_EXP, brampingEndExposure);
        editor.apply();
        mBrampingEndExposure = brampingEndExposure;
    }

    public int getDistanceLapseDistance() {
        if (mDistanceLapaseDistance == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mDistanceLapaseDistance = prefs.getInt(TT_DISTANCELAPSE_DISTANCE, DISTANCELAPSE_DISTANCE_DEFAULT);
        }
        return mDistanceLapaseDistance;
    }

    public void setDistanceLapseDistance(int distance) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_DISTANCELAPSE_DISTANCE, distance);
        editor.apply();
        mDistanceLapaseDistance = distance;
    }

    public int getDistlapseUnit() {
        if (mDistanceLapseUnit == UNINITIALIZED) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
            String resetDelay = sharedPref.getString(SettingsFragment.DISTANCE_UNIT_SETTING, "");
            mDistanceLapseUnit = Integer.parseInt(resetDelay);
        }
        return mDistanceLapseUnit;
    }

    public void setDistancLapseUnit(int distanceUnit) {
        mDistanceLapseUnit = distanceUnit;
    }

    public int getDefaultShutterSpeedVal() {
        if (mDefaultShutterSpeed == UNINITIALIZED) {
            SharedPreferences sharedPref = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mDefaultShutterSpeed = sharedPref.getInt(TT_DEFAULT_SHUTTER_SPEED, ND_FILTER_SHUTTER_SPEED_DEFAULT);
        }

        return mDefaultShutterSpeed;
    }

    public void setDefaultShutterSpeedVal(int shutterSpeedLoc) {
        mDefaultShutterSpeed = shutterSpeedLoc;
        SharedPreferences sharedPref = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = sharedPref.edit();
        editor.putInt(TT_DEFAULT_SHUTTER_SPEED, shutterSpeedLoc);
        editor.apply();
    }

    public int getDistlapseSpeedUnit() {
        if (mDistanceLapseSpeedUnit == UNINITIALIZED) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
            String resetDelay = sharedPref.getString(SettingsFragment.DISTANCE_SPEED_SETTING, "");
            mDistanceLapseSpeedUnit = Integer.parseInt(resetDelay);
        }
        return mDistanceLapseSpeedUnit;
    }

    public void setDistancLapseSpeedUnit(int distanceSpeedUnit) {
        mDistanceLapseSpeedUnit = distanceSpeedUnit;
    }

    public int getTimeWarpIterations() {
        if (mTimeWarpInterations == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimeWarpInterations = prefs.getInt(TT_TIMEWARP_ITERATIONS, TIMEWARP_INTERATIONS_DEAFAULT);
        }
        return mTimeWarpInterations;
    }

    public void setTimeWarpIterations(int iterations) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(TT_TIMEWARP_ITERATIONS, iterations);
        editor.apply();
        mTimeWarpInterations = iterations;
    }

    public long getTimewarpDuration() {
        if (mTimeWarpDuration == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimeWarpDuration = prefs.getLong(TT_TIMEWARP_DURATION, TIMEWARP_DURATION_DEFAULT);
        }
        return mTimeWarpDuration;
    }

    public void setTimewarpDuration(long exposure) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(TT_TIMEWARP_DURATION, exposure);
        editor.apply();
        mTimeWarpDuration = exposure;
    }

    public float getTimewarpControl1X() {
        if (mTimewarpControl1X == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimewarpControl1X = prefs.getFloat(TT_TIMEWARP_CONTROL1_X, TIMEWARP_CONTROL1_X_DEFAULT);
        }
        return mTimewarpControl1X;
    }

    public void setTimewarpControl1X(float controlCoord) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putFloat(TT_TIMEWARP_CONTROL1_X, controlCoord);
        editor.apply();
        mTimewarpControl1X = controlCoord;
    }

    public float getTimewarpControl1Y() {
        if (mTimewarpControl1Y == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimewarpControl1Y = prefs.getFloat(TT_TIMEWARP_CONTROL1_Y, TIMEWARP_CONTROL1_Y_DEFAULT);
        }
        return mTimewarpControl1Y;
    }

    public void setTimewarpControl1Y(float controlCoord) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putFloat(TT_TIMEWARP_CONTROL1_Y, controlCoord);
        editor.apply();
        mTimewarpControl1Y = controlCoord;
    }

    public float getTimewarpControl2X() {
        if (mTimewarpControl2X == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimewarpControl2X = prefs.getFloat(TT_TIMEWARP_CONTROL2_X, TIMEWARP_CONTROL2_X_DEFAULT);
        }
        return mTimewarpControl2X;
    }

    public void setTimewarpControl2X(float controlCoord) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putFloat(TT_TIMEWARP_CONTROL2_X, controlCoord);
        editor.apply();
        mTimewarpControl2X = controlCoord;
    }

    public float getTimewarpControl2Y() {
        if (mTimewarpControl2Y == UNINITIALIZED) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mTimewarpControl2Y = prefs.getFloat(TT_TIMEWARP_CONTROL2_Y, TIMEWARP_CONTROL2_Y_DEFAULT);
        }
        return mTimewarpControl2Y;
    }

    public void setTimewarpControl2Y(float controlCoord) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putFloat(TT_TIMEWARP_CONTROL2_Y, controlCoord);
        editor.apply();
        mTimewarpControl2Y = controlCoord;
    }

    public String getSlaveLastMaster() {
        if (UNINITIALIZED_STRING == mLastConnectedMaster) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mLastConnectedMaster = prefs.getString(TT_WIFI_SLAVE_LAST_MASTER, WIFI_SLAVE_LAST_MASTER_DEFAULT);
        }
        return mLastConnectedMaster;
    }

    public void setSlaveLastMaster(String lastMaster) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(TT_WIFI_SLAVE_LAST_MASTER, lastMaster);
        editor.apply();
        mLastConnectedMaster = lastMaster;
    }

    public boolean isMasterOn() {
        if (mIsMasterON == UNINITIALIZED_BOOL) {
            SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
            mIsMasterON = prefs.getBoolean(TT_WIFI_MASTER_IS_ON, WIFI_MASTER_IS_ON_DEFAULT);
        }
        return mIsMasterON;
    }

    public void setMasterOn(boolean state) {
        SharedPreferences prefs = mAppContext.getSharedPreferences(TT_PREFS, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putBoolean(TT_WIFI_MASTER_IS_ON, state);
        editor.apply();
        mIsMasterON = state;
    }

    public interface FragmentTags {
        String NONE = "none";
        String GETTING_STARTED = "getting_started";
        String BUY_DONGLE = "buy_dongle";
        String SIMPLE = "simple";
        String QUICK_RELEASE = "quick_release";
        String PRESS_AND_HOLD = "press_and_hold";
        String PRESS_TO_START = "press_to_start";
        String TIMED = "timed";
        String SELF_TIMER = "self_timer";
        String TIMELAPSE = "timelapse";
        String TIMEWARP = "timewarp";
        String STARTRAIL = "startrail";
        String BRAMPING = "bramping";
        String BANG = "bang";
        String DISTANCE_LAPSE = "distance_lapse";
        String HDR = "hdr";
        String HDR_LAPSE = "hdr_lapse";
        String WIFI_SLAVE = "wifi_slave";
        String WIFI_MASTER = "wifi_master";
        String PEBBLE = "pebble";
        String PLACEHOLDER = "placeholder";
        String SUNRISESUNSET = "sunrise_sunset";
        String ND_CALCULATOR = "nd_calculator";
    }

    public interface OnGoingAction {
        int INVALID = -2;
        int NONE = -1;
        int PRESS_START_STOP = 0;
        int TIMED = 1;
        int TIMELAPSE = 2;
        int TIMEWARP = 3;
        int STAR_TRAIL = 4;
        int BRAMPING = 5;
        int BANG = 6;
        int DISTANCE_LAPSE = 7;
        int HDR = 8;
        int HDR_TIMELAPSE = 9;
        int WI_FI_SLAVE = 10;
        int WI_FI_MASTER = 11;
        int PRESS_AND_HOLD = 12;
        int PEBBLE = 13;
        int SELF_TIMER = 14;
        int QUICK_RELEASE = 15;
    }

}
