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

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.LastFMApplication;
import fm.last.android.ui.Event.EventActivityResult;
import fm.last.android.ui.adapter.EventListAdapter;
import fm.last.android.ui.adapter.ListAdapter;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.api.Event;
import fm.last.api.LastFmServer;
import fm.last.api.WSError;
import fm.last.neu.R;

public class ProfileEventsFragment extends BaseFragment implements LocationListener {
	// Java doesn't let you treat enums as ints easily, so we have to have this
	// mess
	private static final int EVENTS_MYEVENTS = 0;
	private static final int EVENTS_RECOMMENDED = 1;
	private static final int EVENTS_NEARME = 2;

	private Activity mContext;
	private ViewGroup mViewer;
	
	public static String username; // store this separate so we have access to it
								// before User obj is retrieved
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();

	private boolean mIsMyEventsLoaded;
	private boolean mIsRecommendedLoaded;
	private boolean mIsNearMeLoaded;
	private View mProgressBar;
	private View mListsContainer;
	private ListView[] mEventsLists = new ListView[3];

	private EventActivityResult mOnEventActivityResult;

	private Location mLocation = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mViewer = (ViewGroup) inflater.inflate(R.layout.events,
				container, false);
		mContext = getActivity();
		
//		username = savedInstanceState.getString("user");

		mProgressBar = mViewer.findViewById(R.id.progressBar);
		mListsContainer = mViewer.findViewById(R.id.listsContainer);

		mEventsLists[EVENTS_MYEVENTS] = (ListView) mViewer.findViewById(R.id.myevents_list_view);
		mEventsLists[EVENTS_MYEVENTS].setOnItemClickListener(mEventItemClickListener);

		mEventsLists[EVENTS_RECOMMENDED] = (ListView) mViewer.findViewById(R.id.recommended_list_view);
		mEventsLists[EVENTS_RECOMMENDED].setOnItemClickListener(mEventItemClickListener);

		mEventsLists[EVENTS_NEARME] = (ListView) mViewer.findViewById(R.id.nearme_list_view);
		mEventsLists[EVENTS_NEARME].setOnItemClickListener(mEventItemClickListener);

		// Load list contents.
		refreshData();
		
