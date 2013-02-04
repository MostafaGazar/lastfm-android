package fm.last.android.ui.fragment;

import java.text.NumberFormat;
import java.util.Locale;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.LastFMApplication;
import fm.last.android.player.RadioPlayerService;
import fm.last.android.ui.adapter.EventListAdapter;
import fm.last.android.ui.adapter.NotificationAdapter;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.api.Artist;
import fm.last.api.Event;
import fm.last.api.ImageUrl;
import fm.last.api.LastFmServer;
import fm.last.api.WSError;
import com.meg7.lastfm_neu.R;

public class ArtistBioFragment extends BaseArtistFragment {
	@SuppressWarnings("unused")
	private static final String TAG = ArtistBioFragment.class.getSimpleName();
	
	private LastFmServer mServer = AndroidLastFmServerFactory.getServer();
	
	private String mBio;
	private String mArtistName = "";
	
	private View mProgressBarContainer;
	
	private ViewGroup mViewer;
	private WebView mWebView;
	private ImageButton mOntourButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mArtistName = getArtistName(getActivity().getIntent());
		
		mViewer = (ViewGroup) inflater.inflate(R.layout.fragment_artist_bio,
				container, false);
		
		// Initiate controls.
//		mProgressBarContainer = viewer.findViewById(R.id.progressBarContainer);
		
		mWebView = (WebView) mViewer.findViewById(R.id.webview);
		mOntourButton = (ImageButton) mViewer.findViewById(R.id.ontour);
		mOntourButton.setOnClickListener(mOntourListener);
		
		new LoadBioTask().execute((Void) null);
		new LoadEventsTask().execute((Void) null);
		
