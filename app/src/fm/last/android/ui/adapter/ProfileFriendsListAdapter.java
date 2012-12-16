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
import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import fm.last.android.R;
import fm.last.android.utils.ImageCache;
import fm.last.android.utils.ImageDownloader;
import fm.last.android.utils.ImageDownloaderListener;
import fm.last.api.User;

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
	private int mLoadingBar = -1;
	private boolean mEnabled = true;

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
			holder.label = (TextView) convertView.findViewById(R.id.row_label);
			holder.label_second = (TextView) convertView.findViewById(R.id.row_label_second);
			holder.image = (ImageView) convertView.findViewById(R.id.row_icon);
			holder.disclosure = (ImageView) convertView.findViewById(R.id.row_disclosure_icon);
			holder.vs = (ViewSwitcher) convertView.findViewById(R.id.row_view_switcher);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.label.setText(user.getName());

		// set disclosure image (if set)
		if (mLoadingBar == position) {
			holder.vs.setVisibility(View.VISIBLE);
			holder.disclosure.setImageResource(R.drawable.play);
		} else {
			holder.vs.setVisibility(View.GONE);
		}

		holder.vs.setDisplayedChild(mLoadingBar == position ? 1 : 0);

		// optionally if an URL is specified
		String url = user.getImages().length == 0 ? "" : user
				.getURLforImageSize("extralarge");
		if (url != null) {
			Bitmap bmp = mImageCache.get(url);
			if (bmp != null) {
				holder.image.setImageBitmap(bmp);
			} else {
				holder.image.setImageResource(R.drawable.profile_unknown);
			}
		}

		return convertView;
	}

	@Override
	public boolean isEnabled(int position) {
		return mEnabled;
	}

	/**
	 * Holder pattern implementation, performance boost
	 * 
	 * @author Lukasz Wisniewski
	 * @author Casey Link
	 */
	static class ViewHolder {
		TextView label;
		TextView label_second;
		ImageView image;
		ImageView disclosure;
		ViewSwitcher vs;
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
		}
	}

	public void imageDownloaded(String url) {
		this.notifyDataSetChanged();
	}

	/**
	 * Enables load bar at given position, at the same time only one can be
	 * launched per adapter
	 * 
	 * @param position
	 */
	public void enableLoadBar(int position) {
		this.mLoadingBar = position;
		notifyDataSetChanged();
	}

	/**
	 * Disables load bar
	 */
	public void disableLoadBar() {
		this.mLoadingBar = -1;
		notifyDataSetChanged();
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

	public void setDisabled() {
		mEnabled = false;
	}

	public void setContext(Activity context) {
		mContext = context;
	}

	public void refreshList() {
		setSource(mUsersArray);
	}

//	public void disableDisclosureIcons() {
//		for (User user : mUsersArray)
//			user.getdisclosure_id = -1;
//	}

}
