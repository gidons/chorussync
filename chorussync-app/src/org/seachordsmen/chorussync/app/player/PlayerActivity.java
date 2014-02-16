package org.seachordsmen.chorussync.app.player;

import org.seachordsmen.chorussync.app.R;

import android.app.Activity;
import android.os.Bundle;

public class PlayerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_detail);
    }
}
