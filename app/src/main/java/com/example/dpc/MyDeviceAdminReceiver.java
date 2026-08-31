package com.example.dpc;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.app.admin.FactoryResetProtectionPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import java.util.Collections;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    public static final String ACTION_APPLY_COPE = "com.example.dpc.ACTION_APPLY_COPE";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_APPLY_COPE.equals(intent.getAction()) || 
            ACTION_PROFILE_PROVISIONING_COMPLETE.equals(intent.getAction())) {
            
            applyCopePolicies(context);
        }
    }

    private void applyCopePolicies(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);
        DevicePolicyManager parentDpm = dpm.getParentProfileInstance(admin);

        try {
            // 1. Отключение передачи данных по USB (Android 12+)
            if (Build.VERSION.SDK_INT >= 31) {
                parentDpm.setUsbDataSignalingEnabled(false);
            }

            // 2. Сброс устройства после 3 неверных попыток ввода пароля
            parentDpm.setMaximumFailedPasswordsForWipe(admin, 3);

            // 3. Отключение FRP (Работает в COPE начиная с Android 12)
            if (Build.VERSION.SDK_INT >= 31) {
                FactoryResetProtectionPolicy frpPolicy = new FactoryResetProtectionPolicy.Builder()
                        .setFactoryResetProtectionAccounts(Collections.emptyList())
                        .setFactoryResetProtectionEnabled(false)
                        .build();
                dpm.setFactoryResetProtectionPolicy(admin, frpPolicy);
            }

            Toast.makeText(context, "COPE политики успешно применены!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка COPE: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
