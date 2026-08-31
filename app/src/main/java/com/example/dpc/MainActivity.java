package com.example.dpc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Динамически получаем имя пакета и ресивера
        String pkg = getPackageName();
        String admin = pkg + "/" + MyDeviceAdminReceiver.class.getName();
        int userId = 10;

        // Формируем полный блок ADB-команд
        String adbCommands = 
            "adb shell pm create-user --profileOf 0 --user-type android.os.usertype.profile.MANAGED --user-id " + userId + " WorkProfile\n" +
            "adb shell am start-user " + userId + "\n" +
            "adb shell pm install-existing --user " + userId + " " + pkg + "\n" +
            "adb shell dpm set-profile-owner --user " + userId + " " + admin + "\n" +
            "adb shell dpm mark-profile-owner-on-organization-owned-device --user " + userId + " " + admin + "\n" +
            "adb shell am broadcast --user " + userId + " -a " + MyDeviceAdminReceiver.ACTION_APPLY_COPE + " -n " + admin;

        // UI компоненты (программный layout)
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView tvCommands = new TextView(this);
        tvCommands.setText(adbCommands);
        tvCommands.setTextIsSelectable(true);
        tvCommands.setTextSize(12);

        Button btnCopy = new Button(this);
        btnCopy.setText("Скопировать ADB команды");
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ADB Commands", adbCommands);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Команды скопированы!", Toast.LENGTH_SHORT).show();
        });

        layout.addView(btnCopy);
        layout.addView(tvCommands);
        setContentView(layout);
    }
}
