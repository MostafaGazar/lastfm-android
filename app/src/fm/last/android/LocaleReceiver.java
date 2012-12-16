/**
 * 
 */
package fm.last.android;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import fm.last.android.player.RadioPlayerService;
import fm.last.android.ui.PlayerActivity;

/**
 * @author sam
 *
 */
public class LocaleReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if(RadioPlayerService.radioAvailable(context)) {
			context.getPackageManager().setComponentEnabledSetting(new ComponentName("fm.last.neu", PlayerActivity.class.getName()), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
		} else {
			context.getPackageManager().setComponentEnabledSetting(new ComponentName("fm.last.neu", PlayerActivity.class.getName()), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
		}
	}

}
