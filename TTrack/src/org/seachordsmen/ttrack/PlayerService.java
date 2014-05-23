package org.seachordsmen.ttrack;

import org.seachordsmen.ttrack.audio.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;

public class PlayerService extends Service {
    
    private static final Logger LOG = LoggerFactory.getLogger(PlayerService.class);
    private static final int PLAYING_NOTIFICATION_ID = 1;
    public final class Binder extends android.os.Binder {
        public Player getPlayer() { return player; }
    }
    
    private Player player = new Player(this);
    private Thread playerThread = new Thread(player);
    private IBinder binder = new Binder();
    private Bundle intentExtras;
    
    public PlayerService() {
        LOG.info("Constructing a PlayerService: {}", this);
    }
    
    @Override
    public void onCreate() {
        LOG.info("PlayerService.onCreate()");
        super.onCreate();
        player.init();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.info("PlayerService.onStartCommand: {}, {}", intent, startId);
        intentExtras = intent.getExtras();
        if (!playerThread.isAlive()) {
            playerThread.start();
        }
        createPlayingNotification();
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        LOG.info("PlayerService.onBind()");
        return binder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        LOG.info("PlayerService.onUnbind()");
        if (!player.isPlaying()) {
            stopSelf();
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    public void onRebind(Intent intent) {
        LOG.info("PlayerService.onRebind()");
        super.onRebind(intent);
    }
    
    @Override
    public void onDestroy() {
        LOG.info("PlayerService.onDestroy()");
        player.onDestroy();
        super.onDestroy();
    }

    private void createPlayingNotification() {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(PlayerActivity.class);
        Intent intent = new Intent(this, PlayerActivity.class);
        if (intentExtras != null) {
            intent.putExtras(intentExtras);
        }
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setOngoing(true).setContentTitle("TTrack").setSmallIcon(R.drawable.ic_launcher).setContentIntent(pendingIntent);
        startForeground(PLAYING_NOTIFICATION_ID, builder.build());
    }
    
}
