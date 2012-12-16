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
package fm.last.android.ui.adapter;

import java.io.IOException;
import java.io.Serializable;

import android.app.Activity;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import fm.last.android.utils.ImageCache;
import fm.last.android.utils.ImageDownloader;
import fm.last.android.utils.ImageDownloaderListener;
import fm.last.api.User;
import fm.last.neu.R;

/**
 * Simple adapter for presenting ArrayList of IconifiedEntries as ListView,
 * allows icon customization
 */
public class ProfileFriendsListAdapter extends BaseAdapter implements
		Serializable, ImageDownloaderListener {

	private static final long serialVersionUID = 2679887824070220768L;
	protected transient ImageCache mImageCache;
	protected transient ImageDownloader mImageDownloader;
	protected transient Activity mContext;

	private User[] mUsersArray;

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(mUsersArray);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		try {
			mUsersArray = (User[]) in.readObject();
		} catch (ClassCastException e) {
			mUsersArray = null;
		}
	}

	public ProfileFriendsListAdapter(Activity context) {
		mContext = context;
	}

	/**
	 * Default constructor
	 * 
	 * @param context
	 * @param imageCache
	 */
	public ProfileFriendsListAdapter(Activity context, ImageCache imageCache) {
		mContext = context;
		setImageCache(imageCache);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		User user = mUsersArray[position];
		
		ViewHolder holder;

		if (convertView == null) {
			LayoutInflater inflater = mContext.getLayoutInflater();
			convertView = inflater.inflate(R.layout.list_item_profile_friend, null);

			holder = new ViewHolder();
			holder.user_name = (TextView) convertView.findViewById(R.id.user_name);
			holder.user_details = (TextView) convertView.findViewById(R.id.user_details);
			holder.user_icon = (ImageView) convertView.findViewById(R.id.user_icon);
			
			holder.progress_bar = convertView.findViewById(R.id.progress_bar);
			holder.user_last_track = (ImageView) convertView.findViewById(R.id.user_last_track);
			holder.user_last_track_title = (TextView) convertView.findViewById(R.id.user_last_track_title);
			holder.user_last_track_artist = (TextView) convertView.findViewById(R.id.user_last_track_artist);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.user_name.setText(user.getName());
		StringBuffer userDetailsBuffer = new StringBuffer();
		if (!TextUtils.isEmpty(user.getAge())) {
			userDetailsBuffer.append(user.getAge());
		}
		if (!TextUtils.isEmpty(user.getGender().toString())) {
			if (!TextUtils.isEmpty(user.getAge())) {
				userDetailsBuffer.append(", ");				
			}
			userDetailsBuffer.append(user.getGender().toString().toLowerCase());
		}
		if (user.getCountry() != null && !TextUtils.isEmpty(user.getCountry().getDisplayCountry())) {
			if (!TextUtils.isEmpty(user.getAge())
					|| !TextUtils.isEmpty(user.getGender().toString())) {
				userDetailsBuffer.append(", ");
			}
			userDetailsBuffer.append(user.getCountry().getDisplayCountry());
		}
		holder.user_details.setText(userDetailsBuffer.toString());
		String url = user.getImages().length == 0 ? "" : user
				.getURLforImageSize("extralarge");
		if (url != null) {
			Bitmap bmp = mImageCache.get(url);
			if (bmp != null) {
				holder.user_icon.setImageBitmap(bmp);
			} else {
				holder.user_icon.setImageResource(R.drawable.profile_unknown);
			}
		}
		
		if (user.getRecentTrack() != null) {
			int length = user.getRecentTrack().getImages().length;
			url = length == 0 ? "" : user.getRecentTrack().getImages()[length - 1].getUrl();
			if (url != null) {
				Bitmap bmp = mImageCache.get(url);
				if (bmp != null) {
					holder.user_last_track.setImageBitmap(bmp);
					holder.progress_bar.setVisibility(View.GONE);
				} else {
					holder.user_last_track.setImageResource(R.color.transparent_lastfm);
				}
			} else {
				holder.progress_bar.setVisibility(View.GONE);
			}
			
			holder.user_last_track_title.setText(user.getRecentTrack().getName());
			holder.user_last_track_artist.setText(user.getRecentTrack().getArtist().getName());
		}

		return convertView;
	}

	public void setSource(User[] usersArray) {
		mUsersArray = usersArray;
		if (usersArray == null) {
			return;
		}
		
		for(User user : usersArray) {
			String url = user.getImages().length == 0 ? "" : user
					.getURLforImageSize("extralarge");
			if (url != null) {
				try {
					if (mImageDownloader.getAsyncTaskEx(url) == null) {
						mImageDownloader.getImage(url);
					}
				} catch (java.util.concurrent.RejectedExecutionException e) {
					e.printStackTrace();
				}
			}
			
			if (user.getRecentTrack() != null) {
				int length = user.getRecentTrack().getImages().length;
				url = length == 0 ? "" : user.getRecentTrack().getImages()[length - 1].getUrl();
				if (url != null) {
					try {
						if (mImageDownloader.getAsyncTaskEx(url) == null) {
							mImageDownloader.getImage(url);
						}
					} catch (java.util.concurrent.RejectedExecutionException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void imageDownloaded(String url) {
		this.notifyDataSetChanged();
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	public int getCount() {
		if (mUsersArray != null)
			return mUsersArray.length;
		else
			return 0;
	}

	public Object getItem(int position) {
		return mUsersArray[position];
	}
	
	public User getEntry(int position) {
		return mUsersArray[position];
	}

	public long getItemId(int position) {
		return position;
	}

	public void setImageCache(ImageCache imageCache) {
		mImageDownloader = new ImageDownloader(imageCache);
		mImageDownloader.setListener(this);
		mImageCache = imageCache;
	}

	public void setContext(Activity context) {
		mContext = context;
	}

	public void refreshList() {
		setSource(mUsersArray);
	}

	static class ViewHolder {
		TextView user_name;
		TextView user_details;
		ImageView user_icon;
		
		View progress_bar;
		ImageView user_last_track;
		TextView user_last_track_title;
		TextView user_last_track_artist;
	}

}
