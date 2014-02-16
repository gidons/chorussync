package org.seachordsmen.chorussync.app.player;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

public class PlayerService extends Service {

    public final class Binder extends android.os.Binder {
        public Player getPlayer() { return player; }
    }

    private Player player = new Player(this);
    private Thread playerThread = new Thread(player);
    private IBinder binder = new Binder();
    
    @Override
    public void onCreate() {
        super.onCreate();
        player.init();  
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Uri trackUri = intent.getData();
        player.setTrackUri(trackUri);
        playerThread.start();
        return binder;
    }
}
