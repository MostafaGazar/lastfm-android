package fm.last.android.ui.fragment;

import java.util.ArrayList;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.meg7.lastfm_neu.R;

import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.LastFMApplication;
import fm.last.android.player.RadioPlayerService;
import fm.last.android.ui.adapter.ListAdapter;
import fm.last.android.ui.adapter.ListEntry;
import fm.last.android.ui.adapter.NotificationAdapter;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.api.LastFmServer;
import fm.last.api.Tag;
import fm.last.api.WSError;

public class ArtistTagsFragment extends BaseArtistFragment {
	@SuppressWarnings("unused")
	private static final String TAG = ArtistTagsFragment.class.getSimpleName();
	
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();
	
	private String mArtistName = "";
	private String mTrackName = "";
	
	private ListAdapter mTagAdapter;
	
	private View mProgressBarContainer;
	
	private ViewGroup mViewer;
	private ListView mTagList;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mArtistName = getArtistName(getActivity().getIntent());
		mTrackName = getTrackName(getActivity().getIntent());
		
		mViewer = (ViewGroup) inflater.inflate(R.layout.fragment_artist_tags,
				container, false);
		
		// Initiate controls.
//		mProgressBarContainer = viewer.findViewById(R.id.progressBarContainer);
		mTagList = (ListView) mViewer.findViewById(R.id.tags_list_view);
		
		if(RadioPlayerService.radioAvailable(getActivity())) {
			new LoadTagsTask().execute((Void) null);
		}
		
		return mViewer;
	}
	
	private class LoadTagsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public void onPreExecute() {
			mTagList.setAdapter(new NotificationAdapter(ArtistTagsFragment.this.getActivity(), NotificationAdapter.LOAD_MODE, getString(R.string.common_loading)));
			mTagList.setOnItemClickListener(null);
		}

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				Tag[] tags;
				if(mTrackName != null)
					tags = mServer.getTrackTopTags(mArtistName, mTrackName, null);
				else
					tags = mServer.getArtistTopTags(mArtistName, null);
				if (tags.length == 0)
					return null;
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((tags.length < 10) ? tags.length : 10); i++) {
					ListEntry entry = new ListEntry(tags[i], R.drawable.list_icon_station, tags[i].getName());
					entry.centerIcon = true;
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
				mTagAdapter = new ListAdapter(ArtistTagsFragment.this.getActivity(), getImageCache());
				mTagAdapter.setSourceIconified(iconifiedEntries);
				mTagList.setAdapter(mTagAdapter);
				mTagList.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> l, View v, int position, long id) {
						Tag tag = (Tag) mTagAdapter.getItem(position);
						mTagAdapter.enableLoadBar(position);
						LastFMApplication.getInstance().playRadioStation(ArtistTagsFragment.this.getActivity(), "lastfm://globaltags/" + Uri.encode(tag.getName()), true);
					}

				});
			} else {
				mTagList.setAdapter(new NotificationAdapter(ArtistTagsFragment.this.getActivity(), NotificationAdapter.INFO_MODE, getString(R.string.metadata_notags)));
			}
		}
	}
	
}
