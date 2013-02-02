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

import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

import fm.last.android.Amazon;
import fm.last.android.BaseActivity;
import fm.last.android.LastFMApplication;
import fm.last.android.player.IRadioPlayer;
import fm.last.android.ui.fragment.ArtistBioFragment;
import fm.last.android.ui.fragment.ArtistEventsFragment;
import fm.last.android.ui.fragment.ArtistFansFragment;
import fm.last.android.ui.fragment.ArtistSimilarFragment;
import fm.last.android.ui.fragment.ArtistTagsFragment;
import fm.last.neu.R;

/**
 * @author Jono Cole <jono@last.fm>
 * 
 */
public class ArtistActivity extends BaseActivity {
	
	private ArtistBioFragment mArtistBioFragment;
	private ArtistSimilarFragment mArtistSimilarFragment;
	private ArtistTagsFragment mArtistTagsFragment;
	private ArtistEventsFragment mEventsFragment;
	private ArtistFansFragment mFansFragment;
	
	private String mArtistName = "";
	private String mTrackName = "";

	private boolean mIsPlaying = false;

	private FragmentStatePagerAdapter mAdapter;
	private ViewPager mTabPager;
	private PageIndicator mIndicator;
	
	public ArtistActivity() {
		super();
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_artist);

		if(getIntent().getData() != null) {
			if(getIntent().getData().getScheme().equals("http")) {
				List<String> segments = getIntent().getData().getPathSegments();
				
				mArtistName = Uri.decode(segments.get(1)).replace("+", " ");
				if(segments.size() > 3)
					mTrackName = Uri.decode(segments.get(3)).replace("+", " ");
			}
		} else if(getIntent().getAction() != null) {
			if (getIntent().getAction().equals(MediaStore.INTENT_ACTION_MEDIA_SEARCH)) {
				mArtistName = getIntent().getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST);
			}
		} else {
			mArtistName = getIntent().getStringExtra("artist");
			mTrackName = getIntent().getStringExtra("track");
		}
		
		mArtistBioFragment = new ArtistBioFragment();
		mArtistSimilarFragment = new ArtistSimilarFragment();
		mArtistTagsFragment = new ArtistTagsFragment();
		mEventsFragment = new ArtistEventsFragment();
		mFansFragment = new ArtistFansFragment();
		mAdapter = new LastfmFragmentStatePagerAdapter(getSupportFragmentManager());
		
		mTabPager = (ViewPager) findViewById(R.id.pager);
		mTabPager.setAdapter(mAdapter);
		mTabPager.setOffscreenPageLimit(5);
		
		mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
		mIndicator.setViewPager(mTabPager, 1);
		
		if (getIntent().hasExtra("show_events")) {
			mTabPager.setCurrentItem(3, true);
		}

		mIsPlaying = false;

		LastFMApplication.getInstance().bindService(new Intent(LastFMApplication.getInstance(), fm.last.android.player.RadioPlayerService.class),
				new ServiceConnection() {
					public void onServiceConnected(ComponentName comp, IBinder binder) {
						IRadioPlayer player = IRadioPlayer.Stub.asInterface(binder);
						try {
							mIsPlaying = player.isPlaying();
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							LastFMApplication.getInstance().unbindService(this);
						} catch (IllegalArgumentException e) {
						}
					}

					public void onServiceDisconnected(ComponentName comp) {
					}
				}, 0);

	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			LastFMApplication.getInstance().tracker.trackPageView("/Artist");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.player, menu);

		MenuItem changeView = menu.findItem(R.id.info_menu_item);
		changeView.setTitle(getString(R.string.action_nowplaying));
		changeView.setIcon(R.drawable.view_artwork);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.buy_menu_item).setEnabled(Amazon.getAmazonVersion(this) > 0);
		menu.findItem(R.id.info_menu_item).setEnabled(mIsPlaying);

		return super.onPrepareOptionsMenu(menu);
	}

	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
		case R.id.info_menu_item:
			Intent i = new Intent(this, PlayerActivity.class);
			startActivity(i);
			finish();
			break;
		case R.id.buy_menu_item:
			Amazon.searchForTrack(this, mArtistName, mTrackName);
			break;
		case R.id.share_menu_item:
			intent = new Intent(this, ShareResolverActivity.class);
			intent.putExtra(Share.INTENT_EXTRA_ARTIST, mArtistName);
			intent.putExtra(Share.INTENT_EXTRA_TRACK, mTrackName);
			startActivity(intent);
			break;
		case R.id.tag_menu_item:
			intent = new Intent(this, fm.last.android.ui.Tag.class);
			intent.putExtra("lastfm.artist", mArtistName);
			intent.putExtra("lastfm.track", mTrackName);
			startActivity(intent);
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	class LastfmFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

		public LastfmFragmentStatePagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0) {
				return mArtistSimilarFragment;
			} else if (position == 1) {
				return mArtistBioFragment;
			} else if (position == 2) {
				return mArtistTagsFragment;
			} else if (position == 3) {
				return mEventsFragment;
			} else if (position == 4) {
				return mFansFragment;
			}
			
			return mArtistBioFragment;
		}

		@Override
		public int getCount() {
			return 5;// Number of tabs.
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position == 0) {
				return getString(R.string.metadata_similar);				
			} else if (position == 1) {
				return getString(R.string.metadata_bio);
			} else if (position == 2) {
				return getString(R.string.metadata_tags);
			} else if (position == 3) {
				return getString(R.string.metadata_events);
			} else if (position == 4) {
				return getString(R.string.metadata_listeners);
			}

			return getString(R.string.metadata_bio);
		}

	}
	
}
