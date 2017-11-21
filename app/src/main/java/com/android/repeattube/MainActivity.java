package com.android.repeattube;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.List;

/**
 * MainActivity, includes a search box, and results are shown in a RecyclerView, for the user to choose a video.
 * Also a search results click listener.
 */
public class MainActivity extends AppCompatActivity implements SearchResultsAdapter.SearchResultsAdapterOnClickListener {

    private EditText searchBox;
    private RecyclerView searchResultsRV;
    private ProgressBar loadingIndicator;
    private SearchResultsAdapter searchAdapter;

    /**
     * Gets all views and sets the RecyclerView.
     * @param savedInstanceState Bundle to pass data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get views references
        searchBox = findViewById(R.id.et_search_box);
        searchResultsRV = findViewById(R.id.rv_search_results);
        loadingIndicator = findViewById(R.id.pb_load_search_results);

        //Creates adapter for RecyclerView
        searchAdapter = new SearchResultsAdapter(this);

        //sets the RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL, false);
        searchResultsRV.setLayoutManager(layoutManager);
        searchResultsRV.setHasFixedSize(true);
        searchResultsRV.setAdapter(searchAdapter);
    }

    /**
     * Initializes the menu.
     * @param menu Menu to initialize.
     * @return True to display the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflates menu layout
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Detects which menu item was selected and calls appropriate methods.
     * @param item Item that was selected.
     * @return Call to super onOptionsSelected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Search option
        if (id == R.id.action_search) {
            loadSearchResults();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets results based on search query.
     */
    private void loadSearchResults() {
        new FetchSearchResults().execute(searchBox.getText().toString());
    }

    /**
     * Happens when an item in the RecyclerView was clicked.
     * @param clickedItemIndex Index of the clicked item.
     */
    @Override
    public void onListClick(int clickedItemIndex) {
        //Gets ID for video
        String videoId = searchAdapter.getListVideoId(clickedItemIndex);

        //Starts video player activity
        Context context = MainActivity.this;
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, videoId);
        startActivity(intent);
    }

    /**
     * AsyncTask to fetch search results from youtube API
     */
    //TODO Change AsyncTask to Load?
    public class FetchSearchResults extends AsyncTask<String, Void, List<SearchResult>> {

        private YouTube youTube;
        private final long NUMBER_OF_VIDEOS_RETURNED = 50;

        /**
         * onPreExecute sets loading indicator.
         */
        @Override
        protected void onPreExecute() {
            loadingIndicator.setVisibility(View.VISIBLE);
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
            search.setQ(strings[0]);
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
            loadingIndicator.setVisibility(View.INVISIBLE);
            searchAdapter.setSearchResults(searchResultList);
    }

    }
}
