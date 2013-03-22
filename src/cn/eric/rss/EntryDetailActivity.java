/**
 * Sparse rss
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import cn.eric.rss.provider.FeedData;
import cn.eric.rss.ui.MyAnimations;
import cn.eric.rss.ui.MenuData;
import cn.eric.rss.utility.MyStrings;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.adsmogo.adview.AdsMogoLayout;
import com.umeng.analytics.MobclickAgent;

public class EntryDetailActivity extends SherlockActivityBase {

	private static final String TEXT_HTML = "text/html";

	private static final String UTF8 = "utf-8";

	private static final String OR_DATE = " or date ";

	private static final String DATE = "(date=";

	private static final String AND_ID = " and _id";

	private static final String ASC = "date asc, _id desc limit 1";

	private static final String DESC = "date desc, _id asc limit 1";

	private static final String CSS = "<head><style type=\"text/css\">body {max-width: 100%}\nimg {max-width: 100%; height: auto;}\ndiv[style] {max-width: 100%;}\npre {white-space: pre-wrap;}</style></head>";

	private static final String FONT_START = CSS
			+ "<body link=\"#97ACE5\" text=\"#C0C0C0\">";

	private static final String FONT_FONTSIZE_START = CSS
			+ "<body link=\"#97ACE5\" text=\"#C0C0C0\"><font size=\"+";

	private static final String FONTSIZE_START = "<font size=\"+";

	private static final String FONTSIZE_MIDDLE = "\">";

	private static final String FONTSIZE_END = "</font>";

	private static final String FONT_END = "</font><br/><br/><br/><br/></body>";

	private static final String BODY_START = "<body>";

	private static final String BODY_END = "<br/><br/><br/><br/></body>";

	private static final int BUTTON_ALPHA = 180;

	private static final String IMAGE_ENCLOSURE = "[@]image/";

	private static final String TEXTPLAIN = "text/plain";

	private static final String BRACKET = " (";

	private int titlePosition, datePosition, abstractPosition, linkPosition,
			feedIdPosition, favoritePosition, readDatePosition,
			enclosurePosition, authorPosition;

	private String _id, _nextId, _previousId;

	private Uri uri, parentUri;

	private int feedId;

	boolean favorite;

	private boolean showRead, canShowIcon;

	private byte[] iconBytes;

	private WebView webView, webView0; // only needed for the animation

	private ViewFlipper viewFlipper;

	private ImageButton nextButton, urlButton, previousButton, playButton;

	int scrollX, scrollY;

	private String link;

	private LayoutParams layoutParams;

	private View content;

	private SharedPreferences preferences;

	private boolean localPictures;

	private TextView titleTextView;

	private int backgroundColor;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		uri = getIntent().getData();
		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
		showRead = getIntent().getBooleanExtra(
				EntriesListActivity.EXTRA_SHOWREAD, true);
		iconBytes = getIntent().getByteArrayExtra(FeedData.FeedColumns.ICON);
		feedId = 0;

		Cursor entryCursor = getContentResolver().query(uri, null, null, null,
				null);

		titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		abstractPosition = entryCursor
				.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		feedIdPosition = entryCursor
				.getColumnIndex(FeedData.EntryColumns.FEED_ID);
		favoritePosition = entryCursor
				.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		readDatePosition = entryCursor
				.getColumnIndex(FeedData.EntryColumns.READDATE);
		enclosurePosition = entryCursor
				.getColumnIndex(FeedData.EntryColumns.ENCLOSURE);
		authorPosition = entryCursor
				.getColumnIndex(FeedData.EntryColumns.AUTHOR);

		entryCursor.close();
		if (MainActivity.notificationManager == null) {
			MainActivity.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		nextButton = (ImageButton) findViewById(R.id.next_button);
		urlButton = (ImageButton) findViewById(R.id.url_button);
		urlButton.setAlpha(BUTTON_ALPHA + 30);
		previousButton = (ImageButton) findViewById(R.id.prev_button);
		playButton = (ImageButton) findViewById(R.id.play_button);
		playButton.setAlpha(BUTTON_ALPHA);

		viewFlipper = (ViewFlipper) findViewById(R.id.content_flipper);

		layoutParams = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);

		webView = new WebView(this);
		backgroundColor = this.getResources().getColor(R.color.common_bg_gray);
		viewFlipper.addView(webView, layoutParams);

		OnKeyListener onKeyEventListener = new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == 92 || keyCode == 94) {
						scrollUp();
						return true;
					} else if (keyCode == 93 || keyCode == 95) {
						scrollDown();
						return true;
					}
				}
				return false;
			}
		};
		webView.setOnKeyListener(onKeyEventListener);

		content = findViewById(R.id.entry_content);

		webView0 = new WebView(this);
		webView0.setOnKeyListener(onKeyEventListener);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		final boolean gestures = preferences.getBoolean(
				MyStrings.SETTINGS_GESTURESENABLED, true);

		final GestureDetector gestureDetector = new GestureDetector(this,
				new OnGestureListener() {
					public boolean onDown(MotionEvent e) {
						return false;
					}

					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						if (gestures) {
							if (Math.abs(velocityY) < Math.abs(velocityX)) {
								if (velocityX > 800) {
									if (previousButton.isEnabled()) {
										previousEntry(true);
									}
								} else if (velocityX < -800) {
									if (nextButton.isEnabled()) {
										nextEntry(true);
									}
								}
							}
						}
						return false;
					}

					public void onLongPress(MotionEvent e) {

					}

					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						return false;
					}

					public void onShowPress(MotionEvent e) {

					}

					public boolean onSingleTapUp(MotionEvent e) {
						return false;
					}
				});

		OnTouchListener onTouchListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};

		webView.setOnTouchListener(onTouchListener);

		content.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return true; // different to the above one!
			}
		});

		webView0.setOnTouchListener(onTouchListener);

		scrollX = 0;
		scrollY = 0;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		webView.restoreState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		if (MainActivity.notificationManager != null) {
			MainActivity.notificationManager.cancel(0);
		}
		uri = getIntent().getData();
		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
		if (MainActivity.POSTGINGERBREAD) {
			CompatibilityHelper.onResume(webView);
		}
		reload();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private void reload() {
		if (_id != null && _id.equals(uri.getLastPathSegment())) {
			return;
		}

		_id = uri.getLastPathSegment();

		ContentValues values = new ContentValues();

		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());

		Cursor entryCursor = getContentResolver().query(uri, null, null, null,
				null);

		if (entryCursor.moveToFirst()) {
			String abstractText = entryCursor.getString(abstractPosition);

			if (entryCursor.isNull(readDatePosition)) {
				getContentResolver().update(
						uri,
						values,
						new StringBuilder(FeedData.EntryColumns.READDATE)
								.append(MyStrings.DB_ISNULL).toString(), null);
			}
			if (abstractText == null) {
				String link = entryCursor.getString(linkPosition);

				entryCursor.close();
				finish();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
			} else {
				updateUpbarTitle(entryCursor.getString(titlePosition));
				if (titleTextView != null) {
					titleTextView.requestFocus(); // restart ellipsize
				}

				int _feedId = entryCursor.getInt(feedIdPosition);

				if (feedId != _feedId) {
					if (feedId != 0) {
						iconBytes = null; // triggers re-fetch of the icon
					}
					feedId = _feedId;
				}

				if (canShowIcon) {
					if (iconBytes == null || iconBytes.length == 0) {
						Cursor iconCursor = getContentResolver().query(
								FeedData.FeedColumns.CONTENT_URI(Integer
										.toString(feedId)),
								new String[] { FeedData.FeedColumns._ID,
										FeedData.FeedColumns.ICON }, null,
								null, null);

						if (iconCursor.moveToFirst()) {
							iconBytes = iconCursor.getBlob(1);
						}
						iconCursor.close();
					}

					// if (iconBytes != null && iconBytes.length > 0) {
					// int bitmapSizeInDip = (int) TypedValue.applyDimension(
					// TypedValue.COMPLEX_UNIT_DIP, 24f,
					// getResources().getDisplayMetrics());
					// Bitmap bitmap = BitmapFactory.decodeByteArray(
					// iconBytes, 0, iconBytes.length);
					// if (bitmap != null) {
					// if (bitmap.getHeight() != bitmapSizeInDip) {
					// bitmap = Bitmap
					// .createScaledBitmap(bitmap,
					// bitmapSizeInDip,
					// bitmapSizeInDip, false);
					// }
					//
					// if (MainActivity.POSTGINGERBREAD) {
					// CompatibilityHelper.setActionBarDrawable(this,
					// new BitmapDrawable(bitmap));
					// } else {
					// setFeatureDrawable(Window.FEATURE_LEFT_ICON,
					// new BitmapDrawable(bitmap));
					// }
					// }
					// }
				}

				long timestamp = entryCursor.getLong(datePosition);

				Date date = new Date(timestamp);

				StringBuilder dateStringBuilder = new StringBuilder(DateFormat
						.getDateFormat(this).format(date)).append(' ').append(
						DateFormat.getTimeFormat(this).format(date));

				String author = entryCursor.getString(authorPosition);

				if (author != null) {
					dateStringBuilder.append(BRACKET).append(author)
							.append(')');
				}

				((TextView) findViewById(R.id.entry_date))
						.setText(dateStringBuilder);

				final ImageView imageView = (ImageView) findViewById(android.R.id.icon);

				favorite = entryCursor.getInt(favoritePosition) == 1;

				imageView.setBackgroundResource(favorite ? R.drawable.favorite
						: R.drawable.not_favorite);
				imageView.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						favorite = !favorite;
						imageView
								.setBackgroundResource(favorite ? R.drawable.favorite
										: R.drawable.not_favorite);
						ContentValues values = new ContentValues();

						values.put(FeedData.EntryColumns.FAVORITE, favorite ? 1
								: 0);
						getContentResolver().update(uri, values, null, null);
					}
				});
				// loadData does not recognize the encoding without correct
				// html-header
				localPictures = abstractText
						.indexOf(MyStrings.IMAGEID_REPLACEMENT) > -1;

				abstractText = abstractText.replace(
						MyStrings.IMAGEID_REPLACEMENT, uri.getLastPathSegment()
								+ MyStrings.IMAGEFILE_IDSEPARATOR);

				Pattern linkP = Pattern.compile("<a[^>]*href=[^>]*>");
				Matcher linkM = linkP.matcher(abstractText);
				if (!linkM.find()) {
					abstractText = abstractText.replaceAll(
							"(?i)(https?://[^ \n\r\t\\[\\]]+)",
							"<a href=\"$1\">$1</a>");
				}

				Pattern brP = Pattern.compile("<br[^>]*>");
				Matcher brM = brP.matcher(abstractText);
				if (!brM.find()) {
					abstractText = abstractText.replaceAll("\n", "<br>");
				}

				abstractText = abstractText.replaceAll("(?i)\\[(/?(b|u))\\]",
						"<$1>");
				abstractText = abstractText.replaceAll(
						"(?i)\\[img\\](https?://[^ \n\r\t\\[\\]]+)\\[/img\\]",
						"<img src='$1'>");
				abstractText = abstractText.replaceAll(
						"(?i)\\[/?(center|color|size|img|url|pre)[^\\]]*\\]",
						"");

				final SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(this);

				if (localPictures) {
					abstractText = abstractText.replace(
							MyStrings.IMAGEID_REPLACEMENT, _id
									+ MyStrings.IMAGEFILE_IDSEPARATOR);
				}

				if (preferences.getBoolean(MyStrings.SETTINGS_DISABLEPICTURES,
						false)) {
					abstractText = abstractText.replaceAll(
							MyStrings.HTML_IMG_REGEX, MyStrings.EMPTY);
					webView.getSettings().setBlockNetworkImage(true);
				} else {
					if (webView.getSettings().getBlockNetworkImage()) {
						/*
						 * setBlockNetwortImage(false) calls postSync, which
						 * takes time, so we clean up the html first and change
						 * the value afterwards
						 */
						webView.loadData(MyStrings.EMPTY, TEXT_HTML, UTF8);
						webView.getSettings().setBlockNetworkImage(false);
					}
				}

				int fontsize = Integer.parseInt(preferences.getString(
						MyStrings.SETTINGS_FONTSIZE, MyStrings.ONE));

				if (fontsize > 0) {
					webView.loadDataWithBaseURL(null,
							new StringBuilder(CSS).append(FONTSIZE_START)
									.append(fontsize).append(FONTSIZE_MIDDLE)
									.append(abstractText).append(FONTSIZE_END)
									.toString(), TEXT_HTML, UTF8, null);
				} else {
					webView.loadDataWithBaseURL(
							null,
							new StringBuilder(CSS).append(BODY_START)
									.append(abstractText).append(BODY_END)
									.toString(), TEXT_HTML, UTF8, null);
				}
				webView.setBackgroundColor(backgroundColor);
				content.setBackgroundColor(Color.WHITE);
				link = entryCursor.getString(linkPosition);

				if (link != null && link.length() > 0) {
					urlButton.setEnabled(true);
					urlButton.setAlpha(BUTTON_ALPHA + 20);
					urlButton.setOnClickListener(new OnClickListener() {
						public void onClick(View view) {
							startActivityForResult(new Intent(
									Intent.ACTION_VIEW, Uri.parse(link)), 0);
						}
					});
				} else {
					urlButton.setEnabled(false);
					urlButton.setAlpha(80);
				}

				final String enclosure = entryCursor
						.getString(enclosurePosition);

				if (enclosure != null && enclosure.length() > 6
						&& enclosure.indexOf(IMAGE_ENCLOSURE) == -1) {
					playButton.setVisibility(View.VISIBLE);
					playButton.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							final int position1 = enclosure
									.indexOf(MyStrings.ENCLOSURE_SEPARATOR);

							final int position2 = enclosure.indexOf(
									MyStrings.ENCLOSURE_SEPARATOR, position1 + 3);

							final Uri uri = Uri.parse(enclosure.substring(0,
									position1));

							if (preferences.getBoolean(
									MyStrings.SETTINGS_ENCLOSUREWARNINGSENABLED,
									true)) {
								Builder builder = new AlertDialog.Builder(
										EntryDetailActivity.this);

								builder.setTitle(R.string.question_areyousure);
								builder.setIcon(android.R.drawable.ic_dialog_alert);
								if (position2 + 4 > enclosure.length()) {
									builder.setMessage(getString(
											R.string.question_playenclosure,
											uri,
											position2 + 4 > enclosure.length() ? MyStrings.QUESTIONMARKS
													: enclosure
															.substring(position2 + 3)));
								} else {
									try {
										builder.setMessage(getString(
												R.string.question_playenclosure,
												uri,
												(Integer.parseInt(enclosure
														.substring(position2 + 3)) / 1024f)
														+ getString(R.string.kb)));
									} catch (Exception e) {
										builder.setMessage(getString(
												R.string.question_playenclosure,
												uri,
												enclosure
														.substring(position2 + 3)));
									}
								}
								builder.setCancelable(true);
								builder.setPositiveButton(android.R.string.ok,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												showEnclosure(uri, enclosure,
														position1, position2);
											}
										});
								builder.setNeutralButton(
										R.string.button_alwaysokforall,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												preferences
														.edit()
														.putBoolean(
																MyStrings.SETTINGS_ENCLOSUREWARNINGSENABLED,
																false).commit();
												showEnclosure(uri, enclosure,
														position1, position2);
											}
										});
								builder.setNegativeButton(
										android.R.string.cancel,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												dialog.dismiss();
											}
										});
								builder.show();
							} else {
								showEnclosure(uri, enclosure, position1,
										position2);
							}
						}
					});
				} else {
					playButton.setVisibility(View.GONE);
				}
				entryCursor.close();
				setupButton(previousButton, false, timestamp);
				setupButton(nextButton, true, timestamp);
				webView.scrollTo(scrollX, scrollY); // resets the scrolling
			}
		} else {
			entryCursor.close();
		}

	}

	private void showEnclosure(Uri uri, String enclosure, int position1,
			int position2) {
		try {
			startActivityForResult(
					new Intent(Intent.ACTION_VIEW).setDataAndType(uri,
							enclosure.substring(position1 + 3, position2)), 0);
		} catch (Exception e) {
			try {
				startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), 0); // fallbackmode
																				// -
																				// let
																				// the
																				// browser
																				// handle
																				// this
			} catch (Throwable t) {
				Toast.makeText(EntryDetailActivity.this, t.getMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void setupButton(ImageButton button, final boolean successor,
			long date) {
		StringBuilder queryString = new StringBuilder(DATE).append(date)
				.append(AND_ID).append(successor ? '>' : '<').append(_id)
				.append(')').append(OR_DATE).append(successor ? '<' : '>')
				.append(date);

		if (!showRead) {
			queryString.append(MyStrings.DB_AND).append(
					EntriesListAdapter.READDATEISNULL);
		}

		Cursor cursor = getContentResolver().query(parentUri,
				new String[] { FeedData.EntryColumns._ID },
				queryString.toString(), null, successor ? DESC : ASC);

		if (cursor.moveToFirst()) {
			button.setEnabled(true);
			button.setAlpha(BUTTON_ALPHA);

			final String id = cursor.getString(0);

			if (successor) {
				_nextId = id;
			} else {
				_previousId = id;
			}
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					if (successor) {
						nextEntry(false);
					} else {
						previousEntry(false);
					}
				}
			});
		} else {
			button.setEnabled(false);
			button.setAlpha(60);
		}
		cursor.close();
	}

	private void switchEntry(String id, boolean animate, Animation inAnimation,
			Animation outAnimation) {
		uri = parentUri.buildUpon().appendPath(id).build();
		getIntent().setData(uri);
		scrollX = 0;
		scrollY = 0;

		if (animate) {
			WebView dummy = webView; // switch reference

			webView = webView0;
			webView0 = dummy;
		}

		reload();

		if (animate) {
			viewFlipper.setInAnimation(inAnimation);
			viewFlipper.setOutAnimation(outAnimation);
			viewFlipper.addView(webView, layoutParams);
			viewFlipper.showNext();
			viewFlipper.removeViewAt(0);
		}
	}

	private void nextEntry(boolean animate) {
		switchEntry(_nextId, animate, MyAnimations.SLIDE_IN_RIGHT,
				MyAnimations.SLIDE_OUT_LEFT);
	}

	private void previousEntry(boolean animate) {
		switchEntry(_previousId, animate, MyAnimations.SLIDE_IN_LEFT,
				MyAnimations.SLIDE_OUT_RIGHT);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
		if (MainActivity.POSTGINGERBREAD) {
			CompatibilityHelper.onPause(webView);
		}
		scrollX = webView.getScrollX();
		scrollY = webView.getScrollY();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == 92 || keyCode == 94) {
				scrollUp();
				return true;
			} else if (keyCode == 93 || keyCode == 95) {
				scrollDown();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void scrollUp() {
		if (webView != null) {
			webView.pageUp(false);
		}
	}

	private void scrollDown() {
		if (webView != null) {
			webView.pageDown(false);
		}
	}

	/**
	 * Works around android issue 6191
	 */
	@Override
	public void unregisterReceiver(BroadcastReceiver receiver) {
		try {
			super.unregisterReceiver(receiver);
		} catch (Exception e) {
			// do nothing
		}
	}

	@Override
	protected void onDestroy() {
		AdsMogoLayout.clear();
		// 清除adsMogoLayout 实例所产生用于多线程缓冲机制的线程池
		// 此方法请不要轻易调用，如果调用时间不当，会造成无法统计计数
		// adsMogoLayoutCode.clearThread();
		super.onDestroy();
	}

	@Override
	protected int getLayoutRes() {
		return R.layout.entry;
	}

	@Override
	protected void onInitUpbar(ActionBar actionBar) {
		super.onInitUpbar(actionBar);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle("");
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {

		com.actionbarsherlock.view.Menu subMenu=getSubMenu(menu);

		subMenu.add(0, MenuData.MENUITEM_COPY_LINK_INTO_CLIPBOARD, 0,
				R.string.contextmenu_copyurl);
		subMenu.add(0, MenuData.MENUITEM_SHARE, 0, R.string.menu_share);

		subMenu.add(0, MenuData.MENUITEM_DELETE, 0, R.string.contextmenu_delete);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case MenuData.MENUITEM_COPY_LINK_INTO_CLIPBOARD: {
			if (link != null) {
				((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
						.setText(link);
			}
			break;
		}

		case MenuData.MENUITEM_SHARE: {
			if (link != null) {
				startActivity(Intent.createChooser(new Intent(
						Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, link)
						.setType(TEXTPLAIN), getString(R.string.menu_share)));
			}
			break;
		}

		case MenuData.MENUITEM_DELETE: {
			getContentResolver().delete(uri, null, null);
			if (localPictures) {
				FeedData.deletePicturesOfEntry(_id);
			}

			if (nextButton.isEnabled()) {
				nextButton.performClick();
			} else {
				if (previousButton.isEnabled()) {
					previousButton.performClick();
				} else {
					finish();
				}
			}
			break;
		}
		}
		return super.onOptionsItemSelected(menuItem);
	}

}
