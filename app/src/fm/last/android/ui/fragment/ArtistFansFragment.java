package fm.last.android.ui.fragment;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.meg7.lastfm_neu.R;

import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.ui.adapter.ListAdapter;
import fm.last.android.ui.adapter.ListEntry;
import fm.last.android.ui.adapter.NotificationAdapter;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.api.LastFmServer;
import fm.last.api.User;
import fm.last.api.WSError;

public class ArtistFansFragment extends BaseArtistFragment {
	@SuppressWarnings("unused")
	private static final String TAG = ArtistFansFragment.class.getSimpleName();
	
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();
	
	private String mTrackName = "";
	private String mArtistName = "";
	
	private ListAdapter mFanAdapter;
	
	private View mProgressBarContainer;
	
	private ViewGroup mViewer;
	private ListView mFanList;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mArtistName = getArtistName(getActivity().getIntent());
		mTrackName = getTrackName(getActivity().getIntent());
		
		mViewer = (ViewGroup) inflater.inflate(R.layout.fragment_artist_fans,
				container, false);
		
		// Initiate controls.
//		mProgressBarContainer = viewer.findViewById(R.id.progressBarContainer);
		
		mFanList = (ListView) mViewer.findViewById(R.id.listeners_list_view);
		
		new LoadListenersTask().execute((Void) null);
		
		return mViewer;
	}
	
	private class LoadListenersTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public void onPreExecute() {
			mFanList.setAdapter(new NotificationAdapter(ArtistFansFragment.this.getActivity(), NotificationAdapter.LOAD_MODE, getString(R.string.common_loading)));
			mFanList.setOnItemClickListener(null);
		}

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				User[] fans;
				if(mTrackName != null)
					fans = mServer.getTrackTopFans(mTrackName, mArtistName, null);
				else
					fans = mServer.getArtistTopFans(mArtistName, null);

				if (fans.length == 0)
					return null;
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((fans.length < 10) ? fans.length : 10); i++) {
					ListEntry entry = new ListEntry(fans[i], R.drawable.profile_unknown, fans[i].getName(), fans[i].getURLforImageSize("extralarge"),
							R.drawable.list_icon_arrow);
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				mFanAdapter = new ListAdapter(ArtistFansFragment.this.getActivity(), getImageCache());
				mFanAdapter.setSourceIconified(iconifiedEntries);
				mFanList.setAdapter(mFanAdapter);
				mFanList.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> l, View v, int position, long id) {
						User user = (User) mFanAdapter.getItem(position);
						Intent profileIntent = new Intent(ArtistFansFragment.this.getActivity(), fm.last.android.ui.ProfileActivity.class);
						profileIntent.putExtra("lastfm.profile.username", user.getName());
						startActivity(profileIntent);
					}
				});
			} else {
				mFanList.setAdapter(new NotificationAdapter(ArtistFansFragment.this.getActivity(), NotificationAdapter.INFO_MODE, getString(R.string.metadata_nofans)));
			}
		}
	}
}
