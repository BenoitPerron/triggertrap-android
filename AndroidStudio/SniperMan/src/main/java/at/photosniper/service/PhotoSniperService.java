package at.photosniper.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RemoteViews;

import com.praetoriandroid.cameraremote.rpc.ActTakePictureRequest;
import com.praetoriandroid.cameraremote.rpc.ActTakePictureResponse;

import java.util.ArrayList;
import java.util.Calendar;

import at.photosniper.PhotoSniperApp;
import at.photosniper.R;
import at.photosniper.activities.MainActivity;
import at.photosniper.inputs.MicVolumeMonitor;
import at.photosniper.location.PhotoSniperLocationService;
import at.photosniper.outputs.OutputDispatcher;
import at.photosniper.outputs.OutputDispatcher.OutputListener;
import at.photosniper.util.PulseGenerator;
import at.photosniper.util.SonyWiFiRPC;
import at.photosniper.util.StopwatchTimer;
import at.photosniper.wifi.IZeroConf;
import at.photosniper.wifi.PhotoSniperServiceInfo;
import at.photosniper.wifi.PhotoSniperSlaveInfo;
import at.photosniper.wifi.SlaveSocket;


public class PhotoSniperService extends Service implements OutputListener, SlaveSocket.SlaveListener, MicVolumeMonitor.VolumeListener, PhotoSniperLocationService.LocationListener
{

    private static final String TAG = PhotoSniperService.class.getSimpleName();
    private static final String STOP_SERVICE_ACTION = "stop_service_action";

    // Binder given to clients
    private final IBinder mBinder = new PhotoSniperServiceBinder();
    private int mState = State.IDLE;
    // Can't find a way to get at Service flags directly so track status here.
    private boolean mIsRunningInForeground = false;
    private PhotoSniperServiceListener mListener = null;
    private int mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;

    private PowerManager mPowerManager;

    // Sequence vars
    private long[] mSequence;
    private int mSequenceCursor = 0;
    private int mSequenceCount = 0;
    private int mSequenceInterationCount = 0;
    private long mTimeToNextExposure = 0;
    private long mTotalTimeForSequence = 0;
    private OutputDispatcher mOutputDispatcher;
    private boolean mRepeatSequence = false;

    // Used to track the total time for the completed/remaining exposures and
    // gaps.
    private long mCompletedIterationsTime = 0;
    private long mRemaingIterationsTime = 0;
    private long mRemainingSequenceTime = 0;

    // Timed mode
    private CountDownTimer mCountDownTimer;

    // Start Stop mode
    private StopwatchTimer mStopwatchTimer;

    // Wifi vars
    private IZeroConf mZeroConf;
    private SlaveSocket mSlaveSocket;
    private final ArrayList<PhotoSniperServiceInfo> mAvailableMasters = new ArrayList<>();
    private String mConnectedMasterName = "";
    private boolean mIsWifiMasterOn = false;

    // Sound Sensor
    private MicVolumeMonitor mMicVolumeMonitor;

    // Distance Lapse
    private PhotoSniperLocationService mLocationService = null;
    private int mTriggerDistance = PhotoSniperApp.DISTANCELAPSE_DISTANCE_DEFAULT;
    private float mAccumulativeDistance = 0;
    private float mSpeed;

    // Pebble
    //private PebbleController mPebbleController;

    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    private final Handler mHandler = new Handler();

    private RemoteViews mRemoteViews;

    private Calendar mSequenceStartStopTime;
    // Used for stopping service from Notification bar.
    private final BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopCurrentAction();
            goTobackground();
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "Service onCreate");
        registerReceiver(stopServiceReceiver, new IntentFilter(STOP_SERVICE_ACTION));

        BitmapDrawable iconDrawable = (BitmapDrawable) this.getResources().getDrawable(R.drawable.ps_nofication_large);
        Bitmap largeIconBitmap = iconDrawable.getBitmap();
        int height = (int) this.getResources().getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) this.getResources().getDimension(android.R.dimen.notification_large_icon_width);
        largeIconBitmap = Bitmap.createScaledBitmap(largeIconBitmap, width, height, false);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mRemoteViews = new RemoteViews(getPackageName(), R.layout.custom_notification);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(STOP_SERVICE_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.stopService, pendingIntent);
        // mRemoteViews.setBoolean(R.id.notificationDescription,"setSelected",
        // true);

        mOutputDispatcher = new OutputDispatcher(this, this);
        mNotificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        mNotificationBuilder = new Notification.Builder(this).setContent(mRemoteViews).setTicker("PhotoSniper", mRemoteViews).setSmallIcon(R.drawable.notification_icon);


        // TODO Create a factory here to get Correct ZeroConf Implementation
