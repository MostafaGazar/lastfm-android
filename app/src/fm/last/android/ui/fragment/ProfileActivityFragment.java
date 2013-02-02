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

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.LastFMApplication;
import fm.last.android.player.RadioPlayerService;
import fm.last.android.ui.ArtistActivity;
import fm.last.android.ui.PopupActionActivity;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.android.widget.QuickContactProfileBubble;
import fm.last.api.Album;
import fm.last.api.Artist;
import fm.last.api.LastFmServer;
import fm.last.api.Track;
import fm.last.api.User;
import fm.last.api.WSError;
import fm.last.neu.R;

public class ProfileActivityFragment extends BaseFragment {

	private Activity mContext;
	private ViewGroup viewer;
	
	public static String username; // store this separate so we have access to it before User obj is retrieved
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();

	private IntentFilter mIntentFilter;
	
	private QuickContactProfileBubble mProfileBubble;
	
	private ImageView mLastTrackImg1;
	private ImageView mLastTrackImg2;
	private ImageView mLastTrackImg3;
	
	private View mTopArtisitsContainer;
	private ImageView mTopArtistImg1;
	private ImageView mTopArtistImg2;
	private ImageView mTopArtistImg3;
	
	private View mTopAlbumsContainer;
	private ImageView mTopAlbumImg1;
	private ImageView mTopAlbumImg2;
	private ImageView mTopAlbumImg3;
	
	private View mTopTracksContainer;
	private ImageView mTopTrackImg1;
	private ImageView mTopTrackImg2;
	private ImageView mTopTrackImg3;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		viewer = (ViewGroup) inflater.inflate(R.layout.fragment_profile_activity,
				container, false);
		mContext = getActivity();
		
//		username = savedInstanceState.getString("user");
		
		mProfileBubble = (QuickContactProfileBubble) viewer.findViewById(R.id.user_profile_bubble);
		mProfileBubble.setClickable(false);

		mLastTrackImg1 = (ImageView) viewer.findViewById(R.id.user_last_track_1);
		mLastTrackImg1.setOnClickListener(mTrackClickListener);
		mLastTrackImg2 = (ImageView) viewer.findViewById(R.id.user_last_track_2);
		mLastTrackImg2.setOnClickListener(mTrackClickListener);
		mLastTrackImg3 = (ImageView) viewer.findViewById(R.id.user_last_track_3);
		mLastTrackImg3.setOnClickListener(mTrackClickListener);
		
		mTopArtisitsContainer = viewer.findViewById(R.id.user_top_artisits_container);
		mTopArtistImg1 = (ImageView) viewer.findViewById(R.id.user_top_artist_1);
		mTopArtistImg1.setOnClickListener(mArtistClickListener);
		mTopArtistImg2 = (ImageView) viewer.findViewById(R.id.user_top_artist_2);
		mTopArtistImg2.setOnClickListener(mArtistClickListener);
		mTopArtistImg3 = (ImageView) viewer.findViewById(R.id.user_top_artist_3);
		mTopArtistImg3.setOnClickListener(mArtistClickListener);
		
		mTopAlbumsContainer = viewer.findViewById(R.id.user_top_albums_container);
		mTopAlbumImg1 = (ImageView) viewer.findViewById(R.id.user_top_album_1);
		mTopAlbumImg1.setOnClickListener(mAlbumClickListener);
		mTopAlbumImg2 = (ImageView) viewer.findViewById(R.id.user_top_album_2);
		mTopAlbumImg2.setOnClickListener(mAlbumClickListener);
		mTopAlbumImg3 = (ImageView) viewer.findViewById(R.id.user_top_album_3);
		mTopAlbumImg3.setOnClickListener(mAlbumClickListener);
		
		mTopTracksContainer = viewer.findViewById(R.id.user_top_tracks_container);
		mTopTrackImg1 = (ImageView) viewer.findViewById(R.id.user_top_track_1);
		mTopTrackImg1.setOnClickListener(mTrackClickListener);
		mTopTrackImg2 = (ImageView) viewer.findViewById(R.id.user_top_track_2);
		mTopTrackImg2.setOnClickListener(mTrackClickListener);
		mTopTrackImg3 = (ImageView) viewer.findViewById(R.id.user_top_track_3);
		mTopTrackImg3.setOnClickListener(mTrackClickListener);
		
