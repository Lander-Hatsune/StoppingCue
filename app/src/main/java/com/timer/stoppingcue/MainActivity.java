package com.timer.stoppingcue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static int timeInterval;
    private static int vibrateLen;
    private static boolean paused = true;
    private static AlarmManager alarmManager;
    private static PendingIntent alarmIntent;
    private static final String[] TIME_INTERVALS_STR = {"3 (test)", "10", "15", "20", "30", "45", "60", "90", "120"};
    private static final int[] TIME_INTERVALS_INT = {3 * 60000, 10 * 60000, 15 * 60000, 20 * 60000, 30 * 60000,
            45 * 60000, 60 * 60000, 90 * 60000, 120 * 60000};
    private static final String[] VIBRATE_LEN_STR = {"100", "200", "500", "1000", "2000", "5000"};
    private static final int[] VIBRATE_LEN_INT = {100, 200, 500, 1000, 2000, 5000};
    private static final String CHANNEL_ID = "20010814";

    private static TextView lastRingTV;
    private static TextView nextRingTV;

    public static void updateResidentNotification(Context context) {
        String residenceTxt = "Next ring at " + nextRingTV.getText() +
                ", ring " + vibrateLen + "ms at " + timeInterval / 60000 + "min interval";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.pause)
                .setContentTitle(context.getString(R.string.tip_residence))
                .setContentText(residenceTxt)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(811, builder.build());
    }

    public void setTimer() {
        Log.d("setTimer", "interval: " + timeInterval
                + ", vibrateLen: " + vibrateLen);
        String nextRingTxt = new SimpleDateFormat(
                "HH:mm", Locale.getDefault()).format(
                new Date(new Date().getTime() + timeInterval));
        nextRingTV.setText(nextRingTxt);

        Intent alarmIntent_ = new Intent(this, AlarmRing.class);
        alarmIntent_.putExtra("vibrateLen", vibrateLen);
        alarmIntent = PendingIntent.getBroadcast(this, 0,
                alarmIntent_, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(), timeInterval, alarmIntent);

        updateResidentNotification(this);
    }

    public void stopTimer() {
        Log.d("stopTimer", "");
        nextRingTV.setText(getResources().getString(R.string.nought));
        alarmManager.cancel(alarmIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(811);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        timeInterval = TIME_INTERVALS_INT[0];
        vibrateLen = VIBRATE_LEN_INT[0];

        lastRingTV = findViewById(R.id.LastRingTV);
        nextRingTV = findViewById(R.id.NextRingTV);

        createNotificationChannel();

        Button button = findViewById(R.id.Button);
        button.setBackgroundColor(getResources().getColor(R.color.green));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paused = !paused;
                if (paused) {
                    button.setBackgroundColor(getResources().getColor(R.color.green));
                    button.setText("Start");
                    stopTimer();
                } else {
                    button.setBackgroundColor(getResources().getColor(R.color.red));
                    button.setText("Pause");
                    setTimer();
                }
            }
        });

        NumberPicker timeIntervalNP = findViewById(R.id.TimeIntervalNP);
        timeIntervalNP.setMinValue(0);
        timeIntervalNP.setMaxValue(7);
        timeIntervalNP.setDisplayedValues(TIME_INTERVALS_STR);
        timeIntervalNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                if (!paused) {
                    button.performClick();
                }
                timeInterval = TIME_INTERVALS_INT[i1];
                Log.d("timeIntervalNP onChange", "set timeInterval: " + timeInterval);
            }
        });

        NumberPicker vibrateLenNP = findViewById(R.id.VibrateLengthNP);
        vibrateLenNP.setMinValue(0);
        vibrateLenNP.setMaxValue(5);
        vibrateLenNP.setDisplayedValues(VIBRATE_LEN_STR);
        vibrateLenNP.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                if (!paused) {
                    button.performClick();
                }
                vibrateLen = VIBRATE_LEN_INT[i1];
                Log.d("vibrateLenNP onChange", "set vibrateLen: " + vibrateLen);
            }
        });
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static class AlarmRing extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive", "");

            String lastRingTxt = new SimpleDateFormat(
                    "HH:mm", Locale.getDefault()).format(new Date());
            lastRingTV.setText(lastRingTxt);

            String nextRingTxt = new SimpleDateFormat(
                    "HH:mm", Locale.getDefault()).format(
                            new Date(new Date().getTime() + timeInterval));
            nextRingTV.setText(nextRingTxt);

            String notificationTxt = "It's already " + timeInterval / 60000 + " minutes!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.pause)
                    .setContentTitle(context.getString(R.string.tip))
                    .setContentText(notificationTxt)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setVibrate(new long[] {vibrateLen})
                    .setDefaults(Notification.DEFAULT_SOUND);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(814, builder.build());
            updateResidentNotification(context);
        }
    }
}