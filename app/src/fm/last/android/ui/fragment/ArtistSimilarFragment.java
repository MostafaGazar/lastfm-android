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
import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.ui.ArtistActivity;
import fm.last.android.ui.adapter.ListAdapter;
import fm.last.android.ui.adapter.ListEntry;
import fm.last.android.ui.adapter.NotificationAdapter;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.api.Artist;
import fm.last.api.LastFmServer;
import fm.last.api.WSError;
import com.meg7.lastfm_neu.R;

public class ArtistSimilarFragment extends BaseArtistFragment {
	@SuppressWarnings("unused")
	private static final String TAG = ArtistSimilarFragment.class.getSimpleName();
	
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();
	
	private String mArtistName = "";
	
	private ListAdapter mSimilarAdapter;
	
	private View mProgressBarContainer;
	
	private ViewGroup mViewer;
	private ListView mSimilarList;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mArtistName = getArtistName(getActivity().getIntent());
		
		mViewer = (ViewGroup) inflater.inflate(R.layout.fragment_artist_similar,
				container, false);
		
		// Initiate controls.
//		mProgressBarContainer = viewer.findViewById(R.id.progressBarContainer);
		mSimilarList = (ListView) mViewer.findViewById(R.id.similar_list_view);
		
		
		new LoadSimilarTask().execute((Void) null);
		
		return mViewer;
	}

	private class LoadSimilarTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public void onPreExecute() {
			mSimilarList.setOnItemClickListener(null);
			mSimilarList.setAdapter(new NotificationAdapter(ArtistSimilarFragment.this.getActivity(), NotificationAdapter.LOAD_MODE, getString(R.string.common_loading)));
		}

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {

			try {
				Artist[] similar = mServer.getSimilarArtists(mArtistName, null);
				if (similar.length == 0)
					return null;

				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((similar.length < 10) ? similar.length : 10); i++) {
					ListEntry entry = new ListEntry(similar[i], R.drawable.artist_icon, similar[i].getName(), similar[i].getURLforImageSize("extralarge"));
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
				mSimilarAdapter = new ListAdapter(ArtistSimilarFragment.this.getActivity(), getImageCache());
				mSimilarAdapter.setSourceIconified(iconifiedEntries);
				mSimilarList.setAdapter(mSimilarAdapter);
				mSimilarList.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> l, View v, int position, long id) {
						Artist artist = (Artist) mSimilarAdapter.getItem(position);
						Intent i = new Intent(ArtistSimilarFragment.this.getActivity(), ArtistActivity.class);
						i.putExtra("artist", artist.getName());
						startActivity(i);
					}

				});
			} else {
				mSimilarList.setAdapter(new NotificationAdapter(ArtistSimilarFragment.this.getActivity(), NotificationAdapter.INFO_MODE, getString(R.string.metadata_nosimilar)));
			}
		}
	}
	
}
