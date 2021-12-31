package com.mrz.austock.activity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mrz.austock.R;
import com.mrz.austock.data.ProductDbHelper;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class Notification extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        SetupNotifications();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) { return Service.START_NOT_STICKY; }

    private void SetupNotifications() {
        PusherOptions options = new PusherOptions();
        options.setCluster("us2");

        Pusher pusher = new Pusher("357bedbeb874c8c16e05", options);

        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Log.i("Pusher", "State changed from " + change.getPreviousState() +
                        " to " + change.getCurrentState());
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Log.i("Pusher", "There was a problem connecting! " +
                        "\ncode: " + code +
                        "\nmessage: " + message +
                        "\nException: " + e
                );
            }
        }, ConnectionState.ALL);

        Channel channel = pusher.subscribe("my-channel");

        channel.bind("my-event", new SubscriptionEventListener() {
            @Override
            public void onEvent(PusherEvent event) {
                try {
                    JSONObject data = new JSONObject(event.getData());
                    addNotification("AUStock",data.get("message").toString());
                    Log.i("Pusher", "Received event with data: " + event.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void addNotification(String Tittle, String body) {
        ProductDbHelper dbHelper = new ProductDbHelper(this);
        SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
        Log.d("AUSTOCK", "Creando notf");
        final long id = 23;
        NotificationManager mNotificationManager;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this.getApplicationContext(), "1132");
        Intent notificationIntent = new Intent(this, SplashActivity.class);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(contentIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(Tittle);
        mBuilder.setContentText(body);
        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(body));
        mBuilder.setPriority(android.app.Notification.PRIORITY_MAX);

        mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    "1132",
                    "Anuncio",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId("1132");
        }

        mNotificationManager.notify(Math.toIntExact(id), mBuilder.build());

    }
    @Override
    public void onTaskRemoved(Intent rootIntent){
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
    }
}