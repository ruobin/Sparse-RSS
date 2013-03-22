/**
 * 
 */
package cn.eric.rss;

import android.os.Bundle;
import android.os.Looper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.umeng.analytics.MobclickAgent;

/**
 * @author Ruobin Wang
 * 
 */
public abstract class SherlockActivityBase extends SherlockActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(R.style.Theme_Eric);
		setContentView(getLayoutRes());

		ActionBar actionBar = getSupportActionBar();
		onInitConfiguration();
		onInitUpbar(actionBar);
	}

	/**
	 * 获取并设置当前的layout
	 * 
	 * @return
	 */
	protected abstract int getLayoutRes();

	/**
	 * 初始化用户配置
	 */
	protected void onInitConfiguration() {
		// UserConfigurationUtils
		// .loadUserConfig(getApplicationContext(),
		// TravelUserConfigurationImpl.class);
	}

	/**
	 * 初始化upBar
	 */
	protected void onInitUpbar(ActionBar actionBar) {
		actionBar.setLogo(R.drawable.ic_logo);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	/**
	 * if you can't get title as soon as the activity is created, you can update
	 * it later
	 * 
	 * @param title
	 */
	protected void updateUpbarTitle(final String title) {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (Looper.getMainLooper() == Looper.myLooper()) {
				actionBar.setDisplayShowTitleEnabled(true);
				actionBar.setTitle(title);
			} else {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						actionBar.setDisplayShowTitleEnabled(true);
						actionBar.setTitle(title);
					}
				});
			}
		}
	}


	protected SubMenu getSubMenu(com.actionbarsherlock.view.Menu menu){
		SubMenu subMenu = menu.addSubMenu("").setIcon(R.drawable.ic_more);

		subMenu.getItem().setShowAsAction(
				MenuItem.SHOW_AS_ACTION_ALWAYS
						| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return subMenu;
	}
	
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem menuItem) {
		if (menuItem.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(menuItem);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
}