//        mZeroConf = new ZeroConfJmdns(this);
        // mZeroConf = new ZeroConfNds(this);
        mMicVolumeMonitor = new MicVolumeMonitor(this);

        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        mConnectedMasterName = this.getResources().getString(R.string.unconnected);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind: listener is: " + mListener);

        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy: listener is " + mListener);
        unregisterReceiver(stopServiceReceiver);
        mOutputDispatcher.close();
        mZeroConf.unwatch();
        mZeroConf.unregisterMaster();
        mZeroConf.close();
        if (mSlaveSocket != null) {
            mSlaveSocket.close();
        }
        mMicVolumeMonitor.stop();
        mMicVolumeMonitor.release();

    }

    public void goToForeground() {
        Log.d(TAG, "Moving service to Foreground");
        String notificationText = getNotifcationText(mOnGoingAction);
        if (mOnGoingAction == PhotoSniperApp.OnGoingAction.WI_FI_SLAVE) {
            notificationText = getWifiSlaveNotification();
        }
        // mNotificationBuilder.setContentText(notificationText);
        mRemoteViews.setCharSequence(R.id.notificationDescription, "setText", notificationText);
        PendingIntent notificationPendingIntent = getNotifcationPendingIntent();
        mNotificationBuilder.setContentIntent(notificationPendingIntent);
        // Need to use the older method getNotification to support pre 4.1
        // Android
        this.startForeground(R.string.ps_foreground_service_started, mNotificationBuilder.getNotification());
        mIsRunningInForeground = true;
    }

    public void goTobackground() {
        Log.d(TAG, "Moving service to Background");
        this.stopForeground(true);
        mIsRunningInForeground = false;
    }

    public int getState() {
        return mState;
    }

    public int getOnGoingAction() {
        return mOnGoingAction;
    }

    public boolean isFragmentActive(String tag) {
        boolean isActive = false;

        if (tag.equals(PhotoSniperApp.FragmentTags.TIMELAPSE) && mOnGoingAction == PhotoSniperApp.OnGoingAction.TIMELAPSE) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.HDR) && mOnGoingAction == PhotoSniperApp.OnGoingAction.HDR) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.TIMED) && mOnGoingAction == PhotoSniperApp.OnGoingAction.TIMED) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.PRESS_TO_START) && mOnGoingAction == PhotoSniperApp.OnGoingAction.PRESS_START_STOP) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.SELF_TIMER) && mOnGoingAction == PhotoSniperApp.OnGoingAction.SELF_TIMER) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.BANG) && mOnGoingAction == PhotoSniperApp.OnGoingAction.BANG) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.STARTRAIL) && mOnGoingAction == PhotoSniperApp.OnGoingAction.STAR_TRAIL) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.BRAMPING) && mOnGoingAction == PhotoSniperApp.OnGoingAction.BRAMPING) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.DISTANCE_LAPSE) && mOnGoingAction == PhotoSniperApp.OnGoingAction.DISTANCE_LAPSE) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.HDR_LAPSE) && mOnGoingAction == PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.TIMEWARP) && mOnGoingAction == PhotoSniperApp.OnGoingAction.TIMEWARP) {
            isActive = true;
        } else if (tag.equals(PhotoSniperApp.FragmentTags.WIFI_SLAVE) && mOnGoingAction == PhotoSniperApp.OnGoingAction.WI_FI_SLAVE) {
            isActive = true;
        }

        return isActive;
    }

    public boolean isWifiMasterOn() {
        return mIsWifiMasterOn;
    }

    public void stopCurrentAction() {
        switch (mOnGoingAction) {
            case PhotoSniperApp.OnGoingAction.PRESS_START_STOP:
                stopStopWatch();
                break;
            case PhotoSniperApp.OnGoingAction.SELF_TIMER:
                stopSelfTimerMode();
                break;
            case PhotoSniperApp.OnGoingAction.TIMED:
                stopTimedMode();
                break;
            case PhotoSniperApp.OnGoingAction.BANG:
                disableSoundThreshold();
                break;
            case PhotoSniperApp.OnGoingAction.DISTANCE_LAPSE:
                stopLocationUpdates();
                break;
            case PhotoSniperApp.OnGoingAction.WI_FI_SLAVE:
                unWatchMasterWifi();
                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
            case PhotoSniperApp.OnGoingAction.TIMEWARP:
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
            case PhotoSniperApp.OnGoingAction.TIMELAPSE:
            case PhotoSniperApp.OnGoingAction.HDR:
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                stopSequence();
                break;
        }
    }

    public void setListener(PhotoSniperServiceListener listener) {
        mListener = listener;
    }

    public boolean checkInProgressState() {
        boolean isInProgress = false;
        if (mState == State.IN_PROGRESS) {
            if (mListener != null) {
                mListener.onServiceActionRunning(getNotifcationText(mOnGoingAction));

            }
            isInProgress = true;
        }
        return isInProgress;
    }

    public void startPulseSequence(int onGoingAction, long[] sequence, boolean repeat) {
        if (checkInProgressState()) {
            return;
        }
        mRepeatSequence = repeat;
        mSequence = sequence;
        mSequenceInterationCount = 1;
        mSequenceCount = 0;
        mSequenceCursor = 0;
        mState = State.IN_PROGRESS;
        mOnGoingAction = onGoingAction;
        mTimeToNextExposure = (int) (sequence[mSequenceCursor] + sequence[mSequenceCursor + 1]);

        mTotalTimeForSequence = PulseGenerator.getSequenceTime(sequence);
        mRemaingIterationsTime = mTotalTimeForSequence;
        mCompletedIterationsTime = 0;

        playNextPulseInSequence();

    }

    private void playNextPulseInSequence() {

        // Just check if we need to repeat this sequence.
        if (mSequence == null || mSequence.length < mSequenceCursor + 2 && mRepeatSequence) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
            mSequenceCount = 0;
            mSequenceInterationCount++;
            if (mListener != null && !mIsRunningInForeground) {
                mListener.onPulseSequenceIterate(mSequence);
            }
            mSequenceCursor = 0;
            if (mSequence != null) {
                mTimeToNextExposure = (int) (mSequence[mSequenceCursor] + mSequence[mSequenceCursor + 1]);
                mTotalTimeForSequence = PulseGenerator.getSequenceTime(mSequence);
            }
            mRemaingIterationsTime = mTotalTimeForSequence;
            mRemainingSequenceTime = mRemaingIterationsTime;
            mCompletedIterationsTime = 0;
        }

        if (mSequence == null || mSequence.length < mSequenceCursor + 2) {
            Log.d(TAG, "End of pulse sequence");
            updatePulseListenerStop();
            mState = State.IDLE;
            mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
            mOutputDispatcher.stop();
            if (mIsRunningInForeground) {
                goTobackground();
                stopSelf();
            }
            return;
        }
        if (mSequence[mSequenceCursor] > 0) {
            mSequenceCount++;
        }
        // Log.d(TAG, "Sending pulse for " + mSequence[mSequenceCursor] +
        // " then pausing for " + mSequence[mSequenceCursor + 1] + " count: " +
        // mSequenceCount + " sequence length: " + mSequence.length);

        mTimeToNextExposure = (mSequence[mSequenceCursor] + mSequence[mSequenceCursor + 1]);
        mOutputDispatcher.trigger(mSequence[mSequenceCursor++], mSequence[mSequenceCursor++]);

        // if (mCountDownTimer != null) {
        // mCountDownTimer.cancel();
        // }

        // Just shorten the length of the timer a little (100ms)to stop timer
        // overlaps
        // Should be ok as this is just UI feedback timing.
        mCountDownTimer = new CountDownTimer(mTimeToNextExposure - 100, 5) {

            private final int FIVE_TENTHS_INTERVAL = 9;
            private int intervalCount = 0;

            public void onTick(long millisUntilFinished) {
                mRemainingSequenceTime = mRemaingIterationsTime - (mTimeToNextExposure - millisUntilFinished);
                intervalCount++;
                if (mListener != null && !mIsRunningInForeground) {
                    updatePulseListenerProgress(millisUntilFinished);
                } else {
                    if (intervalCount == FIVE_TENTHS_INTERVAL) {
                        upDateNotification(millisUntilFinished);
                    }
                }
                intervalCount = (intervalCount > FIVE_TENTHS_INTERVAL) ? 0 : intervalCount;
            }

            public void onFinish() {

                if (mListener != null && !mIsRunningInForeground) {
                    updatePulseListenerProgress(0);
                }

                mCompletedIterationsTime += mTimeToNextExposure;
                mRemaingIterationsTime = mTotalTimeForSequence - mCompletedIterationsTime;
                // Log.d(TAG,"Remain iterations time: " +
                // mRemaingIterationsTime);
                // Log.d(TAG,"Completed iterations time:" +
                // mCompletedIterationsTime);
            }
        }.start();

        if (mListener != null && !mIsRunningInForeground) {
            updatePulseListenerStart();
        }

    }

    private void upDateNotification(long millisUntilFinished) {
        if (mIsRunningInForeground && mPowerManager.isScreenOn()) {
            String notificationText = "";
            switch (mOnGoingAction) {
                case PhotoSniperApp.OnGoingAction.SELF_TIMER:
                    notificationText = getNotifcationText(mOnGoingAction) + " " + formatMilliSecondsTime(millisUntilFinished);
                    break;
                case PhotoSniperApp.OnGoingAction.PRESS_START_STOP:
                    notificationText = getNotifcationText(mOnGoingAction) + " " + formatMilliSecondsTime(millisUntilFinished);
                    break;
                case PhotoSniperApp.OnGoingAction.TIMED:
                    notificationText = getNotifcationText(mOnGoingAction) + " " + formatMilliSecondsTime(millisUntilFinished);
                    break;
                case PhotoSniperApp.OnGoingAction.TIMELAPSE:
                    notificationText = getNotifcationText(mOnGoingAction) + " " + mSequenceInterationCount + " " + formatMilliSecondsTime(millisUntilFinished);
                    break;
                case PhotoSniperApp.OnGoingAction.WI_FI_SLAVE:
                    notificationText = getWifiSlaveNotification();
                    break;
                case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                case PhotoSniperApp.OnGoingAction.HDR:
                case PhotoSniperApp.OnGoingAction.BRAMPING:
                case PhotoSniperApp.OnGoingAction.TIMEWARP:
                    notificationText = getNotifcationText(mOnGoingAction) + " " + mSequenceCount + " " + formatMilliSecondsTime(mRemainingSequenceTime);
                    break;
                case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                    notificationText = getNotifcationText(mOnGoingAction) + " " + mSequenceInterationCount + " " + formatMilliSecondsTime(mRemainingSequenceTime);
                default:
                    // Do nothing

            }
            // mNotificationBuilder.setContentText(notificationText);
            mRemoteViews.setCharSequence(R.id.notificationDescription, "setText", notificationText);
            mNotificationManager.notify(R.string.ps_foreground_service_started, mNotificationBuilder.getNotification());

        }
    }

    private void updatePulseListenerStart() {
        switch (mOnGoingAction) {
            case PhotoSniperApp.OnGoingAction.TIMELAPSE:
                mListener.onPulseStart(mSequenceInterationCount, 0, mTimeToNextExposure, 0);
                break;
            case PhotoSniperApp.OnGoingAction.HDR:
                mListener.onPulseStart(mSequenceCount, (mSequence.length / 2), mTimeToNextExposure, mTotalTimeForSequence);
                break;
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                mListener.onPulseStart(mSequenceInterationCount, (mSequence.length / 2), mTimeToNextExposure, mTotalTimeForSequence);
                break;
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                mListener.onPulseStart(mSequenceCount, (mSequence.length / 2), mTimeToNextExposure, mTotalTimeForSequence);
                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
                mListener.onPulseStart(mSequenceCount, (mSequence.length / 2), mTimeToNextExposure, mTotalTimeForSequence);
                break;
            case PhotoSniperApp.OnGoingAction.TIMEWARP:
                mListener.onPulseStart(mSequenceCount, (mSequence.length / 2), mTimeToNextExposure, mTotalTimeForSequence);
                break;
            default:
                // If we can't identify ongoing action do no update listener.
        }

    }

    private void updatePulseListenerProgress(long remainingPulseTime) {
        switch (mOnGoingAction) {
            case PhotoSniperApp.OnGoingAction.TIMELAPSE:
                mListener.onPulseUpdate(mSequence, mSequenceInterationCount, mTimeToNextExposure, remainingPulseTime, 0);
                break;
            case PhotoSniperApp.OnGoingAction.HDR:
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                mListener.onPulseUpdate(mSequence, mSequenceCount, mTimeToNextExposure, remainingPulseTime, mRemainingSequenceTime);
                break;
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                // The remaining sequence time is the remain time of the iterations
                // minus the expired time of the current iteration.
                mListener.onPulseUpdate(mSequence, mSequenceCount, mTimeToNextExposure, remainingPulseTime, mRemainingSequenceTime);
                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
                // The remaining sequence time is the remain time of the iterations
                // minus the expired time of the current iteration.
                mListener.onPulseUpdate(mSequence, mSequenceCount, mTimeToNextExposure, remainingPulseTime, mRemainingSequenceTime);
                break;
            case PhotoSniperApp.OnGoingAction.TIMEWARP:
                // The remaining sequence time is the remain time of the iterations
                // minus the expired time of the current iteration.
                mListener.onPulseUpdate(mSequence, mSequenceCount, mTimeToNextExposure, remainingPulseTime, mRemainingSequenceTime);
                break;
            default:
                // If we can't identify ongoing action do no update listener.
        }

    }

    private void updatePulseListenerStop() {
        if (mListener != null) {
            mListener.onPulseFinished();
        }
    }

    private String getNotifcationText(int onGoingAction) {
        String[] notifcations = getResources().getStringArray(R.array.ps_notifications);
        String notifcationText = notifcations[onGoingAction];

        if (mOnGoingAction == PhotoSniperApp.OnGoingAction.DISTANCE_LAPSE) {
            int distanceToTrigger = mTriggerDistance - (int) mAccumulativeDistance;
            notifcationText += " " + distanceToTrigger + " m to trigger";
        }
        return notifcationText;
    }

    private PendingIntent getNotifcationPendingIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_FROM_BACKGROUND);
        // TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // stackBuilder.addParentStack(MainActivity.class);

        switch (mOnGoingAction) {
            case PhotoSniperApp.OnGoingAction.PRESS_START_STOP:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.PRESS_TO_START);
                break;
            case PhotoSniperApp.OnGoingAction.TIMED:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.TIMED);
                break;
            case PhotoSniperApp.OnGoingAction.TIMELAPSE:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.TIMELAPSE);
                break;
            case PhotoSniperApp.OnGoingAction.TIMEWARP:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.TIMEWARP);
                break;
            case PhotoSniperApp.OnGoingAction.BANG:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.BANG);
                break;
            case PhotoSniperApp.OnGoingAction.HDR:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.HDR);
                break;
            case PhotoSniperApp.OnGoingAction.HDR_TIMELAPSE:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.HDR_LAPSE);
                break;
            case PhotoSniperApp.OnGoingAction.STAR_TRAIL:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.STARTRAIL);
                break;
            case PhotoSniperApp.OnGoingAction.BRAMPING:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.BRAMPING);
                break;
            case PhotoSniperApp.OnGoingAction.DISTANCE_LAPSE:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.DISTANCE_LAPSE);
                break;
            case PhotoSniperApp.OnGoingAction.WI_FI_SLAVE:
                notificationIntent.putExtra(MainActivity.FRAGMENT_TAG, PhotoSniperApp.FragmentTags.WIFI_SLAVE);
                break;
            default:
                // Do nothing don't add a tag.
        }

        // stackBuilder.addNextIntent(notificationIntent);
        // PendingIntent resultPendingIntent =
        // stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        return PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void stopSequence() {

        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
        mRepeatSequence = false;
        mOutputDispatcher.stop();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        mSequence = null;
        mState = State.IDLE;
    }

    /*
     * Simple mode method
     */
    public void startSimple() {
        if (checkInProgressState()) {
            return;
        }
        if (mListener != null) {
            mListener.onServiceStartSimple();
        }

        trigger(PhotoSniperApp.getInstance(this).getBeepLength());


    }

    /*
     *
     */
    private void playWifiSlaveBeep() {

        if (mListener != null) {
            mListener.onServiceStartSimple();
        }
        trigger(PhotoSniperApp.getInstance(this).getBeepLength());
    }

    /*
     * Timed mode methods
     */
    public void startTimedMode(long time) {
        if (checkInProgressState()) {
            return;
        }
        mOnGoingAction = PhotoSniperApp.OnGoingAction.TIMED;
        mListener.onServiceTimedStart(time);
        mCountDownTimer = new CountDownTimer(time, 5) {
            private final int FIVE_TENTHS_INTERVAL = 9;
            private int intervalCount = 0;

            public void onTick(long millisUntilFinished) {
                intervalCount++;
                if (mListener != null && !mIsRunningInForeground) {
                    mListener.onServiceTimedUpdate(millisUntilFinished);
                } else {
                    if (intervalCount == FIVE_TENTHS_INTERVAL) {
                        upDateNotification(millisUntilFinished);
                    }
                }
                intervalCount = (intervalCount > FIVE_TENTHS_INTERVAL) ? 0 : intervalCount;
            }

            public void onFinish() {
                mState = State.IDLE;
                mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;

                if (mListener != null) {
                    mListener.onServiceTimedStop();
                }

                mOutputDispatcher.stop();
                if (mIsRunningInForeground) {
                    goTobackground();
                    stopSelf();
                }
            }
        }.start();
        mState = State.IN_PROGRESS;
        mOutputDispatcher.start();
    }

    private String formatMilliSecondsTime(long time) {
        int hundreds, seconds, minutes, hours;
        seconds = (int) time / 1000;
        hundreds = (int) (time - seconds * 1000) / 10;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        StringBuilder formattedTime = new StringBuilder().append(String.format("%02d", hours)).append(":").append(String.format("%02d", minutes)).append(":").append(String.format("%02d", seconds)).append(":").append(String.format("%02d", hundreds));

        return formattedTime.toString();

    }

    public void stopTimedMode() {
        mCountDownTimer.cancel();
        mListener.onServiceTimedStop();
        mState = State.IDLE;
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
        mOutputDispatcher.stop();
    }

    /**
     * Self Timer Mode methods
     */

    public void startSelfTimer(long time) {
        if (checkInProgressState()) {
            return;
        }
        mOnGoingAction = PhotoSniperApp.OnGoingAction.SELF_TIMER;
        mListener.onServiceTimedStart(time);
        mCountDownTimer = new CountDownTimer(time, 5) {
            private final int FIVE_TENTHS_INTERVAL = 9;
            private int intervalCount = 0;

            public void onTick(long millisUntilFinished) {
                intervalCount++;
                if (mListener != null && !mIsRunningInForeground) {
                    mListener.onServiceTimedUpdate(millisUntilFinished);
                } else {
                    if (intervalCount == FIVE_TENTHS_INTERVAL) {
                        upDateNotification(millisUntilFinished);
                    }
                }
                intervalCount = (intervalCount > FIVE_TENTHS_INTERVAL) ? 0 : intervalCount;
            }

            public void onFinish() {
                finishSelfTimerMode();
            }
        }.start();
        mState = State.IN_PROGRESS;


    }

    private void finishSelfTimerMode() {
        trigger(PhotoSniperApp.getInstance(this).getBeepLength());

        stopSelfTimerMode();
    }

    public void stopSelfTimerMode() {
        mCountDownTimer.cancel();

        mState = State.IDLE;
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;

        if (mListener != null) {
            mListener.onServiceTimedStop();
        }

        if (mIsRunningInForeground) {
            goTobackground();
            stopSelf();
        }
    }

    /*
     * Stopwatch (Star/Stop mode) methods
     */
    public void startStopwatch() {
        if (checkInProgressState()) {
            return;
        }
        mOnGoingAction = PhotoSniperApp.OnGoingAction.PRESS_START_STOP;
        mListener.onServiceStopwatchStart();
        mStopwatchTimer = new StopwatchTimer() {
            private final int FIVE_TENTHS_INTERVAL = 9;
            private int intervalCount = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                intervalCount++;
                if (mListener != null && !mIsRunningInForeground) {
                    mListener.onServiceStopwatchUpdate(millisUntilFinished);
                } else {
                    if (intervalCount == FIVE_TENTHS_INTERVAL) {
                        upDateNotification(millisUntilFinished);
                    }
                }
                intervalCount = (intervalCount > FIVE_TENTHS_INTERVAL) ? 0 : intervalCount;

            }
        }.start();
        mState = State.IN_PROGRESS;
        mOutputDispatcher.start();
    }

    public void stopStopWatch() {
        mStopwatchTimer.cancel();
        mListener.onServiceStopwatchStop();
        mState = State.IDLE;
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
        mOutputDispatcher.stop();
    }

    public void onStartPress() {
        if (checkInProgressState()) {
            return;
        }
        mOnGoingAction = PhotoSniperApp.OnGoingAction.PRESS_AND_HOLD;
        mListener.onServicePressStart();
        mStopwatchTimer = new StopwatchTimer() {
            @Override
            public void onTick(long millisUntilFinished) {
                if (mListener != null && !mIsRunningInForeground) {
                    mListener.onServicePressUpdate(millisUntilFinished);
                }
            }
        }.start();
        mState = State.IN_PROGRESS;
        mOutputDispatcher.start();
    }

    public void onStopPress() {
        mStopwatchTimer.cancel();
        mListener.onServicePressStop();
        mState = State.IDLE;
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
        mOutputDispatcher.stop();
    }

    /**
     * Listener for QuickRelease
     */

    public void onQuickStartPress() {
        if (checkInProgressState()) {
            return;
        }
        mOnGoingAction = PhotoSniperApp.OnGoingAction.QUICK_RELEASE;
        mListener.onServicePressStart();
        mStopwatchTimer = new StopwatchTimer() {
            @Override
            public void onTick(long millisUntilFinished) {
                if (mListener != null && !mIsRunningInForeground) {
                    mListener.onServicePressUpdate(millisUntilFinished);
                }

            }
        }.start();
        mState = State.IN_PROGRESS;
    }

    public void onQuickStopPress() {
        trigger(PhotoSniperApp.getInstance(this).getBeepLength());
        mStopwatchTimer.cancel();
        mListener.onServicePressStop();
        mState = State.IDLE;
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
    }

    /**
     * Listener for MicVolumeMonitor
     */
    @Override
    public void onVolumeUpdate(int amplitude) {
        if (mListener != null && !mIsRunningInForeground) {
            mListener.onSoundVolumeUpdate(amplitude);
        }

        if (mIsRunningInForeground) {
            String notificationText = getNotifcationText(mOnGoingAction);
            // mNotificationBuilder.setContentText(notificationText);
            mRemoteViews.setCharSequence(R.id.notificationDescription, "setText", notificationText);
//            mNotificationManager.notify(R.string.ps_foreground_service_started,
//                    mNotificationBuilder.getNotification());
        }

    }

    @Override
    public void onExceedThreshold(int amplitude) {
        trigger(PhotoSniperApp.getInstance(this).getBeepLength());
        if (mListener != null && !mIsRunningInForeground) {
            mListener.onSoundExceedThreshold(amplitude);
        }
    }

    /*
     * SoundSensor (Bang) Control methods
     */
    public void startSoundSensor() {
        mMicVolumeMonitor.start();
    }

    public void stopSoundSensor() {
        // Only stop the mic monitor is we are not watching it
        if (mOnGoingAction != PhotoSniperApp.OnGoingAction.BANG) {
            mMicVolumeMonitor.stop();
        }
    }

    public void enableSoundThreshold() {
        if (checkInProgressState()) {
            return;
        }
        mOnGoingAction = PhotoSniperApp.OnGoingAction.BANG;
        mMicVolumeMonitor.enabledThreshold();
        mState = State.IN_PROGRESS;
    }

    public void disableSoundThreshold() {
        mMicVolumeMonitor.disableThreshold();
        mState = State.IDLE;
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
    }

    public void setMicSensitivity(int sensitivity) {
        mMicVolumeMonitor.setMicSensitivity(sensitivity);

    }

    public void setSoundThreshold(int threshold) {
        mMicVolumeMonitor.setThreshold(threshold);
    }

    /**
     * Listener for updates from the location service
     */
    @Override
    public void onDistanceChanged(float distanceTraveled, float speed) {
        Log.d(TAG, "New distance is : " + distanceTraveled);
        mSpeed = speed;

        mAccumulativeDistance += distanceTraveled;

        if (mListener != null) {
            mListener.onDistanceUpdated(mAccumulativeDistance, speed);
        }

        if (mAccumulativeDistance >= mTriggerDistance) {
            // Trigger a beep if we travel greater than the Trigger distance.
            trigger(PhotoSniperApp.getInstance(this).getBeepLength());
            mAccumulativeDistance = mAccumulativeDistance % mTriggerDistance;
        }

        if (mIsRunningInForeground) {
            String notificationText = getNotifcationText(mOnGoingAction);
            // mNotificationBuilder.setContentText(notificationText);
            mRemoteViews.setCharSequence(R.id.notificationDescription, "setText", notificationText);
            mNotificationManager.notify(R.string.ps_foreground_service_started, mNotificationBuilder.getNotification());
        }
    }

    public float getAccumulativeDistance() {
        float accDistance = mAccumulativeDistance;
        if (mAccumulativeDistance >= mTriggerDistance) {
            accDistance = mAccumulativeDistance % mTriggerDistance;
        }
        return accDistance;
    }

    public float getSpeed() {
        return mSpeed;
    }

    /*
     * DistanceLapse methods
     */
    public void setTTLocationService(PhotoSniperLocationService locationService) {
        mLocationService = locationService;
    }

    public void startLocationUpdates(int triggerDistance) {
        if (checkInProgressState()) {
            return;
        }
        Log.d(TAG, "Starting location services: " + mLocationService.toString());
        mOnGoingAction = PhotoSniperApp.OnGoingAction.DISTANCE_LAPSE;
        mState = State.IN_PROGRESS;
        mTriggerDistance = triggerDistance;
        mAccumulativeDistance = 0;
        mSpeed = 0;
        mLocationService.setListener(this);
        mLocationService.startLocationService();

    }

    public void stopLocationUpdates() {
        if (mLocationService != null) {
            Log.d(TAG, "Stopping location services: " + mLocationService.toString());
            mLocationService.stopLocationService();
        }
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
        mState = State.IDLE;
    }

    /*
     * Wifi methods
     */
    public ArrayList<PhotoSniperServiceInfo> getAvailableMasters() {
        return mAvailableMasters;
    }

    public ArrayList<PhotoSniperSlaveInfo> getConnectedSlaves() {
        return mZeroConf.getConnectedSlaves();
    }

    private String getWifiSlaveNotification() {
        String notificationText = getNotifcationText(mOnGoingAction);

        if (mConnectedMasterName.equals(getResources().getString(R.string.unconnected))) {
            notificationText += " " + mConnectedMasterName;
        } else {
            notificationText += " " + getResources().getString(R.string.Connected_to) + " " + mConnectedMasterName;
        }
        // mNotificationBuilder.setContentText(notificationText);
        mRemoteViews.setCharSequence(R.id.notificationDescription, "setText", notificationText);
        return notificationText;

    }

    public void watchMasterWifi() {

        mZeroConf.watch();
        mOnGoingAction = PhotoSniperApp.OnGoingAction.WI_FI_SLAVE;
        mState = State.IN_PROGRESS;

    }

    public void unWatchMasterWifi() {
        mZeroConf.unwatch();
        // Close any sockets that might be open.
        disconnectFromMaster();
        mOnGoingAction = PhotoSniperApp.OnGoingAction.NONE;
        mState = State.IDLE;

    }

    public void registerWifiMaster() {

        AsyncTask<Void, Void, Void> registerMasterTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mZeroConf.registerMaster();
                return null;
            }
        };
        registerMasterTask.execute();
    }

    public void unRegsiterMaster() {
        AsyncTask<Void, Void, Void> unregisterMasterTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mZeroConf.unregisterMaster();
                return null;
            }
        };
        unregisterMasterTask.execute();
    }

    public void disconnectSlaveFromMaster(String uniqueName) {
        mZeroConf.disconnectSlaveFromMaster(uniqueName);
    }

    public void onWifiMasterRegistered(PhotoSniperServiceInfo info) {
        mIsWifiMasterOn = true;
        //if (mListener != null) {
        mListener.onWifiMasterRegsitered(info);
        //}
    }

    public void onWifiMasterUnregistered() {
        mIsWifiMasterOn = false;
        if (mListener != null) {
            mListener.onWifiMasterUnregister();
        }
    }

    public void onClientConnectionReceived(String name, String uniqueName) {
        mListener.onClientConnected(name, uniqueName);
    }

    public void onClientDisconnectionReceived(String name, String uniqueName) {
        mListener.onClientDisconnected(name, uniqueName);
    }

    private void connectToMaster(String name, String ipAddress, int port) {
        Log.d(TAG, "Connect to master");
        if (mSlaveSocket == null) {
            mSlaveSocket = new SlaveSocket(this);
        } else {
            mSlaveSocket.close();
        }
        mSlaveSocket.connect(ipAddress, port);
        mConnectedMasterName = name;

        upDateNotification(0);

    }

    private void disconnectFromMaster() {
        Log.d(TAG, "Disconnect from master");
        if (mSlaveSocket != null) {
            Log.d(TAG, "Closing the slave socket");
            mSlaveSocket.close();
        }
        mConnectedMasterName = getResources().getString(R.string.unconnected);

    }

    /**
     * Listener for SlaveSocket
     */
    @Override
    public void onSlaveBeep() {
        playWifiSlaveBeep();

    }

    public void wiFiMasterAdded(String name, String ipAddress, int port) {
        PhotoSniperServiceInfo masterInfo = new PhotoSniperServiceInfo(name, ipAddress, port);
        mAvailableMasters.add(masterInfo);

        if (!mIsRunningInForeground) {
            mListener.onWifiMasterAdded(masterInfo);
        }

        if (masterInfo.getName().equals(PhotoSniperApp.getInstance(this).getSlaveLastMaster())) {

            connectToMaster(masterInfo.getName(), masterInfo.getIpAddress(), masterInfo.getPort());
        }

        upDateNotification(0);
    }

    public void wiFiMasterRemoved(String name, String ipAddress, int port) {
        // Log.d(TAG,"wiFiMasterRemoved");
        PhotoSniperServiceInfo masterInfo = new PhotoSniperServiceInfo(name, ipAddress, port);
        // mAvailableMasters.remove(masterInfo);
        for (PhotoSniperServiceInfo serviceInfo : mAvailableMasters) {
            if (serviceInfo.getName().equals(name)) {
                mAvailableMasters.remove(serviceInfo);
                break;
            }
        }

        if (!mIsRunningInForeground) {
            mListener.onWifiMasterRemoved(masterInfo);
        }
        if (masterInfo.getName().equals(mConnectedMasterName)) {
            mConnectedMasterName = getResources().getString(R.string.unconnected);
        }
        upDateNotification(0);
    }


    /*
     * Listeners for OutputDispatcher
     */
    @Override
    public void onOutputStart() {
        // Log.d(TAG,"Start PULSE in sequence");

    }

    @Override
    public void onOutputStop() {
        // This callback is not in UI main thread!
        // Log.d(TAG,"Stop PULSE in sequence");

    }

    @Override
    public void onOutputPauseDone() {
        // This callback is not in UI main thread!
        // Log.d(TAG,"Stop PAUSE in sequence");
        mHandler.post(new Runnable() {
            public void run() {
                playNextPulseInSequence();
            }
        });

    }

    private void trigger(long length) {
        mOutputDispatcher.trigger(length);

        // this is a quick hack for SONY ...........
        if (PhotoSniperApp.getInstance(this).isSonyRPCAvailable()) {
            PhotoSniperApp.getInstance(this).getSonyWiFiRpc().sendRequest(new ActTakePictureRequest(), null, new SonyWiFiRPC.ResponseHandler<ActTakePictureResponse>() {

                @Override
                public void onSuccess(ActTakePictureResponse response) {
                    // juchu !!
                }

                @Override
                public void onFail(Throwable e) {
                    Log.e("@@@@@", "Shot failed", e);

                }
            });
        }

        // ... and for BLE
        if (PhotoSniperApp.getInstance(this).getBLEgattClient() != null) {
            PhotoSniperApp.getInstance(this).getBLEgattClient().writeInteractor();
        }


    }

    public void resetSequenceStartStopTime() {
        mSequenceStartStopTime = Calendar.getInstance();
    }

    public Calendar getSequenceStartStopTime() {
        return mSequenceStartStopTime;
    }

    public interface State {
        int IN_PROGRESS = 0;
        int IDLE = 1;
    }


    public interface MessageType {
        int SEQUENCE = 0;
        int SOUND_TRIGGER = 1;

        interface Action {
            int START = 0;
            int STOP = 1;
        }
    }


    //mSequenceStartStopTime getters and setters

    /**
     * Listener for service updates (used by activity)
     *
     * @author neildavies
     */
    public interface PhotoSniperServiceListener {

        // Listener for Action running
        void onServiceActionRunning(String action);

        // Listener Simple mode
        void onServiceStartSimple();

        // Listener for Press and hold
        void onServicePressStart();

        void onServicePressUpdate(long time);

        void onServicePressStop();

        // Listeners for Start/Stop
        void onServiceStopwatchStart();

        void onServiceStopwatchUpdate(long time);

        void onServiceStopwatchStop();

        // Listeners for Timed mode
        void onServiceTimedStart(long time);

        void onServiceTimedUpdate(long time);

        void onServiceTimedStop();

        // Listeners for SoundSensor
        void onSoundVolumeUpdate(int amplitude);

        void onSoundExceedThreshold(int amplitude);

        // Listener for DistanceLapse
        void onDistanceUpdated(float distanceTraveled, float speed);

        // Listeners for Pulse sequence

        /**
         * Callback when service has an update.
         *
         * @param exposures      The number of exposure taken
         * @param totalExposures The number of exposures in a sequence
         * @param timeToNext     The time to the next exposure.
         * @param timeRemaining  The overall time remain for the Action
         */
        void onPulseStart(int exposures, int totalExposures, long timeToNext, long timeRemaining);

        /**
         * @param exposures          The number of pulses done
         * @param timeToNext         The time to the next pulse
         * @param remainingPulseTime the time remain for current pulse
         */
        void onPulseUpdate(long[] sequence, int exposures, long timeToNext, long remainingPulseTime, long remaingSequenceTime);

        void onPulseFinished();

        void onPulseSequenceIterate(long[] sequence);

        // Listeners for wifi slave
        void onWifiMasterAdded(PhotoSniperServiceInfo info);

        void onWifiMasterRemoved(PhotoSniperServiceInfo info);

        // Listeners for wifi master
        void onWifiMasterRegsitered(PhotoSniperServiceInfo info);

        void onWifiMasterUnregister();

        void onClientConnected(String name, String uniqueName);

        void onClientDisconnected(String name, String uniqueName);


    }

    public class PhotoSniperServiceBinder extends Binder {
        public PhotoSniperService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return PhotoSniperService.this;
        }
    }


}
