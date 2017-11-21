package com.android.repeattube;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Search results Adapter for the RecyclerView
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder> {

    private final int UNINITIALIZED = 1;
    private final int INITIALIZED = 2;
    private int numberItems = 0;

    final private SearchResultsAdapterOnClickListener clickListener;
    private List<SearchResult> results;

    /**
     * Only constructor.
     * @param listener Listener for clicks on the RecyclerView items.
     */
    public SearchResultsAdapter(SearchResultsAdapterOnClickListener listener) {
        clickListener = listener;
    }

    /**
     * Gets the data: String ID, title of a video.
     * @param pos Position of video in the List.
     * @return Youtube's video data.
     */
    public ArrayList<String> getListVideoData(int pos) {
        ArrayList<String> list = new ArrayList<>();
        list.add(results.get(pos).getId().getVideoId());
        list.add(results.get(pos).getSnippet().getTitle());
        return list;
    }

    /**
     * ViewHolder for a SearchResult in the RecyclerView
     */
    class SearchResultViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView listItemSearchResult;
        YouTubeThumbnailView listItemThumbnail;
        YouTubeThumbnailLoader loader;
        View view;
        ProgressBar loadingPB;    //TODO Change load image for a progress bar
        ProgressBar loadingResultPB;

        /**
         * Constructor, get views references, set loading visible, and initialize the view.
         * @param itemView View.
         */
        public SearchResultViewHolder(View itemView) {
            super(itemView);
            view = itemView;

            listItemSearchResult = itemView.findViewById(R.id.tv_item_search_result);
            listItemThumbnail = itemView.findViewById(R.id.ib_item_search_result);
            loadingPB = itemView.findViewById(R.id.pb_loading_thumbnail);
            loadingResultPB = itemView.findViewById(R.id.pb_load_list_result);

            loadingPB.setVisibility(View.VISIBLE);
            listItemThumbnail.setVisibility(View.INVISIBLE);

            initialize();
            itemView.setOnClickListener(this);
        }

        /**
         * Initialization function
         */
        private void initialize() {
            //Set INITIALIZED tags
            listItemThumbnail.setTag(R.id.initialize, INITIALIZED);
            listItemThumbnail.setTag(R.id.thumbnailloader, null);
            listItemThumbnail.setTag(R.id.videoid, "");

            //Initializes the thumbnail
            listItemThumbnail.initialize(Config.YOUTUBE_API_KEY, new YouTubeThumbnailView.OnInitializedListener() {
                @Override
                public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView, final YouTubeThumbnailLoader youTubeThumbnailLoader) {
                    listItemThumbnail.setTag(R.id.thumbnailloader, youTubeThumbnailLoader);
                    loader = youTubeThumbnailLoader;
                    //Gets videoId and verify if it's valid
                    String videoId = (String) listItemThumbnail.getTag(R.id.videoid);
                    if(videoId != null && !videoId.isEmpty()){
                        youTubeThumbnailLoader.setVideo(videoId);
                        youTubeThumbnailLoader.setOnThumbnailLoadedListener(new YouTubeThumbnailLoader.OnThumbnailLoadedListener() {
                            @Override
                            public void onThumbnailLoaded(YouTubeThumbnailView youTubeThumbnailView, String s) {
                                //Sets loading invisible after successful load.
                                loadingPB.setVisibility(View.INVISIBLE);
                                youTubeThumbnailView.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onThumbnailError(YouTubeThumbnailView youTubeThumbnailView, YouTubeThumbnailLoader.ErrorReason errorReason) {
                                //TODO
                            }
                        });
                    }
                }

                @Override
                public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView, YouTubeInitializationResult youTubeInitializationResult) {
                    //If fails, set tag as UNINITIALIZED
                    listItemThumbnail.setTag(R.id.initialize, UNINITIALIZED);
                }
            });
        }

        /**
         * Called when an item in the RecyclerView is clicked.
         * @param view View.
         */
        @Override
        public void onClick(View view) {
            int clickedPos = getAdapterPosition();
            clickListener.onListClick(clickedPos);
        }

        /**
         * Releases the ViewHolder
         */
        public void release() {
            if (loader != null) loader.release();
        }
    }

    /**
     * Interface for a click listener.
     */
    public interface SearchResultsAdapterOnClickListener {
        void onListClick(int clickedItemIndex);
    }

    /**
     * ViewHolder for a SearchResult.
     * @param parent ViewGroup that the new View will be added.
     * @param viewType Type of view, of the new View.
     * @return
     */
    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflates view
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.search_result_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;


        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        SearchResultViewHolder viewHolder = new SearchResultViewHolder(view);
        return viewHolder;
    }

    /**
     * Called by RecyclerView to display data in the position.
     * @param holder The ViewHolder.
     * @param position The position in the RecyclerView.
     */
    @Override
    public void onBindViewHolder(final SearchResultViewHolder holder, int position) {
        //if its the last show loading
        if (position == getItemCount()-1) {
            //Shows LOADING, hides the rest
            holder.listItemThumbnail.setVisibility(View.INVISIBLE);
            holder.listItemSearchResult.setVisibility(View.INVISIBLE);
            holder.loadingPB.setVisibility(View.INVISIBLE);
            holder.loadingResultPB.setVisibility(View.VISIBLE);
        } else {
            //Hides LOADING, shows the rest
            holder.listItemThumbnail.setVisibility(View.VISIBLE);
            holder.listItemSearchResult.setVisibility(View.VISIBLE);
            holder.loadingPB.setVisibility(View.VISIBLE);
            holder.loadingResultPB.setVisibility(View.INVISIBLE);

            SearchResult singleResult = results.get(position);
            ResourceId rId = singleResult.getId();
            String videoData = singleResult.getSnippet().getTitle();

            holder.listItemSearchResult.setText(videoData);
            holder.listItemThumbnail.setTag(R.id.videoid, rId.getVideoId());

            //Gets state of the youtube player
            int state = (int) holder.listItemThumbnail.getTag(R.id.initialize);
            if (state == UNINITIALIZED)
                //If it's not initializes, initializes
                holder.initialize();
            else if (state == INITIALIZED) {
                //If it's already initialized, check if the loader is initialized
                YouTubeThumbnailLoader loader = (YouTubeThumbnailLoader) holder.listItemThumbnail.getTag(R.id.thumbnailloader);
                //If it's initialized, we'll need to set a new video for reuse of the loader
                if (loader != null) {
                    //Sets loading visible, then sets video and wait for the OnThumbnailLoadedListener
                    holder.loadingPB.setVisibility(View.VISIBLE);
                    holder.listItemThumbnail.setVisibility(View.INVISIBLE);
                    loader.setVideo(rId.getVideoId());
                    loader.setOnThumbnailLoadedListener(new YouTubeThumbnailLoader.OnThumbnailLoadedListener() {
                        @Override
                        public void onThumbnailLoaded(YouTubeThumbnailView youTubeThumbnailView, String s) {
                            //After the thumbnail is loaded, we make the loading invisible again.
                            holder.loadingPB.setVisibility(View.INVISIBLE);
                            holder.listItemThumbnail.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onThumbnailError(YouTubeThumbnailView youTubeThumbnailView, YouTubeThumbnailLoader.ErrorReason errorReason) {
                            //TODO
                        }
                    });
                }
            }
        }
    }

    /**
     * Get numberItems.
     * @return Number of Items in the Adapter.
     */
    @Override
    public int getItemCount() {
        return numberItems;
    }

    /**
     * Sets a List of SearchResults for the Adapter.
     * @param list New List of SearchResults.
     */
    public void setSearchResults(List<SearchResult> list) {
        results = list;
        numberItems = list.size() + 1;
        notifyDataSetChanged();
    }

    public void appendSearchResults(List<SearchResult> list) {
        results.addAll(list);
        notifyItemRangeChanged(numberItems-1, (int) MainActivity.FetchSearchResultsPage.NUMBER_OF_VIDEOS_RETURNED +2);
        numberItems = results.size() + 1;
    }

    /**
     * Resets the adapter.
     */
    public void resetState() {
        if (results != null) {
            results.clear();
            numberItems = 0;
            notifyDataSetChanged();
        }
    }
}
