package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    @BindView(R.id.toolbar) Toolbar mToolbar;

    private ArticleListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);

        setupToolbar();

        mSwipeRefreshLayout.setOnRefreshListener(this);

        setupGrid();

        mAdapter = new ArticleListAdapter(this, null);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void setupGrid() {
        int columnCount = getResources().getInteger(R.integer.grid_column_count);
        StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(staggeredGridLayoutManager);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onRefresh() {
       refresh();
    }

    public class ArticleListAdapter extends CursorRecyclerViewAdapter<ArticleListAdapter.ViewHolder>{
        private final ArticleListActivity mActivity;

        public ArticleListAdapter(ArticleListActivity activity, Cursor cursor) {
            super(cursor);
            this.mActivity = activity;
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String transitionName = mActivity.getString(R.string.shared_element_transition) + vh.getAdapterPosition();

                        view.findViewById(R.id.thumbnail).setTransitionName(transitionName);



                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                mActivity, view.findViewById(R.id.thumbnail), transitionName);
                        intent.putExtra("STE", transitionName);
                        mActivity.startActivity(intent, options.toBundle());
                    }
                    else {
                        mActivity.startActivity(intent);
                    }

                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final Cursor cursor) {
            holder.titleView.setText(cursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            cursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString());
            holder.authorTextView.setText(cursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setAspectRatio(cursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            Glide.clear(holder.thumbnailView);

        /*
        https://github.com/bumptech/glide
         */
            Glide.with(holder.thumbnailView.getContext())
                    .load(cursor.getString(ArticleLoader.Query.THUMB_URL))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .into(holder.thumbnailView);

            // Back up as glide is failing to load few images.
            holder.thumbnailView.setImageUrl(cursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(holder.thumbnailView.getContext()).getImageLoader());
        }

        @Override
        public int getItemCount() {
            return super.getItemCount();
        }

        public  class ViewHolder extends RecyclerView.ViewHolder {
            public DynamicHeightNetworkImageView thumbnailView;
            public TextView titleView;
            public TextView subtitleView;
            public TextView authorTextView;

            public ViewHolder(View view) {
                super(view);
                thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
                titleView = (TextView) view.findViewById(R.id.article_title);
                subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
                authorTextView = (TextView) view.findViewById(R.id.article_author);
            }
        }
    }
}
