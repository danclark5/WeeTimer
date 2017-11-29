package com.bruisedthumb.weetimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.text.format.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    /*todo
    * Change Cancel to cancel alarm and quit app. Maybe change to "Stop and Exit"
    *
    * Completed (lower is older)
    * ---------
    * Need something to blink while it's running so that the user knows it's going.
    * Show next interval
    * Show time the alarm goes off
    * Show 0 when the alarm goes off
    * Change Seconds to Minutes. Remember to add 1
    * Add a cancel alarm button
    * create method to invoke an AlarmManager instance
    * create method to cancel an existing alarmManager instance
    * Make sur on Alarmwake that buzzer goes off
    * Learn about properly saving and restoring activity state https://developer.android.com/guide/components/activities/activity-lifecycle.html
    * Get Progress bar to not go to zero upon orientation change.
    * progress bar
    * Stop echoing in alarm
    * Make alarm UI that repeats the alarm and has ability to silence. No snooze.
    */


    private Date timerEnd;

    private TextView timeIndicatorView;
    private TextView nextAlarmView;
    private TextView nextIntervalView;
    private ProgressBar timeProgressBar;
    private ProgressBar runningProgressBar;
    final static int[] intervals = {60,45,30,15};
    private int currentInterval = 0;
    private Button cancelButton;
    DateFormat timeFormat;

    private Uri notification;
    private Ringtone buzzer;

    final static String STATE_TIMER_END = "timerEnd";
    final static String STATE_CURRENT_INTERVAL = "currentInterval";
    final static String LOG_DESC = "WeeTimer";

    private Handler buzzerHandler = new Handler();
    private Runnable buzzerRunnable = new Runnable() {

        public void run(){
            if (!buzzer.isPlaying() && secondsLeft() < 1) {
                buzzer.play();
            }
            if (secondsLeft() < 1) {
                buzzerHandler.postDelayed(buzzerRunnable, 750);
            }
        }
    };

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {

        public void run() {
            int secondsLeft = secondsLeft();
            if (secondsLeft > 0) {
                timeIndicatorView.setText(String.valueOf(secondsLeft / 60 + 1));
            } else {
                timeIndicatorView.setText("0");
            }
            timeProgressBar.setProgress(percentLeft());

            if (secondsLeft > 0) {
                timerHandler.postDelayed(this, 500);
            } else {
                buzzerHandler.postDelayed(buzzerRunnable, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        buzzer = RingtoneManager.getRingtone(getApplicationContext(), notification);

        //Need to look into this a bit more.
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_main);
        timeIndicatorView = findViewById(R.id.timeIndicator);
        nextAlarmView = findViewById(R.id.nextAlarmValue);
        nextIntervalView = findViewById(R.id.nextIntervalValue);
        timeProgressBar = findViewById(R.id.timeProgress);
        timeProgressBar.setMax(10000);
        runningProgressBar = findViewById(R.id.runningProgressBar);
        cancelButton = findViewById(R.id.cancelButton);
        timeFormat = new DateFormat();

    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        currentInterval = sharedPreferences.getInt(STATE_CURRENT_INTERVAL, -1);
        int nextInterval;
        long retrievedTimerEnd = sharedPreferences.getLong(STATE_TIMER_END, -1);
        if (currentInterval > -1 && retrievedTimerEnd > -1) {
            //We have both interval and end time. Set up app with stored values
            timerEnd = new Date(retrievedTimerEnd);
        } else if (currentInterval == -1 && retrievedTimerEnd > -1) {
            //Just have timer end assume shortest period. This should never happen.
            currentInterval = 3;
            timerEnd = new Date(retrievedTimerEnd);
        } else if (currentInterval > -1 && retrievedTimerEnd == -1) {
            //Just have interval, pull end time off of that. This should never happen.
            setTimerEnd(intervals[currentInterval]);
        } else {
            //New instance start fresh
            currentInterval = 0;
            setTimerEnd(intervals[currentInterval]);
        }

        //If the time is too old then reset.
        if(timerEnd.getTime() < System.currentTimeMillis() - ((intervals[0] * 60 * 1000)*2)){
            currentInterval = 0;
            setTimerEnd(intervals[currentInterval]);
        }
        nextAlarmView.setText(timeFormat.format("hh:mm a",timerEnd));
        if (currentInterval == 3) {
            nextInterval = 3;
        } else {
            nextInterval = currentInterval + 1;
        }
        nextIntervalView.setText(String.format("%d minutes", intervals[nextInterval]));
        timerHandler.post(timerRunnable);
        runningProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (timerEnd != null) {
            editor.putLong(STATE_TIMER_END, timerEnd.getTime());
        } else
            editor.remove(STATE_TIMER_END);
        editor.putInt(STATE_CURRENT_INTERVAL, currentInterval);
        editor.commit();

        timerHandler.removeCallbacks(timerRunnable);
        buzzerHandler.removeCallbacks(buzzerRunnable);
        buzzer.stop();
    }

    public void resetButtonClick(View view){
        currentInterval = 0;
        setTimerEnd(intervals[currentInterval]);
        buzzer.stop();
        timerHandler.post(timerRunnable);
        //cancelButton.setVisibility(View.VISIBLE);
        nextAlarmView.setText(timeFormat.format("hh:mm a",timerEnd));
        nextIntervalView.setText(String.format("%d minutes", intervals[1]));
        runningProgressBar.setVisibility(View.VISIBLE);
    }

    public void tryAgainButtonClick(View view){
        int newInterval = currentInterval + 1;
        int nextInterval;
        if (newInterval > 3) {
            newInterval = 3;
        }
        setTimerEnd(intervals[newInterval]);
        buzzer.stop();
        currentInterval = newInterval;
        timerHandler.post(timerRunnable);
        //cancelButton.setVisibility(View.VISIBLE);
        nextAlarmView.setText(timeFormat.format("hh:mm a",timerEnd));
        if (currentInterval == 3) {
            nextInterval = 3;
        } else {
            nextInterval = currentInterval + 1;
        }
        nextIntervalView.setText(String.format("%d minutes", intervals[nextInterval]));
        runningProgressBar.setVisibility(View.VISIBLE);
    }

    public void cancelButtonClick(View view){
        timerHandler.removeCallbacks(timerRunnable);
        buzzerHandler.removeCallbacks(buzzerRunnable);
        buzzer.stop();
        cancelAlarm();
        //Maybe flip text to Resume
        //cancelButton.setVisibility(View.VISIBLE);
        nextAlarmView.setText("N/A");
        timerEnd = null;
        currentInterval = 0;
        nextIntervalView.setText(String.format("%d minutes", intervals[0]));
        runningProgressBar.setVisibility(View.GONE);
    }

    public int secondsLeft(){
        int timeLeft = (int) (timerEnd.getTime() - System.currentTimeMillis()) / 1000;
        if (timeLeft < 0) {
            timeLeft = 0;
        }
        return timeLeft;
    }

    public int percentLeft(){
        Integer totalTime = intervals[currentInterval] * 60000;
        int timeLeft = (int) (timerEnd.getTime() - System.currentTimeMillis());
        if (timeLeft < 0) {
            timeLeft = 0;
        }
        return (int) ((float)timeLeft / totalTime * 10000);
    }

    public void setTimerEnd(int duration){
        timerEnd = new Date(System.currentTimeMillis() + duration * 60000);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class),0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + duration * 60000, pendingIntent);
    }

    public void cancelAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class),0);
        alarmManager.cancel(pendingIntent);
    }
}

