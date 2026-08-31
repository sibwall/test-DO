package com.example.dpc;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String pkg = getPackageName();
        String admin = pkg + "/" + MyDeviceAdminReceiver.class.getName();
        int userId = 10;

        String adbCommands = 
            "adb shell pm create-user --profileOf 0 --user-type android.os.usertype.profile.MANAGED --user-id " + userId + " WorkProfile\n" +
            "adb shell am start-user " + userId + "\n" +
            "adb shell pm install-existing --user " + userId + " " + pkg + "\n" +
            "adb shell dpm set-profile-owner --user " + userId + " " + admin + "\n" +
            "adb shell dpm mark-profile-owner-on-organization-owned-device --user " + userId + " " + admin + "\n" +
            "adb shell am broadcast --user " + userId + " -a " + MyDeviceAdminReceiver.ACTION_APPLY_COPE + " -n " + admin;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
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
