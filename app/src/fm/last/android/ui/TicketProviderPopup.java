/**
 * 
 */
package fm.last.android.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import fm.last.android.BaseListActivity;
import fm.last.android.R;
import fm.last.android.ui.adapter.ListAdapter;
import fm.last.android.ui.adapter.ListEntry;
import fm.last.android.utils.ImageCache;

/**
 * @author sam
 * 
 */
public class TicketProviderPopup extends BaseListActivity {
	private HashMap<String, String> mTicketUrls;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle icicle) {
		ListEntry entry;
		
		super.onCreate(icicle);
		setContentView(R.layout.popup);
		
		mTicketUrls = (HashMap<String, String>)(getIntent().getSerializableExtra("ticketurls"));
		
		ListAdapter adapter = new ListAdapter(this, new ImageCache());
		ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();

		if(mTicketUrls != null && mTicketUrls.size() > 0) {
			Iterator<Entry<String, String>> itr = mTicketUrls.entrySet().iterator();
			
			while(itr.hasNext()) {
				HashMap.Entry<String, String> pairs = (HashMap.Entry<String, String>)itr.next();
				
				entry = new ListEntry(R.drawable.shopping_cart_dark, R.drawable.shopping_cart_dark, (String)pairs.getKey());
				iconifiedEntries.add(entry);
			}
		}

		adapter.setSourceIconified(iconifiedEntries);
		adapter.setIconsUnscaled();
		setListAdapter(adapter);
		getListView().setDivider(new ColorDrawable(0xffd9d7d7));
	}

	@Override
	public void onResume() {
		((ListAdapter)getListAdapter()).disableLoadBar();
		super.onResume();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String provider = ((ListAdapter)getListAdapter()).getEntry(position).text;
		
		final Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(mTicketUrls.get(provider)));
		startActivity(myIntent);
		finish();
	}
}
