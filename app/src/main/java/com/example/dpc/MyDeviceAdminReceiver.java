package com.example.dpc;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.app.admin.FactoryResetProtectionPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.widget.Toast;
import java.util.Collections;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    public static final String ACTION_APPLY_COPE = "com.example.dpc.ACTION_APPLY_COPE";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);        
        applyCopePolicies(context);        
    }

    private void applyCopePolicies(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);
        DevicePolicyManager parentDpm = dpm.getParentProfileInstance(admin);

        try {
            if (Build.VERSION.SDK_INT >= 31) {
                dpm.setUsbDataSignalingEnabled(false);
            }

            parentDpm.setMaximumFailedPasswordsForWipe(admin, 3);

            if (Build.VERSION.SDK_INT >= 30) {
                FactoryResetProtectionPolicy frpPolicy = new FactoryResetProtectionPolicy.Builder()
                        .setFactoryResetProtectionAccounts(Collections.emptyList())
                        .setFactoryResetProtectionEnabled(false)
                        .build();
                dpm.setFactoryResetProtectionPolicy(admin, frpPolicy);
            } else {
                Bundle restrictions = new Bundle();
                restrictions.putBoolean("disableFactoryResetProtectionAdmin", true);
                dpm.setApplicationRestrictions(admin, "com.google.android.gms", restrictions);
            }

            Intent gmsIntent = new Intent("com.google.android.gms.auth.FRP_CONFIG_CHANGED");
            gmsIntent.setPackage("com.google.android.gms");
            context.sendBroadcast(gmsIntent);

            int flags = DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS | 
                        DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS;
            parentDpm.setKeyguardDisabledFeatures(admin, flags);

            parentDpm.addUserRestriction(admin, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);

            Toast.makeText(context, "Начальные запреты COPE применены!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка при настройке: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