		new LoadUserTask().execute((Void)null);

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(RadioPlayerService.PLAYBACK_ERROR);
		mIntentFilter.addAction(RadioPlayerService.STATION_CHANGED);
		mIntentFilter.addAction("fm.last.android.ERROR");
		
		return viewer;
	}
	
	private class LoadUserTask extends AsyncTaskEx<Void, Void, Boolean> {
		User mUser = null;
		
		@Override
		public Boolean doInBackground(Void... params) {
			LastFmServer server = AndroidLastFmServerFactory.getServer();
			try {
				mUser = server.getUserInfo(username, null);
			} catch (WSError e) {
				return false;
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		
		@Override
		public void onPostExecute(Boolean result) {
			if(result) {
				mProfileBubble.setUser(mUser);
				
				new LoadRecentTracksTask().execute((Void)null);
			}
		}
	}

//	public void onListItemClick(ListView l, View v, int position, long id) {
//		setNextAnimation();
//		mProfileAdapter.enableLoadBar(position-1);
//		if(!username.equals(LastFMApplication.getInstance().session.getName()))
//			position++;
//		switch (position-1) {
//		case PROFILE_RECOMMENDED: // "Top Artists"
//			try {
//				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/RecommendedArtists");
//			} catch (Exception e) {
//				//Google Analytics doesn't appear to be thread safe
//			}
//			new LoadRecommendedArtistsTask().execute((Void) null);
//			break;
//		case PROFILE_TOPARTISTS: // "Top Artists"
//			try {
//				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/TopArtists");
//			} catch (Exception e) {
//				//Google Analytics doesn't appear to be thread safe
//			}
//			new LoadTopArtistsTask().execute((Void) null);
//			break;
//		case PROFILE_TOPALBUMS: // "Top Albums"
//			try {
//				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/TopAlbums");
//			} catch (Exception e) {
//				//Google Analytics doesn't appear to be thread safe
//			}
//			new LoadTopAlbumsTask().execute((Void) null);
//			break;
//		case PROFILE_TOPTRACKS: // "Top Tracks"
//			try {
//				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/TopTracks");
//			} catch (Exception e) {
//				//Google Analytics doesn't appear to be thread safe
//			}
//			new LoadTopTracksTask().execute((Void) null);
//			break;
//		case PROFILE_RECENTLYPLAYED: // "Recently Played"
//			try {
//				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/Recent");
//			} catch (Exception e) {
//				//Google Analytics doesn't appear to be thread safe
//			}
//			new LoadRecentTracksTask().execute((Void) null);
//			break;
//		case PROFILE_FRIENDS: // "Friends"
//			try {
//				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Friends");
//			} catch (Exception e) {
//				//Google Analytics doesn't appear to be thread safe
//			}
//			new LoadFriendsTask().execute((Void) null);
//			break;
//		case PROFILE_TAGS: // "Tags"
//			try {
//				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Tags");
//			} catch (Exception e) {
//				//Google Analytics doesn't appear to be thread safe
//			}
//			new LoadTagsTask().execute((Void) null);
//			break;
//		default:
//			break;
//
//		}
//	}

	private OnClickListener mArtistClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Artist artist = (Artist) v.getTag();
				if(artist != null) {
					Intent i = new Intent(mContext, ArtistActivity.class);
					i.putExtra("artist", artist.getName());
					startActivity(i);
				}
			} catch (ClassCastException e) {
				// fine.
			}
		}
	};

	private OnClickListener mAlbumClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Album album = (Album) v.getTag();
				if(album != null) {
					Intent i = new Intent(mContext, PopupActionActivity.class);
					i.putExtra("lastfm.artist", album.getArtist());
					i.putExtra("lastfm.album", album.getTitle());
					startActivity(i);
				}
			} catch (ClassCastException e) {
				// (Album) cast can fail, like when the list contains a string
				// saying: "no items"
			}
		}

	};

	private OnClickListener mTrackClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Track track = (Track) v.getTag();
				if(track != null) {
					Intent i = new Intent(mContext, PopupActionActivity.class);
					i.putExtra("lastfm.artist", track.getArtist().getName());
					i.putExtra("lastfm.track", track.getName());
					startActivity(i);
				}
			} catch (ClassCastException e) {
				// (Track) cast can fail, like when the list contains a string
				// saying: "no items"
			}
		}

	};

