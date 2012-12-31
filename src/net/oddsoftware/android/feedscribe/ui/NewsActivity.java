/*
 *  Copyright 2012 Brendan McCarthy (brendan@oddsoftware.net)
 *
 *  This file is part of Feedscribe.
 *
 *  Feedscribe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3 
 *  as published by the Free Software Foundation.
 *
 *  Feedscribe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Feedscribe.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.oddsoftware.android.feedscribe.ui;

import java.io.IOException;
import java.io.InputStream;

import net.oddsoftware.android.utils.WorkerThread;
import net.oddsoftware.android.utils.WorkerThread.Task;
import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedConfig;
import net.oddsoftware.android.feedscribe.data.FeedItem;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.feedscribe.data.FeedSettings;
import net.oddsoftware.android.feedscribe.service.FeedService;
import net.oddsoftware.android.html.HttpCache;
import net.oddsoftware.android.html.Textify;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import java.text.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class NewsActivity extends Activity
{
    protected MyViewFlipper mViewFlipper;
    
    protected ListView mNewsFeedsListView;
    protected NewsFeedsListAdapter mNewsFeedsListAdapter;
    
    protected ListView mNewsItemsListView;
    protected NewsItemsListAdapter mNewsItemsListAdapter;
    
    protected View mNewsView;

    private boolean mLoading;

    private long mItemId;

    private String mItemDescription;

    private String mItemTitle;

    private String mItemURL;
    
    private long mItemFlags;
    
    private String mProcessedData;

    private WorkerThread mWorkerThread;
    
    private Handler mHandler;
    
    private static final String KEY_FEED_ID = "key_feed_id";
    private static final String KEY_ITEM_ID = "key_item_id";
    private static final String KEY_FEEDS_POSITION = "key_feeds_pos";
    private static final String KEY_ITEMS_POSITION = "key_items_pos";
    private static final String KEY_SUBVIEW = "key_subview";
    private static final String KEY_ITEM_URL = "key_item_url";
    private static final String KEY_PROCESSED_DATA = "processed_data";
    private static final String KEY_SCROLL_PERCENT = "scroll_percent";

    private static final int DIALOG_CONFIRM_DELETE_ID = 10;
    private static final int DIALOG_RENAME_FEED_ID = 11;
    
    private static int MENU_ITEM_DELETE_FEED   = 10;
    private static int MENU_ITEM_FEED_SETTINGS = 11;
    private static int MENU_ITEM_RENAME_FEED   = 12;
    
    private HttpCache mHttpCache;

    protected boolean mForceRefresh;

    private StatusReceiver mStatusReceiver;

    private long mSelectedFeedId;

    private WebView mWebView;
    
    private int mScrollPercent = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mHandler = new Handler();
        
    
        mHttpCache = new HttpCache(this);
        
        // there can be only one
        if( mWorkerThread == null )
        {
            mWorkerThread = new WorkerThread();
            mWorkerThread.start();
        }
        
        
        mNewsFeedsListView = new ListView(this);
        mNewsFeedsListView.setOnItemClickListener(new OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3)
            {
                onNewsFeedClicked(position);
            }
            
        });
        
        mNewsFeedsListAdapter = new NewsFeedsListAdapter( this );
        mNewsFeedsListView.setAdapter( mNewsFeedsListAdapter );
        
        mNewsItemsListView = new ListView(this);
        mNewsItemsListView.setOnItemClickListener(new OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3)
            {
                onNewsItemClicked(position);
            }
            
        });
        mNewsItemsListAdapter = new NewsItemsListAdapter( this );
        mNewsItemsListView.setAdapter(mNewsItemsListAdapter);
        
        // now set up the viewflipper 
        mViewFlipper = new MyViewFlipper(this);
        setContentView(mViewFlipper);
        
        registerForContextMenu(mNewsFeedsListView);
        
        mViewFlipper.addView(mNewsFeedsListView, 0);
        mViewFlipper.addView(mNewsItemsListView, 1);
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mNewsView = inflater.inflate(R.layout.news_view_activity, null, false);
        mViewFlipper.addView( mNewsView, 2);
        
        mWebView = (WebView) mNewsView.findViewById(R.id.web_view);

        /* for column view
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setUseWideViewPort(true);
        */
        
        mWebView.setWebChromeClient( new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                setPageLoadProgress(newProgress);
            }
            
        });
        
        /* for column view
        webview.setWebViewClient( new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                // Column Count is just the number of 'screens' of text. Add one for partial 'screens'
                int columnCount = (int) (view.getContentHeight() * 1.5 / view.getWidth() );
                
                if( columnCount <= 0 )
                {
                    columnCount = 1;
                }
                
                Log.d(Globals.LOG_TAG, "height " + view.getContentHeight() + " width " + view.getWidth() + " column count " + columnCount);

                // Must be expressed as a percentage. If not set then the WebView will not stretch to give the desired effect.
                int columnWidth = columnCount * 100;

                String js = "var d = document.getElementsByTagName('body')[0];";
                js += "d.style.WebkitColumnCount=" + columnCount + ";"; 
                js += "d.style.WebkitColumnWidth='" + (columnWidth) + "%';";
                js += "d.style.width='" + (columnWidth) + "%';";
                
                Log.d(Globals.LOG_TAG, "js is " + js);
                view.loadUrl("javascript:(function(){" + js + "})()");
            }
        });
        */
        
        mScrollPercent = -1;
        
        if( savedInstanceState != null )
        {
            int subView = savedInstanceState.getInt(KEY_SUBVIEW, 0);
            int feedsPosition = savedInstanceState.getInt(KEY_FEEDS_POSITION, 0);
            int itemsPosition = savedInstanceState.getInt(KEY_ITEMS_POSITION, 0);
            long feedId = savedInstanceState.getLong(KEY_FEED_ID, 0);
            long itemId = savedInstanceState.getLong(KEY_ITEM_ID, 0);
            
            if( subView == 0 )
            {
                mNewsFeedsListView.setSelection(feedsPosition);
            }
            if( subView >= 1 )
            {
                int pos = mNewsFeedsListAdapter.getPosition(feedId);
                mNewsFeedsListView.setSelection(pos);
                
                mNewsItemsListAdapter.setFeedId( feedId );
                mNewsItemsListView.setSelection( itemsPosition );
            }
            
            mItemURL = savedInstanceState.getString(KEY_ITEM_URL);
            mProcessedData = savedInstanceState.getString(KEY_PROCESSED_DATA);
            
            FeedItem item = FeedManager.getInstance(this).getItemById(itemId);
            if( item != null )
            {
                mItemFlags = item.mFlags;
            }
            
            int scrollPercent = savedInstanceState.getInt(KEY_SCROLL_PERCENT, -1);
            
            if( subView >= 2 )
            {
                showNewsItem(itemId, scrollPercent);
            }
            
            if( subView >= 0 && subView <= 2 )
            {
                mViewFlipper.setDisplayedChild( subView );
            }
        }
    }

    protected void setPageLoadProgress(int newProgress)
    {
        setProgressBarVisibility(true);
        setProgress(newProgress * 100);
        
        if(newProgress == 100 && mScrollPercent > 0)
        {
            mHandler.postDelayed(new Runnable(){

                @Override
                public void run() {
                    int scrollPos = (int)( mWebView.getContentHeight() * mWebView.getScale() * mScrollPercent / 1000);
                    Globals.LOG.d("scrolling to " + scrollPos + " of " + mWebView.getContentHeight() * mWebView.getScale() );
                    mWebView.scrollTo(0, scrollPos);
                }},
               500); // 500ms delay
        }
        
        Activity parent = getParent();
        if( parent != null)
        {
            setProgressBarVisibility(true);
            parent.setProgress(newProgress * 100);
        }
    }

    @Override
    protected void onPause()
    {
        unregisterReceiver(mStatusReceiver);
        
        super.onPause();
    }
    
    @Override
    protected void onResume()
    {
        IntentFilter statusFilter = new IntentFilter(FeedService.STATUS_UPDATE);
        mStatusReceiver = new StatusReceiver();
        registerReceiver(mStatusReceiver, statusFilter);
        
        mNewsFeedsListAdapter.update();
        mNewsItemsListAdapter.update();
        updateTitle();
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putInt(KEY_SUBVIEW, mViewFlipper.getDisplayedChild() );
        
        outState.putLong(KEY_ITEM_ID, mItemId);
        outState.putLong(KEY_FEED_ID, mNewsItemsListAdapter.getFeedId() );
        
        outState.putInt(KEY_FEEDS_POSITION, mNewsFeedsListView.getFirstVisiblePosition() );
        outState.putInt(KEY_ITEMS_POSITION, mNewsItemsListView.getFirstVisiblePosition() );
        
        outState.putString(KEY_ITEM_URL, mItemURL);
        outState.putString(KEY_PROCESSED_DATA, mProcessedData);
        
        if(mViewFlipper.getDisplayedChild() == 2)
        {
            int height = ((int)(mWebView.getContentHeight() * mWebView.getScale()));
            if( height > 0 )
            {
                outState.putInt(KEY_SCROLL_PERCENT, mWebView.getScrollY()* 1000 / height);
            }
        }
    }
    
    private void onNewsFeedClicked(int position)
    {
        if( Globals.LOGGING) Log.d(Globals.LOG_TAG, "onNewsFeedClicked");
        
        mNewsItemsListAdapter.setFeedId( mNewsFeedsListAdapter.getItemId(position) );
        
        showNext();
    }
    
    
    private void onNewsItemClicked(int position)
    {
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "onNewsItemClicked");
        long itemId = mNewsItemsListAdapter.getItemId(position);
        
        showNewsItem( itemId, -1 );
        showNext();
    }

    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "onKeyDown");
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mViewFlipper.getDisplayedChild() > 0 )
        {
            if( mViewFlipper.getDisplayedChild() == 2 )
            {
                saveNewsPosition(mItemId);
                mNewsItemsListAdapter.update();
            }
            if( mViewFlipper.getDisplayedChild() == 1 )
            {
                mNewsFeedsListAdapter.update();
            }
            showPrevious();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void showNext()
    {
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "showNext");
        mViewFlipper.setInAnimation(this, R.anim.view_transition_in_left);
        mViewFlipper.setOutAnimation(this, R.anim.view_transition_out_left);
        mViewFlipper.showNext();
        
        updateTitle();
    }
    
    private void showPrevious()
    {
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "showPrevious");
        mViewFlipper.setInAnimation(this, R.anim.view_transition_in_right);
        mViewFlipper.setOutAnimation(this, R.anim.view_transition_out_right);
        mViewFlipper.showPrevious();
        
        updateTitle();
    }
    
    
    private void showNewsItem(long itemId, int overrideScrollPosition)
    {
        FeedManager feedManager = FeedManager.getInstance(this);
        FeedItem item = feedManager.getItemById(itemId);
        
        if( item != null )
        {
            mItemId = item.mId;
            
            mItemDescription = item.mCleanDescription;
            mItemTitle = item.mCleanTitle;
            
            // see if mProcessedData may be valid based on matching urls
            if( ! item.mLink.equals(mItemURL) )
            {
                // not valid
                mProcessedData = null;
            }
            
            mItemURL = item.mLink;
            
            if( (item.mFlags & FeedItem.FLAG_READ) == 0)
            {
                item.mFlags |= FeedItem.FLAG_READ;
                feedManager.updateItemFlags(item);
            }
            mItemFlags = item.mFlags;
            
            if(overrideScrollPosition < 0)
            {
                mScrollPercent = (int) item.mPosition;
            }
            else
            {
                mScrollPercent = overrideScrollPosition;
            }
        
            loadURL();
            
            updateTitle( );
        }
    }
    
    private void saveNewsPosition(long itemId)
    {
        FeedManager feedManager = FeedManager.getInstance(this);
        FeedItem item = feedManager.getItemById(itemId);
        
        if( item != null )
        {
            int height = ((int)(mWebView.getContentHeight() * mWebView.getScale()));
            if( height > 0 )
            {
                item.mPosition = mWebView.getScrollY() * 1000 / height;
                
                Globals.LOG.d("saving news item " + itemId + " position as " + item.mPosition + " scrollY " + mWebView.getScrollY() + " height " + mWebView.getContentHeight() + " height " + mWebView.getHeight() + " scale " + mWebView.getScale() );
                feedManager.updateItem(item);
            }
        }
    }
    

    private void loadURL()
    {
        setPageLoadProgress(1);
        
        FeedManager feedManager = FeedManager.getInstance(this);
        final long itemId = mItemId;
        final FeedItem item = feedManager.getItemById(mItemId);
        long feedId = 0;
        if( item != null )
        {
            feedId = item.mFeedId;
        }
        final FeedSettings feedSettings = feedManager.getFeedSettings(feedId);
        final Feed feed = feedManager.getFeed(feedId);
        final boolean textifyEnabled = (feedSettings == null || feedSettings.mTextify == true);
        final boolean displayFullArticle = (feedSettings != null && feedSettings.mDisplayFullArticle == true);
        
        mLoading = true;
                
        mWorkerThread.addTask(new Task(){
            
            String processedData = null;
            String processedTitle = null;
            String finalUrl = null;
            
            

            @Override
            public void doInBackground() {
                super.doInBackground();
                
                // try using cached processed data
                if( mProcessedData != null )
                {
                    processedData = mProcessedData;
                    return;
                }
                
                try
                {
                    // j
                    if( displayFullArticle && ! textifyEnabled )
                    {
                        InputStream is = mHttpCache.getResource(mItemURL, mForceRefresh);
                        mForceRefresh = false;
                        if( is != null )
                        {
                            java.io.Reader reader = new java.io.InputStreamReader(is);
                            StringBuilder builder = new StringBuilder(4096);
                            int count = 0;
                            char[] buf = new char[1024];
                            
                            while( (count = reader.read(buf)) != -1 )
                            {
                                builder.append(buf, 0, count);
                                if( builder.length() > (1024 * 1024) )
                                {
                                    if(Globals.LOGGING)Log.e(Globals.LOG_TAG, "error downloading full article, more than 1mb");
                                    break;
                                }
                            }
                            
                            processedData = builder.toString();
                        }
                        else if( item != null)
                        {
                            if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "error retrieving article from cache, using rss content instead");
                            processedData = item.mDescription;
                        }
                       
                        
                        if(Globals.LOGGING)Log.d(Globals.LOG_TAG, "download article size is " + processedData.length() );
                        return;
                    }
                    
                    Textify textify = new Textify();
                    
                    // for column view
                    //textify.setViewport("width=400, user-scalable=no");
                    
                    if( mItemTitle.length() > 0 && !mItemTitle.equals(mItemURL))
                    {
                        textify.setTitle(mItemTitle);
                    }
                    
                    if( item != null )
                    {
                        String author = item.mAuthor;
                        if( author.length() == 0 )
                        {
                            author = feed.mName;
                        }
                        
                        textify.setAuthor(author);
                        textify.setPubDate( DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(item.mPubDate) );
                    }
                    
                    
                    if( displayFullArticle )
                    {
                        InputStream is = mHttpCache.getResource(mItemURL, mForceRefresh);
                        mForceRefresh = false;
                        
                        if( is != null )
                        {
                            textify.process(is);
                        }
                        else if( item != null )
                        {
                            if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "error retrieving article from cache, using rss content instead");
                            textify.process(item.mDescription);
                        }
                    }
                    else if( item != null )
                    {
                        if( ! textifyEnabled )
                        {
                            textify.setProcessingEnabled( false );
                        }
                
                        textify.process(item.mDescription);
                    }
                    
                    processedTitle = textify.getTitle();
                    
                    processedData = textify.getProcessedArticle();
                    
                    if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "displaying article: " + processedData);
                    
                    // this stuff should really go somewhere else, but this is the 2-stage article filter
                    // if there was nothing interesting in the first pass, disable gross biases and try again
                    if( processedData.length() < 1000 || textify.getArticleScore() < 10 )
                    {
                        textify.setStripUnlikelyCandidates( false );
                        if( displayFullArticle )
                        {
                            InputStream is = mHttpCache.getResource(mItemURL, mForceRefresh);
                            mForceRefresh = false;
                            
                            if( is != null )
                            {
                                textify.process(is);
                            }
                            else
                            {
                                if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "error retrieving article from cache, using rss content instead");
                                textify.process(item.mDescription);
                            }
                        }
                        else
                        {
                            textify.process(item.mDescription);
                        }
                        processedData = textify.getProcessedArticle();
                    }
                    
                    // this time there was still nothing, so just output the whole article
                    if( processedData.length() < 1000 )
                    {
                        textify.setProcessingEnabled( false );
                        if( displayFullArticle )
                        {
                            InputStream is = mHttpCache.getResource(mItemURL, mForceRefresh);
                            mForceRefresh = false;
                            
                            if( is != null )
                            {
                                textify.process(is);
                            }
                            else
                            {
                                if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "error retrieving article from cache, using rss content instead");
                                textify.process(item.mDescription);
                            }
                        }
                        else
                        {
                            textify.process(item.mDescription);
                        }
                        processedData = textify.getProcessedArticle();
                    }
                    
                    finalUrl = mHttpCache.getLastUrl(mItemURL);
                }
                catch(IOException exc)
                {
                    if( Globals.LOGGING) Log.e(Globals.LOG_TAG, "Error loading url", exc);
                }
            }

            @Override
            public void onPostExecute()
            {
                super.onPostExecute();
                
                mWebView.getSettings().setBuiltInZoomControls( FeedConfig.getInstance(NewsActivity.this).getShowZoomControls() );
                
                if( itemId != mItemId )
                {
                    if( Globals.LOGGING ) Log.w(Globals.LOG_TAG, "NewsViewActivity - loadURL complete but id has changed");
                    return;
                }
                
                
                if( processedData == null )
                {
                    if( Globals.LOGGING ) Log.w(Globals.LOG_TAG, "NewsViewActivity - loadURL complete - no data, using original url");
                    // show original
                    mWebView.loadUrl(mItemURL);
                    mProcessedData = "";
                }
                else
                {
                    mProcessedData = processedData;
                    String baseUrl = mItemURL;
                    
                    if( finalUrl != null && finalUrl.length() > 0 )
                    {
                        baseUrl = finalUrl;
                    }
                    
                    mWebView.loadDataWithBaseURL(baseUrl, processedData, "text/html", "utf-8", null);
                }
                
                if( processedTitle != null && processedTitle.length() > 0)
                {
                    FeedManager feedManager = FeedManager.getInstance(NewsActivity.this);
                    FeedItem item = feedManager.getItemById(mItemId);
                    if( item.mCleanTitle.equals(item.mOriginalLink) || item.mCleanTitle.length() == 0 )
                    {
                        item.mTitle = processedTitle;
                        
                        StringBuilder titleBuilder = new StringBuilder(processedTitle.length());
                        feedManager.htmlUnescapeInto( new StringBuilder(processedTitle), titleBuilder);
                        
                        processedTitle = titleBuilder.toString();
                        
                        item.mCleanTitle = processedTitle;
                        feedManager.updateItem(item);
                    }
                    mItemTitle = processedTitle;
                    updateTitle();
                }
                
                mLoading = false;
            }
        });
    }
    
    public void onPrev(View view)
    {
        if( mLoading )
        {
            return;
        }
        
        long itemId = mNewsItemsListAdapter.getPreviousItem(mItemId);
        if( itemId > 0)
        {
            showNewsItem(itemId, -1);
        }
    }
    
    public void onNext(View view)
    {
        if( mLoading )
        {
            return;
        }
        long itemId = mNewsItemsListAdapter.getNextItem(mItemId);
        if( itemId > 0)
        {
            showNewsItem(itemId, -1);
        }
    }
    
    public void launchNewsItem(long itemId)
    {
        mViewFlipper.setDisplayedChild(2);
        showNewsItem(itemId, -1);
        
        FeedManager feedManager = FeedManager.getInstance(this);
        Feed feed = feedManager.getFeedByItemId( itemId );
        if( feed != null )
        {
            mNewsItemsListAdapter.setFeedId(feed.mId);
        }
    }
    
    
    private void updateTitle()
    {
        int child = mViewFlipper.getDisplayedChild();
        
        String newTitle = getResources().getString(R.string.app_name);
        
        if( child == 2 )
        {
            newTitle += ": " + mItemTitle;
        }
        else if (child == 1 )
        {
            newTitle += ": " + mNewsItemsListAdapter.getFeedName();
        }
        
        Activity parent = getParent();
        if( parent != null )
        {
            parent.getWindow().setTitle( newTitle );
        }
        else
        {
            getWindow().setTitle( newTitle );
        }
    }
    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        
        Log.e(Globals.LOG_TAG, "NewsActivity.onPrepareOptionsMenu");
        
        menu.removeItem(R.id.open_original);
        menu.removeItem(R.id.share_original);
        menu.removeItem(R.id.add_star);
        menu.removeItem(R.id.remove_star);
        menu.removeItem(R.id.delete);
        menu.removeItem(R.id.keep_unread);
        menu.removeItem(R.id.refresh_article);
        
        menu.removeItem(R.id.mark_all_read);
        menu.removeItem(R.id.delete_all_read);
        menu.removeItem(R.id.refresh_feed);
        
        if( mViewFlipper.getDisplayedChild() == 2)
        {
            menu.removeItem(R.id.add);
            menu.removeItem(R.id.refresh);
            menu.removeItem(R.id.info);
            menu.removeItem(R.id.preferences);
            
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.news_item_view_context_menu, menu);
            
            if( (mItemFlags & FeedItem.FLAG_STARRED) == 0 )
            {
                menu.removeItem(R.id.remove_star);
            }
            else
            {
                menu.removeItem(R.id.add_star);
            }
        }
        else if( mViewFlipper.getDisplayedChild() == 1)
        {
            menu.removeItem(R.id.add);
            menu.removeItem(R.id.refresh);
            menu.removeItem(R.id.info);
            menu.removeItem(R.id.preferences);
            
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.news_item_list_options_menu, menu);
        }
        
        return true;
    }
    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
 
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch( item.getItemId() )
        {
            case R.id.open_original:
            {
                Uri uri = Uri.parse( mItemURL );
                
                try
                {
                    startActivity( new Intent( Intent.ACTION_VIEW, uri ));
                }
                catch( ActivityNotFoundException exc )
                {
                    Toast.makeText(this, R.string.error_view_url, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.share_original:
            {
                Intent intent = new Intent(Intent.ACTION_SEND);
                
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name) + " - " + mItemTitle);
                intent.putExtra(Intent.EXTRA_TEXT, mItemDescription + " " + mItemURL );
                
                try
                {
                    startActivity( Intent.createChooser( intent, getResources().getString(R.string.share_original) ) );
                }
                catch( ActivityNotFoundException exc )
                {
                    Toast.makeText(this, R.string.error_share, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.add_star:
            {
                FeedItem feedItem = FeedManager.getInstance(this).getItemById(mItemId);
                if( feedItem != null )
                {
                    feedItem.mFlags |= FeedItem.FLAG_STARRED;
                    FeedManager.getInstance(this).updateItemFlags(feedItem);
                    mItemFlags = feedItem.mFlags;
                    Toast.makeText(this, R.string.star_added, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.remove_star:
            {
                FeedItem feedItem = FeedManager.getInstance(this).getItemById(mItemId);
                if( feedItem != null )
                {
                    feedItem.mFlags &= ~(FeedItem.FLAG_STARRED);
                    FeedManager.getInstance(this).updateItemFlags(feedItem);
                    mItemFlags = feedItem.mFlags;
                    Toast.makeText(this, R.string.star_removed, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.delete:
            {
                // in news view
                if( mViewFlipper.getDisplayedChild() == 2 )
                {
                    FeedItem feedItem = FeedManager.getInstance(this).getItemById(mItemId);
                    if( feedItem != null )
                    {
                        FeedManager.getInstance(this).deleteFeedItem(feedItem);
                        mNewsItemsListAdapter.update();
                        showPrevious();
                        Toast.makeText(this, R.string.item_deleted, Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                break;
            }
            case R.id.keep_unread:
            {
                FeedItem feedItem = FeedManager.getInstance(this).getItemById(mItemId);
                if( feedItem != null )
                {
                    feedItem.mFlags &= ~(FeedItem.FLAG_READ);
                    FeedManager.getInstance(this).updateItemFlags(feedItem);
                    mItemFlags = feedItem.mFlags;
                    Toast.makeText(this, R.string.kept_unread, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.refresh_article:
            {
                mForceRefresh = true;
                mProcessedData = null;
                showNewsItem(mItemId, 0);
                
                return true;
            }
            case R.id.mark_all_read:
            {
                FeedManager feedManager = FeedManager.getInstance(this);
                long feedId = mNewsItemsListAdapter.getFeedId();
                if( feedId > 0 )
                {
                    feedManager.setFeedItemsRead(feedId);
                    mNewsItemsListAdapter.update();
                }
                return true;
            }
            case R.id.delete_all_read:
            {
                FeedManager feedManager = FeedManager.getInstance(this);
                long feedId = mNewsItemsListAdapter.getFeedId();
                if( feedId > 0 )
                {
                    feedManager.deleteFeedItemsRead(feedId);
                    mNewsItemsListAdapter.update();
                }
                return true;
            }
            case R.id.refresh_feed:
            {
                long feedId = mNewsItemsListAdapter.getFeedId();
                if( feedId > 0 )
                {
                    FeedService.updateFeed(this, feedId);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        switch(id)
        {
        case DIALOG_CONFIRM_DELETE_ID:
            builder.setMessage(R.string.dialog_confirm_delete_feed)
                   .setCancelable(false)
                   .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        deleteFeed();
                    }
                })
                .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            dialog = builder.create();
            break;
            
        case DIALOG_RENAME_FEED_ID:
            {
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.dialog_rename_feed, null);
                
                builder.setTitle(R.string.dialog_rename_feed)
                       .setView(textEntryView)
                       .setCancelable(true)
                       .setPositiveButton(R.string.dialog_rename, new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            EditText text = (EditText) textEntryView.findViewById(R.id.txt_name);
                            renameFeed(text.getText().toString());
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                dialog = builder.create();
            }
        }
        
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch(id)
        {
            case DIALOG_RENAME_FEED_ID:
            {
                EditText text = (EditText) dialog.findViewById(R.id.txt_name);
                text.setText(mNewsFeedsListAdapter.getFeedNameById(mSelectedFeedId));
                text.selectAll();
            }
            break;
        }
        super.onPrepareDialog(id, dialog);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        
        if( v == mNewsFeedsListView )
        {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(mNewsFeedsListAdapter.getFeedName( info.position ) );
            menu.add(0, MENU_ITEM_DELETE_FEED, 0, R.string.menu_delete_feed);
            menu.add(0, MENU_ITEM_RENAME_FEED, 0, R.string.menu_rename_feed);
            menu.add(0, MENU_ITEM_FEED_SETTINGS, 0, R.string.menu_feed_settings);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem menuItem)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuItem.getMenuInfo();
        int position = info.position;
        long feedId = mNewsFeedsListAdapter.getFeedId(position);
        
        if( menuItem.getItemId() == MENU_ITEM_DELETE_FEED )
        {
            mSelectedFeedId = feedId;
            showDialog(DIALOG_CONFIRM_DELETE_ID);
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_RENAME_FEED )
        {
            mSelectedFeedId = feedId;
            showDialog(DIALOG_RENAME_FEED_ID);
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_FEED_SETTINGS )
        {
            Intent intent = new Intent(this, FeedSettingsActivity.class);
            intent.putExtra(FeedSettingsActivity.EXTRA_FEED_ID, feedId);
            startActivity(intent);
            return true;
        }
        else
        {
            return super.onContextItemSelected(menuItem);
        }
    }
    
    private void deleteFeed()
    {
        FeedManager feedManager = FeedManager.getInstance(this);
        Feed feed = feedManager.getFeed(mSelectedFeedId);
        feedManager.deleteFeed( feed, true );
        mNewsFeedsListAdapter.update();
        mNewsItemsListAdapter.update();
    }
    
    private void renameFeed(String newName)
    {
        if(! newName.equals(""))
        {
            FeedManager feedManager = FeedManager.getInstance(this);
            feedManager.setFeedName(mSelectedFeedId, newName);
            mNewsFeedsListAdapter.update();
        }
    }
    
    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int status = intent.getIntExtra("status", FeedService.STATUS_NONE);
            
            if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "NewsActivity.StatusReceiver.onReceive: " + status);
            
            if( status == FeedService.STATUS_UPDATING )
            {
                if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "NewsActivity.StatusReceiver.onReceive: updating");
            }
            else if ( status == FeedService.STATUS_UPDATE_COMPLETE )
            {
                if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "NewsActivity.StatusReceiver.onReceive: update complete");
                
                mNewsFeedsListAdapter.update();
                mNewsItemsListAdapter.update();
                
                if( intent.getIntExtra(FeedService.EXTRA_ERROR, FeedService.ERROR_NONE) == FeedService.ERROR_NETWORK)
                {
                    Toast.makeText(NewsActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
