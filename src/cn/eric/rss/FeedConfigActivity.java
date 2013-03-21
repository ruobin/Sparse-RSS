/**
 * Sparse rss
 *
 * Copyright (c) 2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package cn.eric.rss;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import cn.eric.rss.provider.FeedData;

public class FeedConfigActivity extends Activity {
	private static final String WASACTIVE = "wasactive";

	private static final String[] PROJECTION = new String[] {
			FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL,
			FeedData.FeedColumns.WIFIONLY };

	private EditText nameEditText;

	private EditText urlEditText;

	private CheckBox refreshOnlyWifiCheckBox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		this.setContentView(R.layout.feedsettings);
		setResult(RESULT_CANCELED);

		Intent intent = getIntent();

		nameEditText = (EditText) findViewById(R.id.feed_title);
		urlEditText = (EditText) findViewById(R.id.feed_url);
		refreshOnlyWifiCheckBox = (CheckBox) findViewById(R.id.wifionlycheckbox);		
		
		//add feed dialog
		if (intent.getAction().equals(Intent.ACTION_INSERT)) {
			
			restoreInstanceState(savedInstanceState);
			((Button) findViewById(R.id.button_ok))
					.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							String url = urlEditText.getText().toString();

							if (!url.startsWith(Strings.HTTP)
									&& !url.startsWith(Strings.HTTPS)) {
								url = Strings.HTTP + url;
							}

							Cursor cursor = getContentResolver().query(
									FeedData.FeedColumns.CONTENT_URI,
									null,
									new StringBuilder(FeedData.FeedColumns.URL)
											.append(Strings.DB_ARG).toString(),
									new String[] { url }, null);

							if (cursor.moveToFirst()) {
								cursor.close();
								Toast.makeText(FeedConfigActivity.this,
										R.string.error_feedurlexists,
										Toast.LENGTH_LONG).show();
							} else {
								cursor.close();
								ContentValues values = new ContentValues();

								values.put(FeedData.FeedColumns.WIFIONLY,
										refreshOnlyWifiCheckBox.isChecked() ? 1
												: 0);
								values.put(FeedData.FeedColumns.URL, url);
								values.put(FeedData.FeedColumns.ERROR,
										(String) null);

								String name = nameEditText.getText().toString();

								if (name.trim().length() > 0) {
									values.put(FeedData.FeedColumns.NAME, name);
								}
								getContentResolver().insert(
										FeedData.FeedColumns.CONTENT_URI,
										values);
								setResult(RESULT_OK);
								finish();
							}
						}
					});
		} else {
			//edit feed dialog
			if (!restoreInstanceState(savedInstanceState)) {
				Cursor cursor = getContentResolver().query(intent.getData(),
						PROJECTION, null, null, null);

				if (cursor.moveToNext()) {
					nameEditText.setText(cursor.getString(0));
					urlEditText.setText(cursor.getString(1));
					refreshOnlyWifiCheckBox.setChecked(cursor.getInt(2) == 1);
					cursor.close();
				} else {
					cursor.close();
					Toast.makeText(FeedConfigActivity.this, R.string.error,
							Toast.LENGTH_LONG).show();
					finish();
				}
			}
			((Button) findViewById(R.id.button_ok))
					.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							String url = urlEditText.getText().toString();

							Cursor cursor = getContentResolver().query(
									FeedData.FeedColumns.CONTENT_URI,
									new String[] { FeedData.FeedColumns._ID },
									new StringBuilder(FeedData.FeedColumns.URL)
											.append(Strings.DB_ARG).toString(),
									new String[] { url }, null);

							if (cursor.moveToFirst()
									&& !getIntent().getData()
											.getLastPathSegment()
											.equals(cursor.getString(0))) {
								cursor.close();
								Toast.makeText(FeedConfigActivity.this,
										R.string.error_feedurlexists,
										Toast.LENGTH_LONG).show();
							} else {
								cursor.close();
								ContentValues values = new ContentValues();

								if (!url.startsWith(Strings.HTTP)
										&& !url.startsWith(Strings.HTTPS)) {
									url = Strings.HTTP + url;
								}
								values.put(FeedData.FeedColumns.URL, url);

								String name = nameEditText.getText().toString();

								values.put(FeedData.FeedColumns.NAME, name
										.trim().length() > 0 ? name : null);
								values.put(FeedData.FeedColumns.FETCHMODE, 0);
								values.put(FeedData.FeedColumns.WIFIONLY,
										refreshOnlyWifiCheckBox.isChecked() ? 1
												: 0);
								values.put(FeedData.FeedColumns.ERROR,
										(String) null);
								getContentResolver().update(
										getIntent().getData(), values, null,
										null);

								setResult(RESULT_OK);
								finish();
							}
						}

					});

		}

		((Button) findViewById(R.id.button_cancel))
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						finish();
					}
				});
	}

	private boolean restoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null
				&& savedInstanceState.getBoolean(WASACTIVE, false)) {
			nameEditText.setText(savedInstanceState
					.getCharSequence(FeedData.FeedColumns.NAME));
			urlEditText.setText(savedInstanceState
					.getCharSequence(FeedData.FeedColumns.URL));
			refreshOnlyWifiCheckBox.setChecked(savedInstanceState
					.getBoolean(FeedData.FeedColumns.WIFIONLY));
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(WASACTIVE, true);
		outState.putCharSequence(FeedData.FeedColumns.NAME,
				nameEditText.getText());
		outState.putCharSequence(FeedData.FeedColumns.URL,
				urlEditText.getText());
		outState.putBoolean(FeedData.FeedColumns.WIFIONLY,
				refreshOnlyWifiCheckBox.isChecked());
	}

	/**
	 * fill in some common feed urls for users
	 * 
	 * @param activity
	 * @return
	 */
	public static void insertInitialFeeds(Activity activity) {
		Cursor cursor = activity.getContentResolver().query(
				FeedData.FeedColumns.CONTENT_URI, null, null, null, null);

		if (cursor.moveToFirst()) {
			// the feed list isn't empty, do nothing
			return;
		}
		String url, name;
		url = "http://news.163.com/special/00011K6L/rss_newstop.xml";
		name = "网易新闻（头条）";
		insertFeed(activity, url, name);
		url = "http://news.163.com/special/00011K6L/rss_hotnews.xml";
		name = "网易新闻（深度）";
		insertFeed(activity, url, name);
		url = "http://news.qq.com/newssh/rss_newssh.xml";
		name = "腾讯社会新闻";
		insertFeed(activity, url, name);
		url = "http://comic.qq.com/news/rss_news.xml";
		name = "腾讯动漫频道";
		insertFeed(activity, url, name);
		url = "http://feed.mtime.com/movienews.rss";
		name = "时光电影新闻";
		insertFeed(activity, url, name);
		url = "http://feed.williamlong.info/";
		name = "月光博客";
		insertFeed(activity, url, name);
		url = "http://online.wsj.com/xml/rss/3_7085.xml";
		name = "华尔街日报（世界新闻，英文）";
		insertFeed(activity, url, name);
	}

	private static boolean insertFeed(Activity activity, String url, String name) {
		ContentValues values = new ContentValues();

		values.put(FeedData.FeedColumns.URL, url);
		values.put(FeedData.FeedColumns.NAME, name);
		values.put(FeedData.FeedColumns.FETCHMODE, 0);
		values.put(FeedData.FeedColumns.WIFIONLY, 0);
		values.put(FeedData.FeedColumns.ERROR, (String) null);
		try {
			activity.getContentResolver().insert(
					FeedData.FeedColumns.CONTENT_URI, values);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}
}
