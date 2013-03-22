package cn.eric.rss;

import java.io.File;
import java.io.FilenameFilter;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.eric.rss.provider.FeedData;
import cn.eric.rss.provider.OPML;
import cn.eric.rss.service.RefreshService;
import cn.eric.rss.ui.MenuData;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

/**
 * the launcher and overview activity
 * 
 * @author Ruobin Wang
 * 
 */
public class MainActivity extends SherlockActivityBase implements
		OnItemClickListener, OnCreateContextMenuListener {

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
		INSTANCE = this;
		if (getPreferences(MODE_PRIVATE).getBoolean(
				Strings.PREFERENCE_LICENSEACCEPTED, false)) {
		} else {
			showDialog(DIALOG_LICENSEAGREEMENT);
		}

		if (notificationManager == null) {
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		FeedConfigActivity.insertInitialFeeds(this);

		listView = (ListView) this.findViewById(R.id.list);
		listAdapter = new RSSOverviewListAdapter(this);

		emptyView = this.findViewById(android.R.id.empty);
		showEmptyView();
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);

		listView.setOnCreateContextMenuListener(this);

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				Strings.SETTINGS_REFRESHENABLED, true)) {
			startService(new Intent(this, RefreshService.class));
		} else {
			stopService(new Intent(this, RefreshService.class));
		}

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				Strings.SETTINGS_REFRESHONPENENABLED, true)) {
			new Thread() {
				public void run() {
					sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
				}
			}.start();
		}
	}

	/**
	 * decide whether to show the empty view
	 */
	private void showEmptyView() {
		if (Looper.getMainLooper() == Looper.myLooper()) {
			if ((listAdapter == null) || listAdapter.isEmpty()) {
				emptyView.setVisibility(View.VISIBLE);
			} else {
				emptyView.setVisibility(View.GONE);
			}
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if ((listAdapter == null) || listAdapter.isEmpty()) {
						emptyView.setVisibility(View.VISIBLE);
					} else {
						emptyView.setVisibility(View.GONE);
					}
				}
			});
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (MainActivity.notificationManager != null) {
			notificationManager.cancel(0);
		}
	}

	@Override
	protected void onInitUpbar(ActionBar actionBar) {
		super.onInitUpbar(actionBar);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setTitle(R.string.rss_feeds);
	}

	void setupLicenseText(AlertDialog.Builder builder) {
		View view = getLayoutInflater().inflate(R.layout.license, null);

		final TextView textView = (TextView) view
				.findViewById(R.id.license_text);

		textView.setTextColor(textView.getTextColors().getDefaultColor()); // disables
																			// color
																			// change
																			// on
																			// selection
		textView.setText(new StringBuilder(getString(R.string.license_intro))
				.append(Strings.THREENEWLINES).append(
						getString(R.string.license)));

		final TextView contributorsTextView = (TextView) view
				.findViewById(R.id.contributors_togglebutton);

		contributorsTextView.setOnClickListener(new OnClickListener() {
			boolean showingLicense = true;

			@Override
			public void onClick(View view) {
				if (showingLicense) {
					textView.setText(R.string.contributors_list);
					contributorsTextView.setText(R.string.license_word);
				} else {
					textView.setText(new StringBuilder(
							getString(R.string.license_intro)).append(
							Strings.THREENEWLINES).append(
							getString(R.string.license)));
					contributorsTextView.setText(R.string.contributors);
				}
				showingLicense = !showingLicense;
			}

		});
		builder.setView(view);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		SubMenu subMenu = menu.addSubMenu("").setIcon(R.drawable.ic_more);
		subMenu.add(0, MenuData.MENUITEM_ADD_FEED, 0, R.string.menu_addfeed);
		subMenu.add(0, MenuData.MENUITEM_REFRESH, 0, R.string.menu_refresh);
		subMenu.add(0, MenuData.MENUITEM_SETTINGS, 0, R.string.menu_settings);
		subMenu.add(0, MenuData.MENUITEM_MARK_ALL_AS_READ, 0,
				R.string.menu_allread);
		subMenu.add(0, MenuData.MENUITEM_ABOUT, 0, R.string.menu_about);
		subMenu.add(0, MenuData.MENUITEM_IMPORT_FROM_OPML, 0,
				R.string.menu_import);
		subMenu.add(0, MenuData.MENUITEM_EXPORT_TO_OPML, 0,
				R.string.menu_export);

		subMenu.getItem().setShowAsAction(
				MenuItem.SHOW_AS_ACTION_ALWAYS
						| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// menu.setGroupVisible(R.id.menu_group_0, !feedSort);
		// menu.setGroupVisible(R.id.menu_group_1, feedSort);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case MenuData.MENUITEM_ADD_FEED:

			startActivity(new Intent(Intent.ACTION_INSERT)
					.setData(FeedData.FeedColumns.CONTENT_URI));
			return true;

		case MenuData.MENUITEM_REFRESH:
			new Thread() {
				public void run() {
					sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS)
							.putExtra(
									Strings.SETTINGS_OVERRIDEWIFIONLY,
									PreferenceManager
											.getDefaultSharedPreferences(
													MainActivity.this)
											.getBoolean(
													Strings.SETTINGS_OVERRIDEWIFIONLY,
													false)));
				}
			}.start();
			return true;

		case MenuData.MENUITEM_SETTINGS: {
			startActivityForResult(new Intent(this,
					ApplicationPreferencesActivity.class),
					ACTIVITY_APPLICATIONPREFERENCES_ID);
			break;
		}
		case MenuData.MENUITEM_MARK_ALL_AS_READ: {
			new Thread() {
				public void run() {
					if (getContentResolver()
							.update(FeedData.EntryColumns.CONTENT_URI,
									getReadContentValues(),
									new StringBuilder(
											FeedData.EntryColumns.READDATE)
											.append(Strings.DB_ISNULL)
											.toString(), null) > 0) {
						getContentResolver().notifyChange(
								FeedData.FeedColumns.CONTENT_URI, null);
					}
				}
			}.start();
			break;
		}
		case MenuData.MENUITEM_ABOUT: {
			showDialog(DIALOG_ABOUT);
			break;
		}
		case MenuData.MENUITEM_IMPORT_FROM_OPML: {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)
					|| Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED_READ_ONLY)) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						this);

				builder.setTitle(R.string.select_file);

				try {
					final String[] fileNames = Environment
							.getExternalStorageDirectory().list(
									new FilenameFilter() {
										public boolean accept(File dir,
												String filename) {
											return new File(dir, filename)
													.isFile();
										}
									});
					builder.setItems(fileNames,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									try {
										OPML.importFromFile(
												new StringBuilder(
														Environment
																.getExternalStorageDirectory()
																.toString())
														.append(File.separator)
														.append(fileNames[which])
														.toString(),
												MainActivity.this);
									} catch (Exception e) {
										showDialog(DIALOG_ERROR_FEEDIMPORT);
									}
								}
							});
					builder.show();
				} catch (Exception e) {
					showDialog(DIALOG_ERROR_FEEDIMPORT);
				}
			} else {
				showDialog(DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
			}

			break;
		}
		case MenuData.MENUITEM_EXPORT_TO_OPML: {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)
					|| Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED_READ_ONLY)) {
				try {
					String filename = new StringBuilder(Environment
							.getExternalStorageDirectory().toString())
							.append("/sparse_rss_")
							.append(System.currentTimeMillis()).append(".opml")
							.toString();

					OPML.exportToFile(filename, this);
					Toast.makeText(
							this,
							String.format(
									getString(R.string.message_exportedto),
									filename), Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					showDialog(DIALOG_ERROR_FEEDEXPORT);
				}
			} else {
				showDialog(DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
			}
			break;
		}

		}

		return true;

	}

	public static final ContentValues getReadContentValues() {
		ContentValues values = new ContentValues();

		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
		return values;
	}

	public static final ContentValues getUnreadContentValues() {
		ContentValues values = new ContentValues();

		values.putNull(FeedData.EntryColumns.READDATE);
		return values;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
		case DIALOG_ERROR_FEEDIMPORT: {
			dialog = createErrorDialog(R.string.error_feedimport);
			break;
		}
		case DIALOG_ERROR_FEEDEXPORT: {
			dialog = createErrorDialog(R.string.error_feedexport);
			break;
		}
		case DIALOG_ERROR_INVALIDIMPORTFILE: {
			dialog = createErrorDialog(R.string.error_invalidimportfile);
			break;
		}
		case DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE: {
			dialog = createErrorDialog(R.string.error_externalstoragenotavailable);
			break;
		}
		case DIALOG_ABOUT: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setIcon(android.R.drawable.ic_dialog_info);
			builder.setTitle(R.string.menu_about);
			MainActivity.INSTANCE.setupLicenseText(builder);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
			builder.setNeutralButton(R.string.changelog,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							startActivity(new Intent(Intent.ACTION_VIEW,
									CANGELOG_URI));
						}
					});
			return builder.create();
		}
		case DIALOG_LICENSEAGREEMENT: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string.dialog_licenseagreement);
			builder.setNegativeButton(R.string.button_decline,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							finish();
						}
					});
			builder.setPositiveButton(R.string.button_accept,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();

							Editor editor = getPreferences(MODE_PRIVATE).edit();

							editor.putBoolean(
									Strings.PREFERENCE_LICENSEACCEPTED, true);
							editor.commit();

							// setContent();
						}
					});
			setupLicenseText(builder);
			builder.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						dialog.cancel();
						finish();
					}
					return true;
				}
			});
			return builder.create();
		}
		default:
			dialog = null;
		}
		return dialog;
	}

	private Dialog createErrorDialog(int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(messageId);
		builder.setTitle(R.string.error);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton(android.R.string.ok, null);
		return builder.create();
	}

	static void showDeleteAllEntriesQuestion(final Context context,
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
										Strings.DB_EXCUDEFAVORITE);
								if (context.getContentResolver().delete(uri,
										Strings.DB_EXCUDEFAVORITE, null) > 0) {
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

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {

		Intent intent = new Intent(Intent.ACTION_VIEW,
				FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));

		intent.putExtra(FeedData.FeedColumns._ID, id);
		startActivity(intent);

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView
				.findViewById(android.R.id.text1)).getText());
		menu.add(0, CONTEXTMENU_REFRESH_ID, Menu.NONE,
				R.string.contextmenu_refresh);
		menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE,
				R.string.contextmenu_markasread);
		menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE,
				R.string.contextmenu_markasunread);
		menu.add(0, CONTEXTMENU_DELETEREAD_ID, Menu.NONE,
				R.string.contextmenu_deleteread);
		menu.add(0, CONTEXTMENU_DELETEALLENTRIES_ID, Menu.NONE,
				R.string.contextmenu_deleteallentries);
		menu.add(0, CONTEXTMENU_EDIT_ID, Menu.NONE, R.string.contextmenu_edit);
		menu.add(0, CONTEXTMENU_RESETUPDATEDATE_ID, Menu.NONE,
				R.string.contextmenu_resetupdatedate);
		menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE,
				R.string.contextmenu_delete);
		menu.add(0, CONTEXTMENU_SETTINGS_ID, Menu.NONE,
				R.string.contextmenu_settings);
	}

	@Override
	public boolean onContextItemSelected(final android.view.MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_addfeed: {
			startActivity(new Intent(Intent.ACTION_INSERT)
					.setData(FeedData.FeedColumns.CONTENT_URI));
			break;
		}
		case R.id.menu_refresh: {
			new Thread() {
				public void run() {
					sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS)
							.putExtra(
									Strings.SETTINGS_OVERRIDEWIFIONLY,
									PreferenceManager
											.getDefaultSharedPreferences(
													MainActivity.this)
											.getBoolean(
													Strings.SETTINGS_OVERRIDEWIFIONLY,
													false)));
				}
			}.start();
			break;
		}
		case CONTEXTMENU_EDIT_ID: {
			startActivity(new Intent(Intent.ACTION_EDIT)
					.setData(FeedData.FeedColumns
							.CONTENT_URI(((AdapterView.AdapterContextMenuInfo) item
									.getMenuInfo()).id)));
			break;
		}
		case CONTEXTMENU_REFRESH_ID: {
			final String id = Long
					.toString(((AdapterView.AdapterContextMenuInfo) item
							.getMenuInfo()).id);

			ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

			final NetworkInfo networkInfo = connectivityManager
					.getActiveNetworkInfo();

			if (networkInfo != null
					&& networkInfo.getState() == NetworkInfo.State.CONNECTED) { // since
																				// we
																				// have
																				// acquired
																				// the
																				// networkInfo,
																				// we
																				// use
																				// it
																				// for
																				// basic
																				// checks
				final Intent intent = new Intent(Strings.ACTION_REFRESHFEEDS)
						.putExtra(Strings.FEEDID, id);

				final Thread thread = new Thread() {
					public void run() {
						sendBroadcast(intent);
					}
				};

				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
						|| PreferenceManager.getDefaultSharedPreferences(
								MainActivity.this).getBoolean(
								Strings.SETTINGS_OVERRIDEWIFIONLY, false)) {
					intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
					thread.start();
				} else {
					Cursor cursor = getContentResolver().query(
							FeedData.FeedColumns.CONTENT_URI(id),
							new String[] { FeedData.FeedColumns.WIFIONLY },
							null, null, null);

					cursor.moveToFirst();

					if (cursor.isNull(0) || cursor.getInt(0) == 0) {
						thread.start();
					} else {
						Builder builder = new AlertDialog.Builder(this);

						builder.setIcon(android.R.drawable.ic_dialog_alert);
						builder.setTitle(R.string.dialog_hint);
						builder.setMessage(R.string.question_refreshwowifi);
						builder.setPositiveButton(android.R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										intent.putExtra(
												Strings.SETTINGS_OVERRIDEWIFIONLY,
												true);
										thread.start();
									}
								});
						builder.setNeutralButton(
								R.string.button_alwaysokforall,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										PreferenceManager
												.getDefaultSharedPreferences(
														MainActivity.this)
												.edit()
												.putBoolean(
														Strings.SETTINGS_OVERRIDEWIFIONLY,
														true).commit();
										intent.putExtra(
												Strings.SETTINGS_OVERRIDEWIFIONLY,
												true);
										thread.start();
									}
								});
						builder.setNegativeButton(android.R.string.no, null);
						builder.show();
					}
					cursor.close();
				}

			}
			break;
		}
		case CONTEXTMENU_DELETE_ID: {
			String id = Long
					.toString(((AdapterView.AdapterContextMenuInfo) item
							.getMenuInfo()).id);

			Cursor cursor = getContentResolver().query(
					FeedData.FeedColumns.CONTENT_URI(id),
					new String[] { FeedData.FeedColumns.NAME }, null, null,
					null);

			cursor.moveToFirst();

			Builder builder = new AlertDialog.Builder(this);

			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(cursor.getString(0));
			builder.setMessage(R.string.question_deletefeed);
			builder.setPositiveButton(android.R.string.yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							new Thread() {
								public void run() {
									getContentResolver()
											.delete(FeedData.FeedColumns
													.CONTENT_URI(Long
															.toString(((AdapterView.AdapterContextMenuInfo) item
																	.getMenuInfo()).id)),
													null, null);
									sendBroadcast(new Intent(
											Strings.ACTION_UPDATEWIDGET));

									showEmptyView();
								}
							}.start();
						}
					});
			builder.setNegativeButton(android.R.string.no, null);
			cursor.close();
			builder.show();
			break;
		}
		case CONTEXTMENU_MARKASREAD_ID: {
			new Thread() {
				public void run() {
					String id = Long
							.toString(((AdapterView.AdapterContextMenuInfo) item
									.getMenuInfo()).id);

					if (getContentResolver()
							.update(FeedData.EntryColumns.CONTENT_URI(id),
									getReadContentValues(),
									new StringBuilder(
											FeedData.EntryColumns.READDATE)
											.append(Strings.DB_ISNULL)
											.toString(), null) > 0) {
						getContentResolver().notifyChange(
								FeedData.FeedColumns.CONTENT_URI(id), null);
					}
				}
			}.start();
			break;
		}
		case CONTEXTMENU_MARKASUNREAD_ID: {
			new Thread() {
				public void run() {
					String id = Long
							.toString(((AdapterView.AdapterContextMenuInfo) item
									.getMenuInfo()).id);

					if (getContentResolver().update(
							FeedData.EntryColumns.CONTENT_URI(id),
							getUnreadContentValues(), null, null) > 0) {
						getContentResolver().notifyChange(
								FeedData.FeedColumns.CONTENT_URI(id), null);
						;
					}
				}
			}.start();
			break;
		}
		case CONTEXTMENU_SETTINGS_ID: {
			startActivity(new Intent(this, FeedPrefsActivity.class).putExtra(
					FeedData.FeedColumns._ID,
					Long.toString(((AdapterView.AdapterContextMenuInfo) item
							.getMenuInfo()).id)));
			break;
		}
		case CONTEXTMENU_DELETEREAD_ID: {
			new Thread() {
				public void run() {
					String id = Long
							.toString(((AdapterView.AdapterContextMenuInfo) item
									.getMenuInfo()).id);

					Uri uri = FeedData.EntryColumns.CONTENT_URI(id);

					String selection = Strings.READDATE_GREATERZERO
							+ Strings.DB_AND + " (" + Strings.DB_EXCUDEFAVORITE
							+ ")";

					FeedData.deletePicturesOfFeed(MainActivity.this, uri,
							selection);
					if (getContentResolver().delete(uri, selection, null) > 0) {
						getContentResolver().notifyChange(
								FeedData.FeedColumns.CONTENT_URI(id), null);
					}
				}
			}.start();
			break;
		}
		case CONTEXTMENU_DELETEALLENTRIES_ID: {
			showDeleteAllEntriesQuestion(
					this,
					FeedData.EntryColumns.CONTENT_URI(Long
							.toString(((AdapterView.AdapterContextMenuInfo) item
									.getMenuInfo()).id)));
			break;
		}
		case CONTEXTMENU_RESETUPDATEDATE_ID: {
			ContentValues values = new ContentValues();

			values.put(FeedData.FeedColumns.LASTUPDATE, 0);
			values.put(FeedData.FeedColumns.REALLASTUPDATE, 0);
			getContentResolver()
					.update(FeedData.FeedColumns.CONTENT_URI(Long
							.toString(((AdapterView.AdapterContextMenuInfo) item
									.getMenuInfo()).id)), values, null, null);
			break;
		}

		case R.id.menu_deleteread: {
			FeedData.deletePicturesOfFeedAsync(this,
					FeedData.EntryColumns.CONTENT_URI,
					Strings.READDATE_GREATERZERO);
			getContentResolver().delete(FeedData.EntryColumns.CONTENT_URI,
					Strings.READDATE_GREATERZERO, null);
			listAdapter.notifyDataSetChanged();

			break;
		}
		case R.id.menu_deleteallentries: {
			showDeleteAllEntriesQuestion(this,
					FeedData.EntryColumns.CONTENT_URI);
			break;
		}
		}
		return true;
	}

	@Override
	protected int getLayoutRes() {
		return R.layout.main;
	}
}