//	private OnItemClickListener mTagClickListener = new OnItemClickListener() {
//		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
//			try {
//				Session session = LastFMApplication.getInstance().session;
//				Tag tag = (Tag) l.getAdapter().getItem(position);
//				if(tag != null) {
//					if (session.getSubscriber().equals("1"))
//						LastFMApplication.getInstance().playRadioStation(mContext, "lastfm://usertags/" + username + "/" + Uri.encode(tag.getName()), true);
//					else
//						LastFMApplication.getInstance().playRadioStation(mContext, "lastfm://globaltags/" + Uri.encode(tag.getName()), true);
//				}
//			} catch (ClassCastException e) {
//				// when the list item is not a tag
//			}
//		}
//
//	};

//	private OnItemClickListener mUserClickListener = new OnItemClickListener() {
//		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
//			try {
//				User user = (User) l.getAdapter().getItem(position);
//				if(user != null) {
//					Intent profileIntent = new Intent(mContext, fm.last.android.ui.ProfileActivity.class);
//					profileIntent.putExtra("lastfm.profile.username", user.getName());
//					startActivity(profileIntent);
//				}
//			} catch (ClassCastException e) {
//				// when the list item is not a User
//			}
//		}
//	};

//	private class LoadRecommendedArtistsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {
//
//		@Override
//		public ArrayList<ListEntry> doInBackground(Void... params) {
//
//			try {
//				Artist[] recartists = mServer.getUserRecommendedArtists(username, LastFMApplication.getInstance().session.getKey());
//				if (recartists.length == 0)
//					return null;
//				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
//				for (int i = 0; i < ((recartists.length < 10) ? recartists.length : 10); i++) {
//					String url = null;
//					try {
//						ImageUrl[] urls = recartists[i].getImages();
//						url = urls[0].getUrl();
//					} catch (ArrayIndexOutOfBoundsException e) {
//					}
//
//					ListEntry entry = new ListEntry(recartists[i], R.drawable.artist_icon, recartists[i].getName(), url);
//					iconifiedEntries.add(entry);
//				}
//				return iconifiedEntries;
//			} catch (Exception e) {
//				e.printStackTrace();
//			} catch (WSError e) {
//				LastFMApplication.getInstance().presentError(mContext, e);
//			}
//			return null;
//		}
//
//		@Override
//		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
//			if (iconifiedEntries != null) {
//				ListAdapter adapter = new ListAdapter(mContext, getImageCache());
//				adapter.setSourceIconified(iconifiedEntries);
//				mProfileLists[PROFILE_RECOMMENDED].setAdapter(adapter);
//			} else {
//				String[] strings = new String[] { getString(R.string.profile_notopartists) };
//				ListAdapter adapter = new ListAdapter(mContext, strings);
//				adapter.disableDisclosureIcons();
//				adapter.setDisabled();
//				mProfileLists[PROFILE_RECOMMENDED].setAdapter(adapter);
//			}
//			// Save the current view
//			mViewHistory.push(mNestedViewFlipper.getDisplayedChild());
//			mNestedViewFlipper.setDisplayedChild(PROFILE_RECOMMENDED + 1);
//		}
//	}

	private class LoadTopArtistsTask extends AsyncTaskEx<Void, Void, Artist[]> {

		@Override
		public Artist[] doInBackground(Void... params) {

			try {
				return mServer.getUserTopArtists(username, "overall", 3);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(mContext, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(Artist[] topArtists) {
			new LoadTopAlbumsTask().execute((Void)null);
			
			if (topArtists != null && topArtists.length > 0) {
				int size = topArtists.length;
				Artist artist = topArtists[0];
				mTopArtistImg1.setTag(artist);
				String url = artist.getImages().length == 0 ? "" : artist
						.getURLforImageSize("extralarge");
				if (!TextUtils.isEmpty(url)) {
					UrlImageViewHelper.setUrlDrawable(mTopArtistImg1, url, R.color.transparent_lastfm_2);
				}
				
				if (size > 1) {
					artist = topArtists[1];
					mTopArtistImg2.setTag(artist);
					url = artist.getImages().length == 0 ? "" : artist
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mTopArtistImg2, url, R.color.transparent_lastfm_2);
					}
				}
				
				if (size > 2) {
					artist = topArtists[2];
					mTopArtistImg3.setTag(artist);
					url = artist.getImages().length == 0 ? "" : artist
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mTopArtistImg3, url, R.color.transparent_lastfm_2);
					}
				}
			} else {
				mTopArtisitsContainer.setVisibility(View.GONE);
			}
		}
	}

	private class LoadTopAlbumsTask extends AsyncTaskEx<Void, Void, Album[]> {

		@Override
		public Album[] doInBackground(Void... params) {

			try {
				return mServer.getUserTopAlbums(username, "overall", 3);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(mContext, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(Album[] topAlbums) {
			new LoadTopTracksTask().execute((Void)null);
			
			if (topAlbums != null && topAlbums.length > 0) {
				int size = topAlbums.length;
				Album album = topAlbums[0];
				mTopAlbumImg1.setTag(album);
				String url = album.getImages().length == 0 ? "" : album
						.getURLforImageSize("extralarge");
				if (!TextUtils.isEmpty(url)) {
					UrlImageViewHelper.setUrlDrawable(mTopAlbumImg1, url, R.color.transparent_lastfm_3);
				}
				
				if (size > 1) {
					album = topAlbums[1];
					mTopAlbumImg2.setTag(album);
					url = album.getImages().length == 0 ? "" : album
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mTopAlbumImg2, url, R.color.transparent_lastfm_3);
					}
				}
				
				if (size > 2) {
					album = topAlbums[2];
					mTopAlbumImg3.setTag(album);
					url = album.getImages().length == 0 ? "" : album
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mTopAlbumImg3, url, R.color.transparent_lastfm_3);
					}
				}
			} else {
				mTopAlbumsContainer.setVisibility(View.GONE);
			}
		}
	}

	private class LoadTopTracksTask extends AsyncTaskEx<Void, Void, Track[]> {

		@Override
		public Track[] doInBackground(Void... params) {
			try {
				return mServer.getUserTopTracks(username, "overall", 3);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(mContext, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(Track[] topTracks) {
			if (topTracks != null && topTracks.length > 0) {
				int size = topTracks.length;
				Track track = topTracks[0];
				mTopTrackImg1.setTag(track);
				String url = track.getImages().length == 0 ? "" : track
						.getURLforImageSize("extralarge");
				if (!TextUtils.isEmpty(url)) {
					UrlImageViewHelper.setUrlDrawable(mTopTrackImg1, url, R.color.transparent_lastfm_4);
				}
				
				if (size > 1) {
					track = topTracks[1];
					mTopTrackImg2.setTag(track);
					url = track.getImages().length == 0 ? "" : track
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mTopTrackImg2, url, R.color.transparent_lastfm_4);
					}
				}
				
				if (size > 2) {
					track = topTracks[2];
					mTopTrackImg3.setTag(track);
					url = track.getImages().length == 0 ? "" : track
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mTopTrackImg3, url, R.color.transparent_lastfm_4);
					}
				}
			} else {
				mTopTracksContainer.setVisibility(View.GONE);
			}
		}
	}

	private class LoadRecentTracksTask extends AsyncTaskEx<Void, Void, Track[]> {

		@Override
		public Track[] doInBackground(Void... params) {
			try {
				return mServer.getUserRecentTracks(username, "true", 3);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
			}
			return null;
		}

		@Override
		public void onPostExecute(Track[] recentTracks) {
			new LoadTopArtistsTask().execute((Void)null);
			
			if (recentTracks != null && recentTracks.length > 0) {
				int size = recentTracks.length;
				Track track = recentTracks[0];
				mLastTrackImg1.setTag(track);
				String url = track.getImages().length == 0 ? "" : track
						.getURLforImageSize("extralarge");
				if (!TextUtils.isEmpty(url)) {
					UrlImageViewHelper.setUrlDrawable(mLastTrackImg1, url, R.color.transparent_lastfm);
				}
				
				if (size > 1) {
					track = recentTracks[1];
					mLastTrackImg2.setTag(track);
					url = track.getImages().length == 0 ? "" : track
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mLastTrackImg2, url, R.color.transparent_lastfm);
					}
				}
				
				if (size > 2) {
					track = recentTracks[2];
					mLastTrackImg3.setTag(track);
					url = track.getImages().length == 0 ? "" : track
							.getURLforImageSize("extralarge");
					if (!TextUtils.isEmpty(url)) {
						UrlImageViewHelper.setUrlDrawable(mLastTrackImg3, url, R.color.transparent_lastfm);
					}
				}
			} else {
				// XXX :: Do nothing for now.
			}
		}
	}

