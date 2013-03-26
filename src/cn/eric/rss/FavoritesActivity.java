package cn.eric.rss;

import cn.eric.rss.service.RefreshService;
import cn.eric.rss.utility.ApplicationHelper;
import cn.eric.rss.utility.MyStrings;

import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class FavoritesActivity extends SherlockActivityBase implements
		OnItemClickListener {

	public static MainActivity INSTANCE;

	public static final boolean POSTGINGERBREAD = !Build.VERSION.RELEASE
			.startsWith("1") && !Build.VERSION.RELEASE.startsWith("2");

	private static final int DIALOG_LICENSEAGREEMENT = 0; // save

	private static final int DIALOG_ERROR_FEEDIMPORT = 3;

	private static final int DIALOG_ERROR_FEEDEXPORT = 4;

	private static final int DIALOG_ERROR_INVALIDIMPORTFILE = 5;

	private static final int DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE = 6;

	private static final int DIALOG_ABOUT = 7;

	private static final int CONTEXTMENU_EDIT_ID = 3;

	private static final int CONTEXTMENU_REFRESH_ID = 4;

	private static final int CONTEXTMENU_DELETE_ID = 5;

	private static final int CONTEXTMENU_MARKASREAD_ID = 6;

	private static final int CONTEXTMENU_MARKASUNREAD_ID = 7;

	private static final int CONTEXTMENU_DELETEREAD_ID = 8;

	private static final int CONTEXTMENU_DELETEALLENTRIES_ID = 9;

	private static final int CONTEXTMENU_RESETUPDATEDATE_ID = 10;

	private static final int ACTIVITY_APPLICATIONPREFERENCES_ID = 1;

	private static final Uri CANGELOG_URI = Uri
			.parse("http://code.google.com/p/MiniRSS/wiki/Changelog");

	private static final int CONTEXTMENU_SETTINGS_ID = 99;

	static NotificationManager notificationManager;

	private RSSOverviewListAdapter listAdapter;
	private ListView listView;

	private View emptyView;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		MobclickAgent.updateOnlineConfig(this);
		UmengUpdateAgent.update(this);
		UmengUpdateAgent.setUpdateOnlyWifi(false);
		if (getPreferences(MODE_PRIVATE).getBoolean(
				MyStrings.PREFERENCE_LICENSEACCEPTED, false)) {
		} else {
			showDialog(DIALOG_LICENSEAGREEMENT);
		}

		if (notificationManager == null) {
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		listView = (ListView) this.findViewById(R.id.list);
		listAdapter = new RSSOverviewListAdapter(this);

		emptyView = this.findViewById(android.R.id.empty);
		ApplicationHelper.showEmptyView(this, listAdapter, emptyView);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);

		listView.setOnCreateContextMenuListener(this);

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				MyStrings.SETTINGS_REFRESHENABLED, true)) {
			startService(new Intent(this, RefreshService.class));
		} else {
			stopService(new Intent(this, RefreshService.class));
		}

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				MyStrings.SETTINGS_REFRESHONPENENABLED, true)) {
			new Thread() {
				public void run() {
					sendBroadcast(new Intent(MyStrings.ACTION_REFRESHFEEDS));
				}
			}.start();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub

	}

	@Override
	protected int getLayoutRes() {
		// TODO Auto-generated method stub
		return 0;
	}

}