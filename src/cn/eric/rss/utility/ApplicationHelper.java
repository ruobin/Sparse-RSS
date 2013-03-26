package cn.eric.rss.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.widget.BaseAdapter;
import cn.eric.rss.MainActivity;
import cn.eric.rss.R;
import cn.eric.rss.data.FeedData;

/**
 * used for global operations
 * 
 * @author Ruobin Wang
 * 
 */
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

	/**
	 * check if this is the first use
	 * 
	 * @param activity
	 * @return
	 */
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
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, activity.getResources()
				.getString(R.string.app_name));
		addIntent.putExtra(
				Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(
						activity.getApplicationContext(), R.drawable.ic_logo));

		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		activity.getApplicationContext().sendBroadcast(addIntent);
	}

	/**
	 * decide whether to show the empty view
	 */
	public static void showEmptyView(Activity activity,
			final BaseAdapter adapter, final View emptyView) {
		if (Looper.getMainLooper() == Looper.myLooper()) {
			if ((adapter == null) || adapter.isEmpty()) {
				emptyView.setVisibility(View.VISIBLE);
			} else {
				emptyView.setVisibility(View.GONE);
			}
		} else {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if ((adapter == null) || adapter.isEmpty()) {
						emptyView.setVisibility(View.VISIBLE);
					} else {
						emptyView.setVisibility(View.GONE);
					}
				}
			});
		}
	}

	public static void showDeleteAllEntriesQuestion(final Context context,
			final Uri uri) {
		Builder builder = new AlertDialog.Builder(context);

		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.contextmenu_deleteallentries);
		builder.setMessage(R.string.question_areyousure);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						new Thread() {
							public void run() {
								FeedData.deletePicturesOfFeed(context, uri,
										MyStrings.DB_EXCUDEFAVORITE);
								if (context.getContentResolver().delete(uri,
										MyStrings.DB_EXCUDEFAVORITE, null) > 0) {
									context.getContentResolver().notifyChange(
											FeedData.FeedColumns.CONTENT_URI,
											null);
								}
							}
						}.start();
					}
				});
		builder.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

}