//	private class LoadTagsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {
//
//		@Override
//		public ArrayList<ListEntry> doInBackground(Void... params) {
//			try {
//				Tag[] tags = mServer.getUserTopTags(username, 10);
//				if (tags.length == 0)
//					return null;
//
//				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
//				for (int i = 0; i < ((tags.length < 10) ? tags.length : 10); i++) {
//					ListEntry entry = new ListEntry(tags[i], -1, tags[i].getName(), R.drawable.list_icon_station);
//					iconifiedEntries.add(entry);
//				}
//				return iconifiedEntries;
//			} catch (Exception e) {
//				e.printStackTrace();
//			} catch (WSError e) {
//				LastFMApplication.getInstance().presentError(mContext, e);
//			}
//			return null;
//		}
//
//		@Override
//		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
//			if (iconifiedEntries != null) {
//				ListAdapter adapter = new ListAdapter(mContext, getImageCache());
//				adapter.setSourceIconified(iconifiedEntries);
//				mProfileLists[PROFILE_TAGS].setAdapter(adapter);
//			} else {
//				String[] strings = new String[] { getString(R.string.profile_notags) };
//				ListAdapter adapter = new ListAdapter(mContext, strings);
//				adapter.disableDisclosureIcons();
//				adapter.setDisabled();
//				mProfileLists[PROFILE_TAGS].setAdapter(adapter);
//			}
//			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
//																		// the
//																		// current
//																		// view
//			mNestedViewFlipper.setDisplayedChild(PROFILE_TAGS + 1);
//		}
//	}

