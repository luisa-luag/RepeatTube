package com.android.repeattube;

import android.content.Intent;
import android.os.PersistableBundle;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

/**
 * Video Player Activity
 */
public class VideoPlayerActivity extends YouTubeBaseActivity {

    private YouTubePlayer player;
    private YouTubePlayerView youTubeView;
    private TextView viewCountTV;
    private int viewCount;
    private final int RECOVERY_REQUEST = 1;
    private YouTubePlayer.PlayerStateChangeListener stateChangeListener;
    private Intent intentThatStartThisActivity;

    //OnInitializedListener to set the player configs after a successful initialization
    YouTubePlayer.OnInitializedListener initializedListener = new YouTubePlayer.OnInitializedListener() {
        @Override
        public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
            if (errorReason.isUserRecoverableError()) {
                    errorReason.getErrorDialog(VideoPlayerActivity.this, RECOVERY_REQUEST).show();
            } else {
                String error = String.format(getString(R.string.player_error), errorReason.toString());
                    Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
            youTubePlayer.setPlayerStateChangeListener(stateChangeListener);
            VideoPlayerActivity.this.player = youTubePlayer;
            if (player != null) {
                player.cueVideo(intentThatStartThisActivity.getStringExtra(Intent.EXTRA_TEXT));
                viewCount = 0;
                viewCountTV.setText(String.valueOf(viewCount));
            }
        }
    };

    /**
     * PlayerStateChangeListener for the repeat feature.
     */
    public final class MyPlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoading() {
            player.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
        }

        @Override
        public void onLoaded(String s) {
            player.play();
        }

        @Override
        public void onAdStarted() {
        }

        @Override
        public void onVideoStarted() {
        }

        /**
         * After video end, count the view and start playing again
         */
        @Override
        public void onVideoEnded() {
            viewCount++;
            viewCountTV.setText(String.valueOf(viewCount));
            player.play();
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
        }
    }

    //TODO For future use.
//    private void registerBroadcastReceiver() {
//
//        final IntentFilter theFilter = new IntentFilter();
//        theFilter.addAction(Intent.ACTION_SCREEN_ON);
//        theFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        theFilter.addAction(Intent.ACTION_USER_PRESENT);
//
//
//        BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String strAction = intent.getAction();
//
//                KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
//                if(strAction.equals(Intent.ACTION_USER_PRESENT) || strAction.equals(Intent.ACTION_SCREEN_OFF) || strAction.equals(Intent.ACTION_SCREEN_ON)  )
//                    if( myKM.inKeyguardRestrictedInputMode())
//                    {
//                        System.out.println("Screen off " + "LOCKED");
//                    } else
//                    {
//                        System.out.println("Screen off " + "UNLOCKED");
//                    }
//
//            }
//        };
//
//        getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);
//    }

    /**
     * On restoring Instace state, initializes the youtube player.
     * @param savedInstanceState Bundle for data.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        youTubeView.initialize(Config.YOUTUBE_API_KEY, initializedListener);
    }

    /**
     * On saving the Instance state, releases the youtube player.
     * @param outState Bundle for data.
     * @param outPersistentState PersistableBundle for data.
     */
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        player.release();
    }

    /**
     * Get references to the views, initialize player and settings.
     * @param savedInstanceState Bundle with data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        stateChangeListener = new MyPlayerStateChangeListener();

        youTubeView = findViewById(R.id.youtube_view);
        viewCountTV = findViewById(R.id.tv_view_count);

        youTubeView.initialize(Config.YOUTUBE_API_KEY, initializedListener);

        intentThatStartThisActivity = getIntent();

        //ViewCounts
        viewCount = 0;
        viewCountTV.setText(String.valueOf(viewCount));

        //registerBroadcastReceiver();  future use
    }

    /**
     * For recovery if something goes bad.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_REQUEST) {
            getYouTubePlayerProvider().initialize(Config.YOUTUBE_API_KEY, initializedListener);
        }
    }

    /**
     * Gets the youtube provider.
     * @return Youtube player provider.
     */
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return youTubeView;
    }
}