		return mViewer;
	}
	
	private View.OnClickListener mOntourListener = new View.OnClickListener() {

		public void onClick(View v) {
			try {
				LastFMApplication.getInstance().tracker.trackEvent("Clicks", // Category
						"on-tour-badge", // Action
						"", // Label
						0); // Value
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			
			// mTabHost.setCurrentTabByTag("events");		
		}

	};
	
	private class LoadBioTask extends AsyncTaskEx<Void, Void, Boolean> {
		@Override
		public void onPreExecute() {
			mWebView.loadData(getString(R.string.common_loading), "text/html", "utf-8");
		}

		@Override
		public Boolean doInBackground(Void... params) {
			Artist artist;
			boolean success = false;

			if(mArtistName == null || LastFMApplication.getInstance().session == null)
				return false;
			
			try {
				String lang = Locale.getDefault().getLanguage();
				if (lang.equalsIgnoreCase("de")) {
					artist = mServer.getArtistInfo(mArtistName, null, lang, LastFMApplication.getInstance().session.getName());
				} else {
					artist = mServer.getArtistInfo(mArtistName, null, null, LastFMApplication.getInstance().session.getName());
				}
				if (artist.getBio().getContent() == null || artist.getBio().getContent().trim().length() == 0) {
					// no bio in current locale -> get the English bio
					artist = mServer.getArtistInfo(mArtistName, null, null, LastFMApplication.getInstance().session.getName());
				}
				String imageURL = "";
				for (ImageUrl image : artist.getImages()) {
					if (image.getSize().contentEquals("large")) {
						imageURL = image.getUrl();
						break;
					}
				}

				String listeners = "";
				String plays = "";
				String userplaycount = "";
				try {
					NumberFormat nf = NumberFormat.getInstance();
					listeners = nf.format(Integer.parseInt(artist.getListeners()));
					plays = nf.format(Integer.parseInt(artist.getPlaycount()));
					userplaycount = nf.format(Integer.parseInt(artist.getUserPlaycount()));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}

				String stationbuttonbg = "url(data:image/png;base64,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%3D%3D)";
				
				String stationbuttonmediumstyle = "color: white;"
						+ "cursor: pointer;"
						+ "display: block;"
						+ "font-size: 11px;"
						+ "font-weight: bold;"
						+ "height: 25px;"
						+ "line-height: 25px;"
						+ "margin: 0px 10px 10px 0px;"
						+ "max-width: 180px;"
						+ "text-decoration: none;"
						+ "overflow: hidden;"
						+ "padding-left: 30px;"
						+ "position: relative;"
						+ "background: " + stationbuttonbg + " top left no-repeat";

				String stationbuttonspanstyle = "position: relative;"
					+ "display: block;"
					+ "padding: 0 10px 0 2px;"
					+ "background: " + stationbuttonbg + " right top no-repeat;"
					+ "height: 25px;";
				
				mBio = "<html><body style='margin:0; padding:0; color:black; background: white; font-family: Helvetica; font-size: 11pt;'>"
						+ "<div style='padding:17px; margin:0; top:0px; left:0px; position:absolute;'>" + "<img src='" + imageURL
						+ "' style='margin-top: 4px; float: left; margin-right: 0px; margin-bottom: 14px; width:64px; border:1px solid gray; padding: 1px;'/>"
						+ "<div style='margin-left:84px; margin-top:3px'>" + "<span style='font-size: 15pt; font-weight:bold; padding:0px; margin:0px;'>"
						+ mArtistName + "</span><br/>" + "<span style='color:gray; font-weight: normal; font-size: 10pt;'>" + listeners + " "
						+ getString(R.string.metadata_listeners) + "<br/>" + plays + " " + getString(R.string.metadata_plays);
				if(userplaycount.length() > 0 && !userplaycount.equals("0"))
					mBio += "<br/>" + userplaycount + " " + getString(R.string.metadata_userplays);

				mBio += "</span>";

				if(RadioPlayerService.radioAvailable(ArtistBioFragment.this.getActivity()))
					mBio += "<br/> <a style='"+ stationbuttonmediumstyle + "' href='lastfm://artist/" + Uri.encode(artist.getName()).replace("'", "%27") + "'>"
							+ "<span style='" + stationbuttonspanstyle + "'>Play " + artist.getName() + " Radio</span></a>";
				mBio += "</div><br style='clear:both;'/>" + formatBio(artist.getBio().getContent()) + "</div></body></html>";

				success = true;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				e.printStackTrace();
			}
			return success;
		}

		private String formatBio(String wikiText) {
			// last.fm api returns the wiki text without para formatting,
			// correct that:
			return wikiText.replaceAll("\\n+", "<br>");
		}

		@Override
		public void onPostExecute(Boolean result) {
			if (result) {
				try {
					mWebView.loadDataWithBaseURL(null, new String(mBio.getBytes(), "utf-8"), // need
																								// to
																								// do
																								// this,
																								// but
																								// is
																								// there
																								// a
																								// better
																								// way?
							"text/html", "utf-8", null);
					// request focus to make the web view immediately scrollable
					mWebView.requestFocus();
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			mWebView.loadData(getString(R.string.metadata_nobio), "text/html", "utf-8");
		}
	}
	
	private class LoadEventsTask extends AsyncTaskEx<Void, Void, Boolean> {

		/**
		 * New adapter representing events data
		 */
		private BaseAdapter mNewEventAdapter;

		@Override
		public void onPreExecute() {
			mOntourButton.setVisibility(View.GONE);
			mOntourButton.invalidate();
		}

		@Override
		public Boolean doInBackground(Void... params) {
			boolean result = false;

			mNewEventAdapter = new EventListAdapter(ArtistBioFragment.this.getActivity());

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
				mNewEventAdapter = new NotificationAdapter(ArtistBioFragment.this.getActivity(), NotificationAdapter.INFO_MODE, getString(R.string.metadata_noevents));
			}

			return result;
		}

		@Override
		public void onPostExecute(Boolean result) {
			if (result) {
				mOntourButton.setVisibility(View.VISIBLE);
			}
		}
	}
	
}
