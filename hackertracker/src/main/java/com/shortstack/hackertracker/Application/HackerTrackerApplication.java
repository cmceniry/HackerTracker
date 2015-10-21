package com.shortstack.hackertracker.Application;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.shortstack.hackertracker.Adapter.DatabaseAdapter;
import com.shortstack.hackertracker.Adapter.DatabaseAdapterStarred;
import com.shortstack.hackertracker.Adapter.DatabaseAdapterVendors;
import com.shortstack.hackertracker.R;
import com.shortstack.hackertracker.Utils.AlarmReceiver;
import com.shortstack.hackertracker.Utils.SharedPreferencesUtil;

import java.io.IOException;

/**
 * Created by Whitney Champion on 3/19/14.
 */
public class HackerTrackerApplication extends Application {

    private static HackerTrackerApplication application;
    private static Context context;
    public static DatabaseAdapter dbHelper;
    public static DatabaseAdapterVendors vendorDbHelper;
    public static DatabaseAdapterStarred myDbHelperStars;

    public void onCreate(){
        super.onCreate();

        application = this;

        // Assign the context to the Application Scope
        context = getApplicationContext();

        // set up database
        setUpDatabase();

        // set up shared preferences
        SharedPreferencesUtil.getInstance();

    }

    public static void cancelNotification(int id) {

        Intent notificationIntent = new Intent(context, AlarmReceiver.class);
        notificationIntent.putExtra(AlarmReceiver.NOTIFICATION_ID, id);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntent.cancel();

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public static void scheduleNotification(Notification notification, long when, int id) {

        Intent notificationIntent = new Intent(context, AlarmReceiver.class);
        notificationIntent.putExtra(AlarmReceiver.NOTIFICATION_ID, id);
        notificationIntent.putExtra(AlarmReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = when;
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    public static Notification getNotification(String content) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle("Scheduled Notification");
        builder.setContentText(content);
        builder.setSmallIcon(R.drawable.ic_launcher);
        return builder.build();
    }

    private void setUpDatabase() {

        dbHelper = new DatabaseAdapter(context);
        vendorDbHelper = new DatabaseAdapterVendors(context);
        myDbHelperStars = new DatabaseAdapterStarred(context);

        try {

            dbHelper.createDataBase();
            vendorDbHelper.createDataBase();
            myDbHelperStars.createDataBase();

        } catch (IOException ioe) {

            throw new Error("Unable to create database");

        }

        dbHelper.copyStarred();

    }

    public static Context getAppContext() {
        return HackerTrackerApplication.context;
    }

    public static HackerTrackerApplication getApplication() {
        if (application == null) {
            throw new IllegalStateException("Application not initialized");
        }
        return application;
    }

}