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
package fm.last.android.ui.fragment;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.LastFMApplication;
import fm.last.android.R;
import fm.last.android.player.RadioPlayerService;
import fm.last.android.ui.adapter.ListEntry;
import fm.last.android.ui.adapter.ProfileFriendsListAdapter;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.android.utils.ImageCache;
import fm.last.api.LastFmServer;
import fm.last.api.User;
import fm.last.api.WSError;

public class ProfileFriendsFragment extends SherlockFragment {

	private Activity mContext;
	private ViewGroup viewer;
	
	public static String username; // store this separate so we have access to it before User obj is retrieved
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();

	private ListView mProfileFriendsListView;

	private ImageCache mImageCache = null;

	private IntentFilter mIntentFilter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		viewer = (ViewGroup) inflater.inflate(R.layout.fragment_profile_friends,
				container, false);
		mContext = getActivity();
		
//		username = savedInstanceState.getString("user");

		mProfileFriendsListView = (ListView) viewer.findViewById(R.id.profile_friends_list);
		mProfileFriendsListView.setOnItemClickListener(mFriendsItemClickListener);

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(RadioPlayerService.PLAYBACK_ERROR);
		mIntentFilter.addAction(RadioPlayerService.STATION_CHANGED);
		mIntentFilter.addAction("fm.last.android.ERROR");

		try {
			LastFMApplication.getInstance().tracker.trackPageView("/Profile/Friends");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}
		new LoadFriendsTask().execute((Void) null);
		
		return viewer;
	}

	private OnItemClickListener mFriendsItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			try {
				User user = (User) l.getAdapter().getItem(position);
				if(user != null) {
					Intent profileIntent = new Intent(mContext, fm.last.android.ui.ProfileActivity.class);
					profileIntent.putExtra("lastfm.profile.username", user.getName());
					startActivity(profileIntent);
				}
			} catch (ClassCastException e) {
				// when the list item is not a User
			}
		}
	};

	private class LoadFriendsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				User[] friends = mServer.getFriends(username, null, "1024").getFriends();
				if (friends.length == 0) {
					return null;
				}
				
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < friends.length; i++) {
					// Some tracks don't have images.
					ListEntry entry = new ListEntry(friends[i],
							R.drawable.profile_unknown, friends[i].getName(),
							friends[i].getImages().length == 0 ? ""
									: friends[i]
											.getURLforImageSize("extralarge")); 

					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(mContext, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ProfileFriendsListAdapter adapter = new ProfileFriendsListAdapter(mContext, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileFriendsListView.setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_nofriends) };
				ProfileFriendsListAdapter adapter = new ProfileFriendsListAdapter(mContext, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileFriendsListView.setAdapter(adapter);
			}
		}
	}

	private ImageCache getImageCache() {
		if (mImageCache == null) {
			mImageCache = new ImageCache();
		}
		
		return mImageCache;
	}

}
