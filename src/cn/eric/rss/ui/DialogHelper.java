package cn.eric.rss.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import cn.eric.rss.R;

public class DialogHelper {


	public static Dialog createErrorDialog(Activity activity, int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setMessage(messageId);
		builder.setTitle(R.string.error);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton(android.R.string.ok, null);
		return builder.create();
	}
	
}
