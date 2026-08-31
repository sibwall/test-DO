package com.example.dpc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        String pkg = getPackageName();
        String admin = pkg + "/" + MyDeviceAdminReceiver.class.getName();

        boolean isWorkProfile = Process.myUserHandle().hashCode() != 0;

        if (!isWorkProfile) {
            TextView tvDescription = new TextView(this);
            tvDescription.setText("Описание приложения:\n" +
                    "Данное DPC-приложение настраивает изолированный рабочий профиль в режиме COPE (Organization-Owned Device).\n" +
                    "Применяются глобальные ограничения безопасности на устройство: отключение USB-передачи, блокировка биометрии и флешек, защита от сброса (FRP) и лимит попыток ввода пароля.\n");
            tvDescription.setTextSize(14);

            String universalCommand = 
                "adb(){ if [ \"$1\" = \"shell\" ]; then shift; fi; \"$@\"; }; " +
                "USER_ID=$(adb shell pm create-user --profileOf 0 --user-type android.os.usertype.profile.MANAGED WorkProfile | grep -o '[0-9]*$') && " +
                "adb shell am start-user $USER_ID && " +
                "adb shell pm install-existing --user $USER_ID " + pkg + " && " +
                "adb shell dpm set-profile-owner --user $USER_ID " + admin + " && " +
                "adb shell dpm mark-profile-owner-on-organization-owned-device --user $USER_ID " + admin;

            TextView tvCommands = new TextView(this);
            tvCommands.setText(universalCommand);
            tvCommands.setTextIsSelectable(true);
            tvCommands.setTextSize(11);
            tvCommands.setPadding(0, 16, 0, 16);

            Button btnCopy = new Button(this);
            btnCopy.setText("Скопировать ADB команду");
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ADB Command", universalCommand);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Команда скопирована!", Toast.LENGTH_SHORT).show();
            });

            layout.addView(tvDescription);
            layout.addView(btnCopy);
            layout.addView(tvCommands);

        } else {
            TextView tvQuestion = new TextView(this);
            tvQuestion.setText("Что вы хотите?");
            tvQuestion.setTextSize(18);
            tvQuestion.setPadding(0, 0, 0, 24);

            Button btnSetPassword = new Button(this);
            btnSetPassword.setText("Задать пароль для рабочего профиля");
            btnSetPassword.setOnClickListener(v -> {
                Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                startActivity(intent);
            });

            Button btnDeleteProfile = new Button(this);
            btnDeleteProfile.setText("Удалить рабочий профиль");
            btnDeleteProfile.setOnClickListener(v -> {
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                dpm.wipeData(0);
            });

            layout.addView(tvQuestion);
            layout.addView(btnSetPassword);
            layout.addView(btnDeleteProfile);

            String errorMsg = validatePolicies();
            if (errorMsg != null) {
                showErrorDialog(errorMsg);
            }
        }

        setContentView(layout);
    }

    private void showErrorDialog(String errorMsg) {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка безопасности!")
                .setMessage("Обнаружены неактивные политики:\n\n" + errorMsg)
                .setPositiveButton("Понятно", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private String validatePolicies() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);
        DevicePolicyManager parentDpm = dpm.getParentProfileInstance(admin);

        StringBuilder errors = new StringBuilder();

        if (!dpm.isProfileOwnerApp(getPackageName())) {
            errors.append("• Приложение не является Profile Owner.\n");
        }
        
        if (Build.VERSION.SDK_INT >= 31) {
            if (dpm.isUsbDataSignalingEnabled()) {
                errors.append("• USB Data Signaling всё ещё ВКЛЮЧЕН.\n");
            }
        }

        if (parentDpm.getMaximumFailedPasswordsForWipe(admin) != 3) {
            errors.append("• Не установлен лимит неудачных попыток ввода пароля (ожидается 3).\n");
        }

        if (!parentDpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)) {
            errors.append("• Запрет физических накопителей (DISALLOW_MOUNT_PHYSICAL_MEDIA) НЕ активен.\n");
        }

        int keyguardFlags = parentDpm.getKeyguardDisabledFeatures(admin);
        int expectedFlags = DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS | DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS;
        if ((keyguardFlags & expectedFlags) != expectedFlags) {
            errors.append("• Ограничения экрана блокировки (Биометрия/Trust Agents) НЕ активны.\n");
        }

        return errors.length() > 0 ? errors.toString() : null;
    }
}
