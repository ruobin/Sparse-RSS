package cn.eric.rss.data;

import android.content.ContentValues;

public class DataHelper {


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

	
}
