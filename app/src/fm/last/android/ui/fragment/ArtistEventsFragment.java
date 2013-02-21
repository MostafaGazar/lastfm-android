package fm.last.android.ui.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.meg7.lastfm_neu.R;

import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.LastFMApplication;
import fm.last.android.ui.Event.EventActivityResult;
import fm.last.android.ui.adapter.EventListAdapter;
import fm.last.android.ui.adapter.NotificationAdapter;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.api.Event;
import fm.last.api.LastFmServer;
import fm.last.api.WSError;

public class ArtistEventsFragment extends BaseArtistFragment {
	@SuppressWarnings("unused")
	private static final String TAG = ArtistEventsFragment.class.getSimpleName();
	
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();
	
	private String mArtistName = "";
	private EventActivityResult mOnEventActivityResult;
	
	private BaseAdapter mEventAdapter;

	private ProgressDialog mProgressDialog;
	private View mProgressBarContainer;
	
	private ViewGroup mViewer;
	private ListView mEventList;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mArtistName = getArtistName(getActivity().getIntent());
		
		mViewer = (ViewGroup) inflater.inflate(R.layout.fragment_artist_events,
				container, false);
		
		// Initiate controls.
//		mProgressBarContainer = viewer.findViewById(R.id.progressBarContainer);
		mEventList = (ListView) mViewer.findViewById(R.id.events_list_view);
		
		new LoadEventsTask().execute((Void) null);
		
		return mViewer;
	}
	
	/**
	 * This load task is slightly bigger as it has to handle OnTour indicator
	 * and Metadata's event list. The main problem here is new events must be
	 * downloaded on track change even if the user is viewing old events in the
	 * metadata view.
	 * 
	 * @author Lukasz Wisniewski
	 */
	private class LoadEventsTask extends AsyncTaskEx<Void, Void, Boolean> {

		/**
		 * New adapter representing events data
		 */
		private BaseAdapter mNewEventAdapter;

		@Override
		public void onPreExecute() {
			mEventList.setOnItemClickListener(null);
			mEventList.setAdapter(new NotificationAdapter(ArtistEventsFragment.this.getActivity(), NotificationAdapter.LOAD_MODE, getString(R.string.common_loading)));
//			mOntourButton.setVisibility(View.GONE);
//			mOntourButton.invalidate();
		}

		@Override
		public Boolean doInBackground(Void... params) {
			boolean result = false;

			mNewEventAdapter = new EventListAdapter(ArtistEventsFragment.this.getActivity());

			try {
				Event[] events = mServer.getArtistEvents(mArtistName);
				((EventListAdapter) mNewEventAdapter).setEventsSource(events);
				if (events.length > 0)
					result = true;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
			}

			if (!result) {
				mNewEventAdapter = new NotificationAdapter(
						getActivity(),
						NotificationAdapter.INFO_MODE,
						getString(R.string.metadata_noevents));
				mEventList.setOnItemClickListener(null);
			}

			return result;
		}

		@Override
		public void onPostExecute(Boolean result) {
			mEventAdapter = mNewEventAdapter;
			mEventList.setAdapter(mEventAdapter);
			if (result) {
				mEventList.setOnItemClickListener(mEventOnItemClickListener);
//				mOntourButton.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private class ShowEventTask extends AsyncTaskEx<Void, Void, Intent> {
		Event event;
		
		public ShowEventTask(Event e) {
			event = e;
		}
		
		@Override
		public void onPreExecute() {
			mProgressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.loading), true, true, new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
					ShowEventTask.this.cancel(true);					
				}
			});
		}

		@Override
		public Intent doInBackground(Void... params) {
			Intent intent = fm.last.android.ui.Event.intentFromEvent(ArtistEventsFragment.this.getActivity(), event);
			try {
				Event[] events = mServer.getUserEvents((LastFMApplication.getInstance().session).getName());
				for (Event e : events) {
					if (e.getId() == event.getId()) {
						intent.putExtra("lastfm.event.status", e.getStatus());
						break;
					}

				}
			} catch (WSError e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return intent;
		}

		@Override
		public void onPostExecute(Intent intent) {
			if (mProgressDialog != null) {
				mOnEventActivityResult = new EventActivityResult() {
					public void onEventStatus(int status) {
						event.setStatus(String.valueOf(status));
						mOnEventActivityResult = null;
					}
				};
				startActivityForResult(intent, 0);

				mProgressDialog.dismiss();
			}
		}
	}
	
	private OnItemClickListener mEventOnItemClickListener = new OnItemClickListener() {

		public void onItemClick(final AdapterView<?> parent, final View v, final int position, long id) {
			final Event event = (Event) parent.getAdapter().getItem(position);
			new ShowEventTask(event).execute((Void)null);
		}

	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			int status = data.getExtras().getInt("status", -1);
			if (mOnEventActivityResult != null && status != -1) {
				mOnEventActivityResult.onEventStatus(status);
			}
		}
	}
	
}
