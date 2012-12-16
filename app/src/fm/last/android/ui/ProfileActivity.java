/***************************************************************************
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, Michael Novak Jr, and Mostafa Gazar.                   *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.         *
 ***************************************************************************/
package fm.last.android.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.BaseActivity;
import fm.last.android.LastFMApplication;
import fm.last.android.LastFm;
import fm.last.android.player.IRadioPlayer;
import fm.last.android.player.RadioPlayerService;
import fm.last.android.sync.AccountAuthenticatorService;
import fm.last.android.ui.fragment.ProfileActivityFragment;
import fm.last.android.ui.fragment.ProfileEventsFragment;
import fm.last.android.ui.fragment.ProfileFriendsFragment;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.api.LastFmServer;
import fm.last.api.Session;
import fm.last.api.SessionInfo;
import fm.last.api.WSError;
import fm.last.neu.R;

public class ProfileActivity extends BaseActivity {
	private static final String TAG = ProfileActivity.class.getSimpleName();
	
	private SessionInfoTask mSessionInfoTask;
	
	private boolean mIsPlaying = false;
	private boolean mIsPaused = false;
	
	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	
	public static boolean isHTCContactsInstalled(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
			pm.getPackageInfo("com.android.htccontacts", 0);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public void onCreate(Bundle icicle) {
		String username = "";
		boolean isAuthenticatedUser = false;

		super.onCreate(icicle);
		setContentView(R.layout.home);
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(3);
		mTabsAdapter = new TabsAdapter(this, mViewPager);
		
		
		Session session = LastFMApplication.getInstance().session;
		if (session == null || session.getName() == null 
				|| (Build.VERSION.SDK_INT >= 6 
				&& !AccountAuthenticatorService.hasLastfmAccount(this))) {
			LastFMApplication.getInstance().logout();
			Intent intent = new Intent(ProfileActivity.this, SearchActivity.class);
			if(getIntent() != null && getIntent().getStringExtra(SearchManager.QUERY) != null)
				intent.putExtra(SearchManager.QUERY, getIntent().getStringExtra(SearchManager.QUERY));
			startActivity(intent);
			finish();
			return;
		}
		
		Intent intent = getIntent();
		if (intent.getData() != null) {
			if(intent.getData().getScheme() != null && intent.getData().getScheme().equals("lastfm")) {
				LastFMApplication.getInstance().playRadioStation(LastFMApplication.getInstance().mCtx, intent.getData().toString(), true);
				finish();
				return;
			} else if(getIntent().getData().getScheme() != null && getIntent().getData().getScheme().equals("http")) {  //The search provider sent us an http:// URL, forward it to the metadata screen
				Intent i = null;
				if(intent.getData().getPath().contains("/user/")) {
					List<String> segments = getIntent().getData().getPathSegments();
					username = Uri.decode(segments.get(segments.size() - 1));
				} else {
					i = new Intent(this, Metadata.class);
					i.setData(intent.getData());
					startActivity(i);
					finish();
					return;
				}
			} else {
				@SuppressWarnings("deprecation")
				Cursor cursor = managedQuery(getIntent().getData(), null, null,
						null, null);
				if(cursor != null && cursor.moveToNext()) {
					username = cursor.getString(cursor.getColumnIndex("DATA1"));
				}
			}
		} else {
			username = getIntent().getStringExtra("lastfm.profile.username");
		}

		if (username == null) {
			username = session.getName();
			isAuthenticatedUser = true;
		} else {
			isAuthenticatedUser = false;
		}
		
		if(intent.getStringExtra("ERROR_TITLE") != null) {
			AlertDialog.Builder d = new AlertDialog.Builder(this);
			d.setTitle(intent.getStringExtra("ERROR_TITLE"));
			d.setMessage(intent.getStringExtra("ERROR_DESCRIPTION"));
			d.setIcon(android.R.drawable.ic_dialog_alert);
			d.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			d.show();
		}

		if(RadioPlayerService.radioAvailable(this)) {
			getPackageManager().setComponentEnabledSetting(new ComponentName("fm.last.neu", PlayerActivity.class.getName()), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
		} else {
			getPackageManager().setComponentEnabledSetting(new ComponentName("fm.last.neu", PlayerActivity.class.getName()), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(false);

		if (isAuthenticatedUser) {
			actionBar.setDisplayShowTitleEnabled(false);
			
			// Latest Activity.
			Bundle chartsArgs = new Bundle();
			ProfileActivityFragment.username = username;
			chartsArgs.putString("user", username);
			mTabsAdapter.addTab(actionBar
		            .newTab()
		            .setText(R.string.profile_myprofile)
		            , ProfileActivityFragment.class, chartsArgs);
			
			// Friends.
			Bundle friendsArgs = new Bundle();
			ProfileFriendsFragment.username = username;
			chartsArgs.putString("user", username);
			mTabsAdapter.addTab(actionBar
		            .newTab()
		            .setText(R.string.profile_friends)
		            , ProfileFriendsFragment.class, friendsArgs);
						
			// Events.
			Bundle eventsArgs = new Bundle();
			ProfileEventsFragment.username = username;
			eventsArgs.putString("user", username);
			mTabsAdapter.addTab(actionBar
		            .newTab()
		            .setText(R.string.profile_events)
		            , ProfileEventsFragment.class, eventsArgs);
		} else {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(username);
			
			// Latest Activity.
			Bundle chartsArgs = new Bundle();
			ProfileActivityFragment.username = username;
			chartsArgs.putString("user", username);
			mTabsAdapter.addTab(actionBar
		            .newTab()
		            .setText(getString(R.string.profile_userprofile))
		            , ProfileActivityFragment.class, chartsArgs);
			
			// Something.
			Bundle xxxArgs = new Bundle();
			ProfileActivityFragment.username = username;
			xxxArgs.putString("user", username);
			mTabsAdapter.addTab(actionBar
		            .newTab()
		            .setText("Something")
			            , ProfileActivityFragment.class, chartsArgs);
		}

		File f = new File(Environment.getExternalStorageDirectory() + "/lastfm-logs.zip");
		if (f.exists()) {
			Log.i("Last.fm", "Removing stale bug report archive");
			f.delete();
		}

		mSessionInfoTask = new SessionInfoTask();
		mSessionInfoTask.execute();
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		getSupportActionBar().setSelectedNavigationItem(state.getInt("tab", 0));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mIsPlaying = false;
		
		LastFMApplication.getInstance().bindService(new Intent(LastFMApplication.getInstance(), fm.last.android.player.RadioPlayerService.class),
				new ServiceConnection() {
					public void onServiceConnected(ComponentName comp, IBinder binder) {
						IRadioPlayer player = IRadioPlayer.Stub.asInterface(binder);
						try {
							mIsPlaying = player.isPlaying();
							mIsPaused = (player.getState() == RadioPlayerService.STATE_PAUSED);
						} catch (RemoteException e) {
							Log.e(TAG, e.toString());
						}
						try {
							LastFMApplication.getInstance().unbindService(this);
						} catch (IllegalArgumentException e) {
							Log.e(TAG, e.toString());
						}
					}

					public void onServiceDisconnected(ComponentName comp) {
					}
				}, Context.BIND_AUTO_CREATE);

		if (LastFMApplication.getInstance().session == null) {
			finish(); // We shouldn't really get here, but sometimes the window
						// stack keeps us around
		}
		try {
			LastFMApplication.getInstance().tracker.trackPageView("/Profile");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}
		
		showSyncPrompts();

	}

	private void showSyncPrompts() {
		if(Build.VERSION.SDK_INT >= 6) {
			SharedPreferences settings = getSharedPreferences(LastFm.PREFS, 0);
			if(!settings.getBoolean("sync_nag", false) && !isHTCContactsInstalled(this)) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("sync_nag", true);
				editor.commit();
				showContactSyncPrompt();
			} else if(Build.VERSION.SDK_INT >= 14 && !settings.getBoolean("sync_nag_cal", false)) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("sync_nag_cal", true);
				editor.commit();
				showCalendarSyncPrompt();
			}
		}
	}

	private void showContactSyncPrompt() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sync_prompt_title)
			.setMessage(R.string.sync_prompt_body)
			.setCancelable(false)
			.setPositiveButton(R.string.common_yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					AccountManager am = AccountManager.get(ProfileActivity.this);
					Account[] accounts = am.getAccountsByType(getString(R.string.ACCOUNT_TYPE));
					ContentResolver.setIsSyncable(accounts[0], ContactsContract.AUTHORITY, 1);
		            ContentResolver.setSyncAutomatically(accounts[0], ContactsContract.AUTHORITY, true);
		            showSyncPrompts();
				}
			})
			.setNegativeButton(R.string.common_no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					showSyncPrompts();
				}
			});
		builder.show();
	}

	private void showCalendarSyncPrompt() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.cal_sync_prompt_title)
			.setMessage(R.string.cal_sync_prompt_body)
			.setCancelable(false)
			.setPositiveButton(R.string.common_yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					AccountManager am = AccountManager.get(ProfileActivity.this);
					Account[] accounts = am.getAccountsByType(getString(R.string.ACCOUNT_TYPE));
					ContentResolver.setIsSyncable(accounts[0], CalendarContract.AUTHORITY, 1);
		            ContentResolver.setSyncAutomatically(accounts[0], CalendarContract.AUTHORITY, true);
		            showSyncPrompts();
				}
			})
			.setNegativeButton(R.string.common_no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					showSyncPrompts();
				}
			});
		builder.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.profile, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(mIsPaused) {
			menu.findItem(R.id.menu_item_nowplaying).setTitle(getString(R.string.action_nowpaused));
		} else {
			menu.findItem(R.id.menu_item_nowplaying).setTitle(getString(R.string.action_nowplaying));
		}
		menu.findItem(R.id.menu_item_nowplaying).setEnabled(mIsPlaying || mIsPaused);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_item_search:
			Bundle appData = new Bundle();
			startSearch(null, false, appData, false);
			return true;
		case R.id.menu_item_logout:
			LastFMApplication.getInstance().logout();
			intent = new Intent(ProfileActivity.this, LastFm.class);
			startActivity(intent);
			finish();
			break;
		case R.id.menu_item_settings:
			intent = new Intent(ProfileActivity.this, Preferences.class);
			startActivity(intent);
			return true;
