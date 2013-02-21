/***************************************************************************
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, Michael Novak Jr, and Mostafa Gazar.				   *
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

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.meg7.lastfm_neu.R;

import fm.last.android.BaseListActivity;
import fm.last.android.LastFMApplication;
import fm.last.android.SearchProvider;
import fm.last.android.ui.adapter.ListAdapter;
import fm.last.android.ui.adapter.ListEntry;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.android.utils.ImageCache;

public class SearchActivity extends BaseListActivity {
	
	private String mQuery;
	
	private ImageCache mImageCache;
	
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.search);

		mImageCache = new ImageCache();
		
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
		}
		
		mQuery = getIntent().getStringExtra(SearchManager.QUERY);
		if(mQuery != null) {
			new SearchTask().execute((Void)null);
			
			setTitle(mQuery);
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234 && resultCode == RESULT_OK) {
			new SearchTask().execute((Void)null);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ListAdapter a = (ListAdapter)getListAdapter();
		
		if(a != null) {
			LastFMApplication.getInstance().mCtx = this;
			String URI = (String)a.getItem(position);
			Intent i = new Intent(this, ProfileActivity.class);
			i.setData(Uri.parse(URI));
			startActivity(i);
		}
	}
	
	@Override
	public boolean onSearchRequested() {
	     return true;
	 }
	
	private class SearchTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public void onPreExecute() {
			String[] strings = new String[] { "Searching" };
			ListAdapter adapter = new ListAdapter(SearchActivity.this, strings);

			adapter.disableDisclosureIcons();
			adapter.setDisabled();
			adapter.enableLoadBar(0);
			setListAdapter(adapter);
			getListView().setVisibility(View.VISIBLE);
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				Cursor managedCursor = managedQuery(Uri.withAppendedPath(
						SearchProvider.SUGGESTIONS_URI,
						Uri.encode(mQuery).replace(
								"/", "%2f")), null, null, null, null);
				if (managedCursor.getCount() == 0)
					return null;
				
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				while(managedCursor.moveToNext()) {
					String text1 = managedCursor.getString(managedCursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1));
					String text2 = managedCursor.getString(managedCursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_2));
					String value = managedCursor.getString(managedCursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_INTENT_DATA));
					String imageURL = managedCursor.getString(managedCursor.getColumnIndexOrThrow("_imageURL"));
					
					ListEntry entry;
					if(!imageURL.equals("-1"))
						entry = new ListEntry(value, R.drawable.profile_unknown, text1, imageURL, text2);
					else
						entry = new ListEntry(value, R.drawable.list_icon_station, text1, imageURL, text2);
					entry.centerIcon = true;
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			ListAdapter adapter;
			
			if (iconifiedEntries != null) {
				adapter = new ListAdapter(SearchActivity.this, mImageCache);
				adapter.setSourceIconified(iconifiedEntries);
			} else {
				String[] strings = new String[] { getString(R.string.newstation_noresults) };
				adapter = new ListAdapter(SearchActivity.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
			}
			setListAdapter(adapter);
			getListView().setVisibility(View.VISIBLE);
		}
	}

}