//	private class LoadFriendsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {
//
//		@Override
//		public ArrayList<ListEntry> doInBackground(Void... params) {
//			try {
//				User[] friends = mServer.getFriends(username, null, "1024").getFriends();
//				if (friends.length == 0)
//					return null;
//				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
//				for (int i = 0; i < friends.length; i++) {
//					ListEntry entry = new ListEntry(friends[i], R.drawable.profile_unknown, friends[i].getName(), friends[i].getImages().length == 0 ? ""
//							: friends[i].getURLforImageSize("extralarge")); // some
//																	// tracks
//																	// don't
//																	// have
//																	// images
//					iconifiedEntries.add(entry);
//				}
//				return iconifiedEntries;
//			} catch (Exception e) {
//				e.printStackTrace();
//			} catch (WSError e) {
//				LastFMApplication.getInstance().presentError(mContext, e);
//			}
//			return null;
//		}
//
//		@Override
//		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
//			if (iconifiedEntries != null) {
//				ListAdapter adapter = new ListAdapter(mContext, getImageCache());
//				adapter.setSourceIconified(iconifiedEntries);
//				mProfileLists[PROFILE_FRIENDS].setAdapter(adapter);
//			} else {
//				String[] strings = new String[] { getString(R.string.profile_nofriends) };
//				ListAdapter adapter = new ListAdapter(mContext, strings);
//				adapter.disableDisclosureIcons();
//				adapter.setDisabled();
//				mProfileLists[PROFILE_FRIENDS].setAdapter(adapter);
//			}
//			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
//																		// the
//																		// current
//																		// view
//			mNestedViewFlipper.setDisplayedChild(PROFILE_FRIENDS + 1);
//		}
//	}

}
