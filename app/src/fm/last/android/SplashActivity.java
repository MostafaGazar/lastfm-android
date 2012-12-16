package fm.last.android;

import fm.last.neu.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * @author Mostafa Gazar
 */
public class SplashActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.splash);
		
		doInit();
	}

	protected void startNextActivity() {
		Intent intent = new Intent(this, LastFm.class);
		this.startActivity(intent);
		this.finish();
	}

	protected void doInit() {
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				startNextActivity();
			}
		}, 2000);
	}

}