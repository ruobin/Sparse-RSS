package cn.eric.rss.utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import cn.eric.rss.MainActivity;
import cn.eric.rss.R;

public class ApplicationHelper {

	public static void claimMaidenVoyage(Activity activity) {
		SharedPreferences sharedPref = activity
				.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(
				activity.getResources().getString(R.string.pref_maiden_voyage),
				false);
		editor.commit();
	}

	public static boolean isMaidenVoyage(Activity activity) {
		SharedPreferences sharedPref = activity
				.getPreferences(Context.MODE_PRIVATE);
		boolean isMaidenVoyage = sharedPref.getBoolean(activity.getResources()
				.getString(R.string.pref_maiden_voyage), true);
		return isMaidenVoyage;
	}

	public static void createShortcutOnHomeScreen(Activity activity) {

		Intent shortcutIntent = new Intent(activity.getApplicationContext(),
				MainActivity.class);

		shortcutIntent.setAction(Intent.ACTION_MAIN);

		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.app_name);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(
						activity.getApplicationContext(), R.drawable.ic_logo));

		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		activity.getApplicationContext().sendBroadcast(addIntent);
	}

}