//		case 2:
//			intent = new Intent(Profile.this, Help.class);
//			startActivity(intent);
//			return true;
		case R.id.menu_item_nowplaying:
			intent = new Intent(ProfileActivity.this, PlayerActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}
	
	/**
	 * In a task because it can take a while, and Android has a tendency to
	 * panic and show the force quit/wait dialog quickly. And this blocks.
	 */
	private class SessionInfoTask extends AsyncTaskEx<String, Void, SessionInfo> {

		SessionInfoTask() {
		}

		@Override
		public SessionInfo doInBackground(String... params) {

			try {
				LastFmServer server = AndroidLastFmServerFactory.getServer();
				SessionInfo userSession = server.getSessionInfo(LastFMApplication.getInstance().session.getKey());
				return userSession;
			} catch (WSError e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(SessionInfo userSession) {
			mSessionInfoTask = null;

			if (userSession != null) {
				SharedPreferences.Editor editor = getSharedPreferences(LastFm.PREFS, 0).edit();
				editor.putBoolean("lastfm_radio", userSession.getRadio());
				editor.putBoolean("lastfm_freetrial", userSession.getFreeTrial());
				editor.putBoolean("lastfm_expired", userSession.getExpired());
				if(userSession.getPlaysLeft() != null)
					editor.putInt("lastfm_playsleft", userSession.getPlaysLeft());
				if(userSession.getPlaysElapsed() != null)
					editor.putInt("lastfm_playselapsed", userSession.getPlaysElapsed());
				editor.commit();
			}
		}
	}

	public static class TabsAdapter extends FragmentPagerAdapter implements
			ActionBar.TabListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final ActionBar mActionBar;
		private final ViewPager mViewPager;
		private final List<TabInfo> mTabs = new ArrayList<TabInfo>();

		static final class TabInfo {
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(Class<?> _class, Bundle _args) {
				clss = _class;
				args = _args;
			}
		}

		public TabsAdapter(SherlockFragmentActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mActionBar = activity.getSupportActionBar();
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
			TabInfo info = new TabInfo(clss, args);
			tab.setTag(info);
			tab.setTabListener(this);
			mTabs.add(info);
			mActionBar.addTab(tab);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.clss.getName(),
					info.args);
		}

		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		public void onPageSelected(int position) {
			mActionBar.setSelectedNavigationItem(position);
		}

		public void onPageScrollStateChanged(int state) {
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Object tag = tab.getTag();
			for (int i = 0; i < mTabs.size(); i++) {
				if (mTabs.get(i) == tag) {
					mViewPager.setCurrentItem(i);
				}
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}
}