		return mViewer;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			int status = data.getExtras().getInt("status", -1);
			if (mOnEventActivityResult != null && status != -1) {
				mOnEventActivityResult.onEventStatus(status);
			}
		}
	}
	
	@Override
	public void onPause() {
		LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(this);
		super.onPause();
	}

	public void refreshData() {
		mIsMyEventsLoaded = false;
		mIsRecommendedLoaded = false;
		mIsNearMeLoaded = false;
		
		mListsContainer.setVisibility(View.GONE);
		mProgressBar.setVisibility(View.VISIBLE);
		
		// EVENTS_MYEVENTS:
		try {
			LastFMApplication.getInstance().tracker.trackPageView("/Profile/Events");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}
		new LoadMyEventsTask().execute((Void) null);
	
		// EVENTS_RECOMMENDED:
		try {
			LastFMApplication.getInstance().tracker.trackPageView("/Profile/Events/Recommended");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}
		new LoadRecommendedEventsTask().execute((Void) null);
	
		// EVENTS_NEARME:
		try {
			LastFMApplication.getInstance().tracker.trackPageView("/Profile/Events/Nearby");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}
		LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		
		Criteria criteria = new Criteria(); 
		criteria.setPowerRequirement(Criteria.POWER_LOW); 
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); 
		criteria.setAltitudeRequired(false); 
		criteria.setBearingRequired(false); 
		criteria.setSpeedRequired(false); 
		criteria.setCostAllowed(false); 

		String provider = lm.getBestProvider(criteria, true);
		if(provider != null) {
			mLocation = lm.getLastKnownLocation(provider);
			lm.requestLocationUpdates(provider, 30000L, 1000.0f, this);
		}
		new LoadNearbyEventsTask().execute((Void) null);
	}
	
	private void showLists() {
		if (mIsMyEventsLoaded && mIsRecommendedLoaded && mIsNearMeLoaded) {
			mListsContainer.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.GONE);
		}
	}

	private OnItemClickListener mEventItemClickListener = new OnItemClickListener() {

		public void onItemClick(final AdapterView<?> parent, final View v, final int position, long id) {
			try {
				final Event event = (Event) parent.getAdapter().getItem(position);
				mOnEventActivityResult = new EventActivityResult() {
					public void onEventStatus(int status) {
						event.setStatus(String.valueOf(status));
						mOnEventActivityResult = null;
					}
				};
				startActivityForResult(fm.last.android.ui.Event.intentFromEvent(mContext, event), 0);
			} catch (ClassCastException e) {
				// when the list item is not an event
			}
		}

	};

	private class LoadMyEventsTask extends AsyncTaskEx<Void, Void, EventListAdapter> {

		@Override
		public EventListAdapter doInBackground(Void... params) {
			try {
				fm.last.api.Event[] events = mServer.getUserEvents(username);
				if (events.length > 0) {
					EventListAdapter result = new EventListAdapter(mContext);
					result.setEventsSource(events);
					return result;
				}
			} catch (WSError e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(EventListAdapter result) {
			if (result != null) {
				mEventsLists[EVENTS_MYEVENTS].setAdapter(result);
				
				mViewer.findViewById(R.id.myevents_header).setVisibility(View.GONE);
			} else {
				String[] strings = new String[] { getString(R.string.profile_noevents) };
				ListAdapter adapter = new ListAdapter(mContext, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				
				// Show list header.
				mViewer.findViewById(R.id.myevents_header).setVisibility(View.VISIBLE);
				
				mEventsLists[EVENTS_MYEVENTS].setAdapter(adapter);
			}
			
			mIsMyEventsLoaded = true;
			showLists();
		}
	}
	
	private class LoadRecommendedEventsTask extends AsyncTaskEx<Void, Void, EventListAdapter> {

		@Override
		public EventListAdapter doInBackground(Void... params) {

			try {
				fm.last.api.Event[] events = mServer.getUserRecommendedEvents(username, LastFMApplication.getInstance().session.getKey());
				if (events.length > 0) {
					EventListAdapter result = new EventListAdapter(mContext);
					result.setEventsSource(events);
					return result;
				}
			} catch (WSError e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(EventListAdapter result) {
			if (result != null) {
				mEventsLists[EVENTS_RECOMMENDED].setAdapter(result);
				
				mViewer.findViewById(R.id.recommended_header).setVisibility(View.GONE);
			} else {
				String[] strings = new String[] { getString(R.string.profile_noevents) };
				ListAdapter adapter = new ListAdapter(mContext, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				
				// Add list header.
				mViewer.findViewById(R.id.recommended_header).setVisibility(View.VISIBLE);
				
				mEventsLists[EVENTS_RECOMMENDED].setAdapter(adapter);
			}
			
			mIsRecommendedLoaded = true;
			showLists();
		}
		
	}
	
	private class LoadNearbyEventsTask extends AsyncTaskEx<Void, Void, EventListAdapter> {

		@Override
		public EventListAdapter doInBackground(Void... params) {
			try {
				if(mLocation != null) {
					String latitude = String.valueOf(mLocation.getLatitude());
					String longitude = String.valueOf(mLocation.getLongitude());
					fm.last.api.Event[] events = mServer.getNearbyEvents(latitude, longitude);
					if (events.length > 0) {
						EventListAdapter result = new EventListAdapter(mContext);
						result.setEventsSource(events);
						return result;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (WSError e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(EventListAdapter result) {
			if (result != null) {
				mEventsLists[EVENTS_NEARME].setAdapter(result);
				
				mViewer.findViewById(R.id.nearme_header).setVisibility(View.GONE);
			} else {
				String[] strings = new String[] { getString(R.string.profile_noevents) };
				ListAdapter adapter = new ListAdapter(mContext, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				
				// Add list header.
				mViewer.findViewById(R.id.nearme_header).setVisibility(View.VISIBLE);
				
				mEventsLists[EVENTS_NEARME].setAdapter(adapter);
			}
			
			mIsNearMeLoaded = true;
			showLists();
		}
	}

	public void onLocationChanged(Location location) {
		if(location != null) {
			mLocation = location;
			new LoadNearbyEventsTask().execute((Void) null);
		}
	}

	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
}
