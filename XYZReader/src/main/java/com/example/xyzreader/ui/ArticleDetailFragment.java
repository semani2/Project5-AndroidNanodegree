package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.Slide;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A fragment representing a single Article detail screen.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id";
    private Unbinder unbinder;
    private long mItemId;
    @BindView(R.id.article_photo)
    ImageView mPhotoView;

    @BindView(R.id.share_fab)
    FloatingActionButton mShareFab;

    @BindView(R.id.detail_toolbar)
    Toolbar mToolbar;

    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mCollapsingToolbarLayout;

    @BindView(R.id.app_bar)
    AppBarLayout mAppBarLayout;

    @Nullable
    @BindView(R.id.details_extra_toolbar)
    LinearLayout detailsToolbar;

    @Nullable
    @BindView(R.id.article_title)
    TextView mTitleView;

    @BindView(R.id.article_author)
    TextView mAuthorView;

    @BindView(R.id.article_body)
    TextView mBodyView;

    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_article_detail, container, false);
        unbinder = ButterKnife.bind(this, view);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide(Gravity.BOTTOM);
            slide.addTarget(mBodyView);
            slide.setInterpolator(
                    AnimationUtils.loadInterpolator(getActivity(),
                            android.R.interpolator.linear_out_slow_in));
            slide.setDuration(300);
            getActivity().getWindow().setEnterTransition(slide);
        }

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor == null || cursor.isClosed() || !cursor.moveToFirst()) {
            return;
        }

        final String title = cursor.getString(ArticleLoader.Query.TITLE);
        String author = Html.fromHtml(
                DateUtils.getRelativeTimeSpanString(
                        cursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by "
                        + cursor.getString(ArticleLoader.Query.AUTHOR)).toString();
        final String body = Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY)).toString();
        String photo = cursor.getString(ArticleLoader.Query.PHOTO_URL);

        if (mToolbar != null) {
            mToolbar.setTitle(title);
            mToolbar.setNavigationIcon(R.drawable.ic_action_arrow_left);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
        }

        if(mTitleView != null) {
            mTitleView.setText(title);
        }
        mAuthorView.setText(author);
        mBodyView.setText(body);


        /*
        https://github.com/bumptech/glide
         */
        Glide.with(this)
                .load(photo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model,
                                                   Target<GlideDrawable> target,
                                                   boolean isFromMemoryCache, boolean isFirstResource) {
                        Bitmap bitmap = ((GlideBitmapDrawable) resource.getCurrent()).getBitmap();
                        changeToolbarColors(bitmap);
                        return false;
                    }
                })
                .into(mPhotoView);

        mShareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(body)
                        .getIntent(), getString(R.string.action_share)));
            }
        });
    }

    /*
       Selecting the Palette color was taken from the following source:
       https://guides.codepath.com/android/Dynamic-Color-using-Palettes
        */
    private void changeToolbarColors(Bitmap bitmap) {
        Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                int defaultColor = 0xFF333333;
                int darkMutedColor = palette.getDarkMutedColor(defaultColor);

                if (mCollapsingToolbarLayout != null) {
                    mCollapsingToolbarLayout.setContentScrimColor(darkMutedColor);
                    mCollapsingToolbarLayout.setStatusBarScrimColor(darkMutedColor);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

}
