package com.android.repeattube;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Video Player Activity
 */
public class VideoPlayerActivity extends YouTubeBaseActivity implements SearchResultsAdapter.SearchResultsAdapterOnClickListener {

    private YouTubePlayer player;
    private YouTubePlayerView youTubeView;
    private TextView titleTV;
    private TextView viewCountTV;
    private int viewCount;
    private final int RECOVERY_REQUEST = 1;
    private final String REPEATS = "Repeats: ";
    private YouTubePlayer.PlayerStateChangeListener stateChangeListener;
    private Intent intentThatStartThisActivity;
    private String nextPageToken;
    private RecyclerView relatedResultsRV;
    private LinearLayoutManager rvLayoutManager;
    private SearchResultsAdapter relatedResultsAdapter;
    private ProgressBar relatedVideosLoadingIndicator;
    private String videoID;

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
                ArrayList<String> videoData = intentThatStartThisActivity.getStringArrayListExtra(Intent.EXTRA_TEXT);
                player.cueVideo(videoData.get(0));
                titleTV.setText(videoData.get(1));
                viewCount = 0;
                String repeat = REPEATS + String.valueOf(viewCount);
                viewCountTV.setText(repeat);
                relatedResultsAdapter.resetState();
                fetchRelatedVideos(videoData.get(0));
            }
        }
    };

    //onScrollListener for loading more results
    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        private boolean loading = true;
        private int previousItemCount = 0;
        private final int scrollThreshold = 3;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            int lastVisiblePosition = rvLayoutManager.findLastVisibleItemPosition();
            int itemCount = rvLayoutManager.getItemCount();

            //if there's less items reset state
            if (previousItemCount > itemCount) {
                resetState();

                //if loading, detects if already loaded
            } else if (loading) {
                if (itemCount > previousItemCount) {
                    previousItemCount = itemCount;
                    loading = false;
                }

                //if not loading and in range, load more
            } else if (lastVisiblePosition > rvLayoutManager.getItemCount()-scrollThreshold) {
                loadMore();
                loading = true;
            }
        }

        private void loadMore() {
            new FetchRelateResultsPage().execute(videoID);
        }

        public void resetState() {
            loading = true;
            previousItemCount = 0;
        }
    };

    /**
     * Happens when an item in the RecyclerView was clicked.
     * @param clickedItemIndex Index of the clicked item.
     */
    @Override
    public void onListClick(int clickedItemIndex) {
        //Gets ID for video
        ArrayList<String> videoData = relatedResultsAdapter.getListVideoData(clickedItemIndex);

//        //Starts video player activity
//        Context context = getApplicationContext();
//        Intent intent = new Intent(context, VideoPlayerActivity.class);
//
//        intent.putStringArrayListExtra(Intent.EXTRA_TEXT, videoData);
//        startActivity(intent);

        //doesnt create new activity, just changes the current
        intentThatStartThisActivity.putStringArrayListExtra(Intent.EXTRA_TEXT, videoData);
        relatedResultsAdapter.resetState();
        player.release();
        youTubeView.initialize(Config.YOUTUBE_API_KEY, initializedListener);

        intentThatStartThisActivity = getIntent();

        //ViewCounts
        viewCount = 0;
        String repeat = REPEATS + String.valueOf(viewCount);
        viewCountTV.setText(repeat);
    }

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
            String repeat = REPEATS + String.valueOf(viewCount);
            viewCountTV.setText(repeat);
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
        titleTV = findViewById(R.id.tv_video_title);
        relatedResultsRV = findViewById(R.id.rv_related_results);
        relatedVideosLoadingIndicator = findViewById(R.id.pb_related_loading);

        //Creates adapter for RecyclerView
        relatedResultsAdapter = new SearchResultsAdapter(this);

        //sets the RecyclerView
        rvLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL, false);
        relatedResultsRV.setLayoutManager(rvLayoutManager);
        relatedResultsRV.setHasFixedSize(true);
        relatedResultsRV.setAdapter(relatedResultsAdapter);
        relatedResultsRV.addOnScrollListener(scrollListener);

        youTubeView.initialize(Config.YOUTUBE_API_KEY, initializedListener);

        intentThatStartThisActivity = getIntent();

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

    private void fetchRelatedVideos(String videoId) {
        videoID = videoId;
        new FetchInitialRelatedResults().execute(videoId);
    }
    /**
     * AsyncTask to fetch initial related results from youtube API
     */
    //TODO Change AsyncTask to Load?
    public class FetchInitialRelatedResults extends AsyncTask<String, Void, List<SearchResult>> {

        private YouTube youTube;
        private final long NUMBER_OF_VIDEOS_RETURNED = 10;

        /**
         * onPreExecute sets loading indicator.
         */
        @Override
        protected void onPreExecute() {
            relatedVideosLoadingIndicator.setVisibility(View.VISIBLE);
        }

        /**
         * Interacts with youtube API to fetch data.
         * @param strings Search query.
         * @return List with SearchResults.
         */
        @Override
        protected List<SearchResult> doInBackground(String... strings) {
            //Check for valid query
            if ((strings == null) || (strings[0].length() == 0)) return null;

            //Builds a YouTube object
            youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {}
            }).setApplicationName("com.android.repeattube").build();

            //Creates and initialize Search List
            YouTube.Search.List search;
            try {
                search = youTube.search().list("snippet");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            search.setKey(Config.YOUTUBE_API_KEY);
            search.setRelatedToVideoId(strings[0]);
            search.setType("video");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            //Executes the search
            SearchListResponse searchListResponse;
            try {
                searchListResponse = search.execute();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            nextPageToken = searchListResponse.getNextPageToken();
            //Return items
            List<SearchResult> searchResultList = searchListResponse.getItems();
            return searchResultList;
        }

        /**
         * Remove the loading indicator and show results or errors.
         * @param searchResultList List of search results (video data)
         */
        @Override
        protected void onPostExecute(List<SearchResult> searchResultList) {
            //If something wrong happened
            if (searchResultList == null) {
                //TODO Create Error Message
                return;
            }

            //Sets loading visibility and search results to the adapter
            relatedVideosLoadingIndicator.setVisibility(View.INVISIBLE);
            relatedResultsAdapter.setSearchResults(searchResultList);
        }

    }

    /**
     * AsyncTask to fetch related results from youtube API
     */
    //TODO Change AsyncTask to Load?
    public class FetchRelateResultsPage extends AsyncTask<String, Void, List<SearchResult>> {

        private YouTube youTube;
        public static final long NUMBER_OF_VIDEOS_RETURNED = 10;

        /**
         * Interacts with youtube API to fetch data.
         *
         * @param strings Search query.
         * @return List with SearchResults.
         */
        @Override
        protected List<SearchResult> doInBackground(String... strings) {
            //Check for valid query
            if ((strings == null) || (strings[0].length() == 0)) return null;

            //Builds a YouTube object
            youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName("com.android.repeattube").build();

            //Creates and initialize Search List
            YouTube.Search.List search;
            try {
                search = youTube.search().list("snippet");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            search.setKey(Config.YOUTUBE_API_KEY);
            search.setRelatedToVideoId(strings[0]);
            search.setType("video");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
            search.setPageToken(nextPageToken);

            //Executes the search
            SearchListResponse searchListResponse;
            try {
                searchListResponse = search.execute();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            nextPageToken = searchListResponse.getNextPageToken();
            //Return items
            List<SearchResult> searchResultList = searchListResponse.getItems();
            return searchResultList;
        }

        /**
         * Remove the loading indicator and show results or errors.
         * @param searchResultList List of search results (video data)
         */
        @Override
        protected void onPostExecute(List<SearchResult> searchResultList) {
            //If something wrong happened
            if (searchResultList == null) {
                //TODO Create Error Message
                return;
            }
            relatedResultsAdapter.appendSearchResults(searchResultList);
        }
    }
    /**
     * Releases the player.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        for (int i = 0; i < relatedResultsRV.getChildCount(); ++i) {
            SearchResultsAdapter.SearchResultViewHolder holder = (SearchResultsAdapter.SearchResultViewHolder) relatedResultsRV.getChildViewHolder(relatedResultsRV.getChildAt(i));
            holder.release();
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
