package at.photosniper.util;

import android.app.Activity;
import android.content.Context;

import at.photosniper.TTApp;
import at.photosniper.fragments.dialog.ScopeFeelingsDialog;

/**
 * Class to handle the display of dialogs coercing users to rate the app in the
 * play store.
 *
 * @author scottmellors
 * @since 2.2
 */
public class AppRater {

    private static final int DAYS_UNTIL_PROMPT = 3;
    private static final int LAUNCHES_UNTIL_PROMPT = 7;
    private static final long DAY_IN_MILLI = 86400000;

    public static void appLaunched(Context mContext, Activity mActivity) {

        TTApp app = TTApp.getInstance(mContext);

        if (!app.getShowAgain()) {
            return;
        }

        // Increment launch counter
        int launchCount = app.getLaunchCount() + 1;
        app.setLaunchCount(launchCount);

        // Get date of first launch
        Long dateFirstLaunch = app.getFirstLaunchDate();

        if (dateFirstLaunch == 0) {
            dateFirstLaunch = System.currentTimeMillis();
            app.setFirstLaunchDate(dateFirstLaunch);
        }

        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= dateFirstLaunch + (DAYS_UNTIL_PROMPT * DAY_IN_MILLI)) {
                showRateDialog(mContext, mActivity);
            }
        }

    }

    private static void showRateDialog(final Context context, final Activity activity) {

        ScopeFeelingsDialog dialog = new ScopeFeelingsDialog();
        dialog.show(context, activity);

    }

}
