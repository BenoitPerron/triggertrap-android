package at.photosniper.analytics;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper class to cover tracking of Analytics
 * <p>
 * Created by scottmellors on 21/08/2014.
 *
 * @since 2.3
 */
public class AnalyticTracker {

    private static final String PRODUCTION_KEY = "4122506383dcc763557d3aff6fc2e85e";
    private static AnalyticTracker mInstance;
    //    private MixpanelAPI mMixpanel;
    private static JSONObject mProperties;
    private Context mContext;
    private Calendar mStartTime;

    public AnalyticTracker(Context context) {
        mContext = context;
    }

    public static AnalyticTracker getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AnalyticTracker(context);
        }

        mProperties = new JSONObject();

        return mInstance;
    }

    public void startSession() {

//        mMixpanel = MixpanelAPI.getInstance(mContext, PRODUCTION_KEY);

        mStartTime = Calendar.getInstance();
    }

    public void endSession() {

        //Generate time in seconds between now and mStartTime
        Calendar timeNow = Calendar.getInstance();
        long diffInMs = 0;
        if (mStartTime != null) {
            diffInMs = timeNow.getTime().getTime() - mStartTime.getTime().getTime();
        }
        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);

        JSONObject properties = new JSONObject();
        try {
            properties.put(Property.SESSION_DURATION, diffInSec);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //push to mixpanelmanger
//        mMixpanel.track(Event.SESSION_COMPLETED, properties);

        //tidy up variables
        mStartTime = null;
    }

    public void trackEvent(String eventName) {
//        mMixpanel.track(eventName, mProperties);
    }

    public void addProperty(String key, String value) {
        try {
            mProperties.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addProperty(String key, int value) {
        try {
            mProperties.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addProperty(String key, long value) {
        try {
            mProperties.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void flush() {
//        mMixpanel.flush();
    }

    public interface Event {
        String SEQUENCE_COMPLETED = "Sequence Completed";
        String SESSION_COMPLETED = "Session Completed";
    }

    public interface Property {
        String MODE = "Mode";
        String NO_EXPOSURES_TAKEN = "No. Exposures Taken";
        String SESSION_DURATION = "Session Duration";
        String EXPOSURE_DURATION = "Exposure Duration";
        String SEQUENCE_DURATION = "Sequence Duration";
        String LANGUAGE = "Language";
        String TIMELAPSE_INTERVAL = "Timelapse Interval";
        String SENSOR_DELAY = "Sensor Delay";
        String SENSOR_RESET_DELAY = "Sensor Reset Delay";
        String PULSE_LENGTH = "Pulse Length";
        String SPEED_UNIT = "Speed Unit";
        String DISTANCE_UNIT = "Distance Unit";
    }

}
