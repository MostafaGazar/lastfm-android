package fm.last.android.ui.fragment;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import fm.last.android.utils.ImageCache;

public class BaseArtistFragment extends BaseFragment {
	private static final String TAG = BaseArtistFragment.class.getSimpleName();
	
	private ImageCache mImageCache;
	
	protected String getArtistName(Intent intent) {
		if(intent.getData() != null) {
			if(intent.getData().getScheme().equals("http")) {
				List<String> segments = intent.getData().getPathSegments();
				
				return Uri.decode(segments.get(1)).replace("+", " ");
			}
		} else if(intent.getAction() != null) {
			if (intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_SEARCH)) {
				return intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST);
			}
		} else {
			return intent.getStringExtra("artist");
		}
		
		return "";
	}
	
	protected String getTrackName(Intent intent) {
		if(intent.getData() != null) {
			if(intent.getData().getScheme().equals("http")) {
				List<String> segments = intent.getData().getPathSegments();
				
				if(segments.size() > 3) {
					return Uri.decode(segments.get(3)).replace("+", " ");
				}
			}
		} else {
			return intent.getStringExtra("track");
		}
		
		return "";
	}
	
	protected ImageCache getImageCache() {
		if (mImageCache == null) {
			mImageCache = new ImageCache();
		}
		
		return mImageCache;
	}
	
}
