package com.example.dpc;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView tvStatus;
    private TextView tvLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        String pkg = getPackageName();
        String admin = pkg + "/" + MyDeviceAdminReceiver.class.getName();

        // Проверяем, находимся ли мы в рабочем профиле (User ID != 0)
        boolean isWorkProfile = Process.myUserHandle().hashCode() != 0;

        if (!isWorkProfile) {
            // === РЕЖИМ ОСНОВНОГО ПОЛЬЗОВАТЕЛЯ (USER 0) ===
            TextView tvTitle = new TextView(this);
            tvTitle.setText("Запустите команду в aShell:");
            tvTitle.setTextSize(16);

            String aShellCommands = 
                "USER_ID=$(pm create-user --profileOf 0 --user-type android.os.usertype.profile.MANAGED WorkProfile | grep -o '[0-9]*$') && " +
                "am start-user $USER_ID && " +
                "pm install-existing --user $USER_ID " + pkg + " && " +
                "dpm set-profile-owner --user $USER_ID " + admin + " && " +
                "dpm mark-profile-owner-on-organization-owned-device --user $USER_ID " + admin + " && " +
                "am broadcast --user $USER_ID -a " + MyDeviceAdminReceiver.ACTION_APPLY_COPE + " -n " + admin;

            TextView tvCommands = new TextView(this);
            tvCommands.setText(aShellCommands);
            tvCommands.setTextIsSelectable(true);
            tvCommands.setTextSize(11);
            tvCommands.setPadding(0, 16, 0, 16);

            Button btnCopy = new Button(this);
            btnCopy.setText("Скопировать для aShell");
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("aShell Commands", aShellCommands);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Команда скопирована!", Toast.LENGTH_SHORT).show();
            });

            layout.addView(tvTitle);
            layout.addView(btnCopy);
            layout.addView(tvCommands);

        } else {
            // === РЕЖИМ РАБОЧЕГО ПРОФИЛЯ (USER 10+) ===
            tvStatus = new TextView(this);
            tvStatus.setTextSize(14);
            tvStatus.setPadding(0, 0, 0, 16);

            Button btnApply = new Button(this);
            btnApply.setText("Применить COPE политики и проверить");
            btnApply.setOnClickListener(v -> checkAndApplyPolicies());

            tvLog = new TextView(this);
            tvLog.setTextSize(12);
            tvLog.setTextIsSelectable(true);

            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(tvLog);

            layout.addView(tvStatus);
            layout.addView(btnApply);
            layout.addView(scrollView);

            updateProfileStatus();
            checkAndApplyPolicies(); // Запускаем проверку при открытии
        }

        setContentView(layout);
    }

    private void updateProfileStatus() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isProfileOwner = dpm.isProfileOwnerApp(getPackageName());
        boolean isOrgOwned = false;

        if (Build.VERSION.SDK_INT >= 30) {
            isOrgOwned = dpm.isOrganizationOwnedDeviceWithManagedProfile();
        }

        String status = "Статус профиля:\n" +
                "• Profile Owner: " + (isProfileOwner ? "ДА" : "НЕТ") + "\n" +
                "• COPE (Org-Owned): " + (isOrgOwned ? "ДА (Устройство организации)" : "НЕТ (Обычный BYOD)");
        
        tvStatus.setText(status);
    }

    private void checkAndApplyPolicies() {
        StringBuilder log = new StringBuilder();
        log.append("=== РЕЗУЛЬТАТ ПРИМЕНЕНИЯ ПОЛИТИК ===\n\n");

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);
        DevicePolicyManager parentDpm = dpm.getParentProfileInstance(admin);

        // 1. Проверка USB Signaling
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                parentDpm.setUsbDataSignalingEnabled(false);
                log.append("[УСПЕХ] USB Data Signaling отключен.\n");
            } catch (Exception e) {
                log.append("[ОШИБКА USB] ").append(e.getMessage()).append("\n");
            }
        } else {
            log.append("[ПРОПУСК] USB Data Signaling доступен только с Android 12.\n");
        }

        // 2. Проверка неверных попыток ввода пароля
        try {
            parentDpm.setMaximumFailedPasswordsForWipe(admin, 3);
            log.append("[УСПЕХ] Лимит попыток пароля (3) установлен.\n");
        } catch (Exception e) {
            log.append("[ОШИБКА Пароля] ").append(e.getMessage()).append("\n");
        }

        // 3. Проверка FRP Policy
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                android.app.admin.FactoryResetProtectionPolicy frpPolicy = 
                        new android.app.admin.FactoryResetProtectionPolicy.Builder()
                        .setFactoryResetProtectionAccounts(java.util.Collections.emptyList())
                        .setFactoryResetProtectionEnabled(false)
                        .build();
                dpm.setFactoryResetProtectionPolicy(admin, frpPolicy);
                log.append("[УСПЕХ] FRP отключен.\n");
            } catch (Exception e) {
                log.append("[ОШИБКА FRP] ").append(e.getMessage()).append("\n");
            }
        } else {
            log.append("[ПРОПУСК] Сброс FRP из COPE доступен только с Android 12.\n");
        }

        tvLog.setText(log.toString());
        updateProfileStatus();
    }
}
