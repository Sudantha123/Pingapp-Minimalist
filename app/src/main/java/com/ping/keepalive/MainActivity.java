package com.ping.keepalive;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView statusText;
    private EditText inputUrl, inputDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        inputUrl = findViewById(R.id.inputUrl);
        inputDelay = findViewById(R.id.inputDelay);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
        }

        updateStatus();

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String host = inputUrl.getText().toString().trim();
                String delay = inputDelay.getText().toString().trim();

                Intent intent = new Intent(MainActivity.this, PingService.class);
                intent.putExtra("HOST", host);
                intent.putExtra("DELAY", Integer.parseInt(delay));
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                
                Toast.makeText(MainActivity.this, "Native Engine Started", Toast.LENGTH_SHORT).show();
                updateStatus();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, PingService.class));
                updateStatus();
            }
        });
    }

    private void updateStatus() {
        if (PingService.isRunning) {
            statusText.setText("Status: NATIVE ENGINE RUNNING");
            statusText.setTextColor(Color.GREEN);
        } else {
            statusText.setText("Status: OFF");
            statusText.setTextColor(Color.RED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
