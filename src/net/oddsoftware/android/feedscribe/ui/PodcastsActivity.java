package net.oddsoftware.android.feedscribe.ui;

import java.io.File;

import net.oddsoftware.android.feedscribe.AudioPlayer;
import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Enclosure;
import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedItem;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.feedscribe.service.FeedService;
import net.oddsoftware.android.utils.Utilities;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class PodcastsActivity extends Activity {

    ViewFlipper mViewFlipper = null;
    
    ListView mPodcastsListView = null;
    PodcastFeedsListAdaptor mPodcastsAdapter = null;
    
    ListView mItemsListView = null;
    PodcastItemsListAdapter mItemsAdapter = null;
    
    WebView mShownotesView = null;
    
    String mContentType;
    
    int mOverrideContextMenuPosition;
    
    private static String KEY_CURRENT_VIEW = "current_view";
    private static String KEY_CURRENT_FEED = "current_feed";
    private static String KEY_CURRENT_ITEM = "current_item";
    private static String KEY_FIRST_FEED = "first_feed";
    private static String KEY_FIRST_ITEM = "first_item";
    
    private static int MENU_ITEM_PLAY = 1;
    private static int MENU_ITEM_ADD_PLAYLIST = 2;
    private static int MENU_ITEM_DELETE = 3;
    private static int MENU_ITEM_DOWNLOAD = 4;
    private static int MENU_ITEM_SHOWNOTES = 5;
    private static int MENU_ITEM_MARK_READ = 6;
    private static int MENU_ITEM_MARK_UNREAD = 7;
    private static int MENU_ITEM_ADD_PLAYLIST_STREAM = 8;
    private static int MENU_ITEM_STREAM = 9;
    private static int MENU_ITEM_PLAY_EXTERNAL = 10;
    private static int MENU_ITEM_DELETE_FILE = 11;
    private static int MENU_ITEM_ITEMS_MAX = 11;
    
    private static int MENU_ITEM_DELETE_FEED = 12;
    private static int MENU_ITEM_RENAME_FEED = 13;
    private static int MENU_ITEM_REFRESH_FEED = 14;
            
    
    private static final int DIALOG_CONFIRM_DELETE_ID = 10;
    private static final int DIALOG_RENAME_FEED_ID = 11;

    private long mSelectedFeedId;
    
    private long mCurrentItem;
    
    private Handler mHandler = null;
    private Runnable mUpdateTask;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPodcastsListView = new ListView(this);
        mPodcastsListView.setOnItemClickListener(new OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                onPodcastClicked(position);
            }
            
        });
        
        mContentType = getIntent().getStringExtra("type");
        
        mPodcastsAdapter = new PodcastFeedsListAdaptor( this, mContentType );
        mPodcastsListView.setAdapter( mPodcastsAdapter );
        
        if( mHandler == null )
        {
            mHandler = new Handler();
        }
        
        if( mUpdateTask == null )
        {
            mUpdateTask = new Runnable() {
                
                @Override
                public void run() {
                    onTimer();
                }
            };
        }
        
        
        if( mItemsListView == null)
        {
            mItemsListView = new ListView(this);
            
            mItemsListView.setOnItemClickListener( new OnItemClickListener() {
                
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    onItemClicked(position);
                }
            });
        }
        
        mShownotesView = new WebView(this);
        
        mItemsAdapter = new PodcastItemsListAdapter( this, mContentType );
        mItemsListView.setAdapter( mItemsAdapter );
        registerForContextMenu( mItemsListView );
        registerForContextMenu( mPodcastsListView );
        
        mOverrideContextMenuPosition = -1;
        
        // now set up the view flipper 
        mViewFlipper = new MyViewFlipper(this);
        
        setContentView(mViewFlipper);
        
        mViewFlipper.addView(mPodcastsListView, 0);
        mViewFlipper.addView(mItemsListView, 1);
        mViewFlipper.addView(mShownotesView, 2);
        
        if( savedInstanceState != null )
        {
            long currentFeed = savedInstanceState.getLong(KEY_CURRENT_FEED, 0);
            if( currentFeed > 0 )
            {
                mItemsAdapter.setFeedId(currentFeed);
            }
            
            mCurrentItem = savedInstanceState.getLong(KEY_CURRENT_ITEM, 0);
            if( mCurrentItem > 0)
            {
                FeedManager feedManager = FeedManager.getInstance(this);
                FeedItem feedItem = feedManager.getItemById(mCurrentItem);
                if( feedItem != null )
                {
                    mShownotesView.loadDataWithBaseURL(null, feedItem.mDescription, "text/html", "utf-8", null);
                }
            }
            
            
            int currentView = savedInstanceState.getInt(KEY_CURRENT_VIEW, 0);
            if( currentView >= 0 && currentView <= 2)
            {
                mViewFlipper.setDisplayedChild(currentView);
            }
            
            int firstItem = savedInstanceState.getInt(KEY_FIRST_ITEM, 0);
            if( firstItem >= 0 && firstItem < mItemsAdapter.getCount() )
            {
                mItemsListView.setSelection(firstItem);
            }
            
            int firstFeed = savedInstanceState.getInt(KEY_FIRST_FEED, 0);
            if( firstFeed >= 0 && firstFeed < mPodcastsAdapter.getCount() )
            {
                mPodcastsListView.setSelection(firstFeed);
            }
        }
    }
    
    
    private void onItemClicked(int position)
    {
        if( Globals.LOGGING) Log.d(Globals.LOG_TAG, "onItemClicked");
        
        long itemId      = mItemsAdapter.getFeedItemId(position);
        long enclosureId = mItemsAdapter.getEnclosureId(position);
        
        FeedManager feedManager = FeedManager.getInstance(this);
        
        FeedItem item = feedManager.getItemById(itemId);
        Enclosure enclosure = feedManager.getEnclosure(enclosureId);
        feedManager.verifyEnclosure(enclosure);
        
        if( enclosure.mDownloadTime <= 0 )
        {
            // not downloaded, show menu asking what to do
            mOverrideContextMenuPosition = position;
            this.openContextMenu(mItemsListView);
        }
        else if( mContentType.equals("audio") )
        {
            playAudio(item, enclosure);
        }
        else // if( mContentType.equals("video") )
        {
            playVideo(item, enclosure);
        }
    }


    private void onPodcastClicked(int position)
    {
        if( Globals.LOGGING) Log.d(Globals.LOG_TAG, "onPodcastClicked");
        long feedId = mPodcastsAdapter.getFeedId(position);
        mItemsAdapter.setFeedId( feedId );
        
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
                mItemsAdapter.update();
            }
            if( mViewFlipper.getDisplayedChild() == 1 )
            {
                mPodcastsAdapter.update();
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

    
    @Override
    protected void onPause()
    {
        mHandler.removeCallbacks(mUpdateTask);
        super.onPause();
    }
    

    @Override
    protected void onResume()
    {
        super.onResume();
        
        AudioPlayer player = AudioPlayer.getInstance(this);
        
        if(player.isPlaying())
        {
            long playingItemId = AudioPlayer.getInstance(this).getItemId();
            mItemsAdapter.setPlayingItemId(playingItemId);
        }
        else
        {
            mItemsAdapter.setPlayingItemId(-1);
        }
        
        mItemsAdapter.update();
        mPodcastsAdapter.update();
        
        updateTitle();
        
        onTimer();
    }


    @Override
    public boolean onContextItemSelected(MenuItem menuItem)
    {
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "onContextItemSelected " + menuItem);
        
        boolean handled = false;
        if( menuItem.getItemId() <= MENU_ITEM_ITEMS_MAX )
        {
            handled = onItemContextItemSelected( menuItem );
        }
        else
        {
            handled = onPodcastContextItemSelected( menuItem );
        }
        
        if( handled )
        {
            return true;
        }
        else
        {
            return super.onContextItemSelected(menuItem);
        }
    }
    
    
    private boolean onItemContextItemSelected(MenuItem menuItem)
    {
        int position = mOverrideContextMenuPosition;
        
        if( position < 0 )
        {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuItem.getMenuInfo();
            position = info.position;
        }
            
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "onPodcastContextItemSelected " + position);
        
        FeedManager feedManager = FeedManager.getInstance(this);
        
        FeedItem item = null;
        Enclosure enclosure = null;
        if( position >= 0 )
        {
            long itemId = mItemsAdapter.getFeedItemId(position);
            long enclosureId = mItemsAdapter.getEnclosureId(position);
            
            item = feedManager.getItemById(itemId);
            enclosure = feedManager.getEnclosure(enclosureId);
        }
        
        if( menuItem.getItemId() == MENU_ITEM_PLAY )
        {
            if( mContentType.equals("audio"))
            {
                playAudio(item, enclosure);
            }
            else
            {
                playVideo(item, enclosure);
            }
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_PLAY_EXTERNAL && enclosure != null )
        {
            Uri uri = null;
            if( enclosure.mDownloadTime > 0 )
            {
                uri = Uri.fromFile( new File( enclosure.mDownloadPath ) );
            }
            else
            {
                uri = Uri.parse( enclosure.mURL );
            }
            
            try
            {
                Intent intent = new Intent( Intent.ACTION_VIEW, uri );
                intent.setDataAndType( uri, enclosure.mContentType.toLowerCase() );
                startActivity( intent );
                
                item.mFlags |= FeedItem.FLAG_READ;
                feedManager.updateItemFlags(item);
                mItemsAdapter.update();
            }
            catch( ActivityNotFoundException exc )
            {
                Toast.makeText(this, R.string.error_play_external, Toast.LENGTH_LONG).show();
            }
            
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_STREAM )
        {
            if( mContentType.equals("audio"))
            {
                playAudio(item, enclosure);
            }
            else
            {
                playVideo(item, enclosure);
            }
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_MARK_READ )
        {
            item.mFlags |= FeedItem.FLAG_READ;
            feedManager.updateItemFlags(item);
            mItemsAdapter.update();
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_MARK_UNREAD )
        {
            item.mFlags &= ~FeedItem.FLAG_READ;
            feedManager.updateItemFlags(item);
            mItemsAdapter.update();
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_DELETE )
        {
            feedManager.deleteFeedItem(item);
            mItemsAdapter.update();
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_DELETE_FILE )
        {
            feedManager.deleteDownloadedEnclosure(enclosure);
            mItemsAdapter.update();
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_SHOWNOTES )
        {
            mCurrentItem = item.mId;
            mShownotesView.loadDataWithBaseURL(null, item.mDescription, "text/html", "utf-8", null);
            showNext();
            return true;
        }
        else if( menuItem.getItemId() == MENU_ITEM_DOWNLOAD )
        {
            if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "podcasts activity - adding download");
            feedManager.addDownload(item, enclosure);
            FeedService.downloadAdded(this);
            return true;
        }
        else
        {
            return false; // not handled
        }
    }
    
    private boolean onPodcastContextItemSelected(MenuItem menuItem)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuItem.getMenuInfo();
        
        int position = info.position;
        long feedId = mPodcastsAdapter.getFeedId(position);
        
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
        else if( menuItem.getItemId() == MENU_ITEM_REFRESH_FEED )
        {
            FeedService.updateFeed(this, feedId);
            return true;
        }
        else
        {
            return false; // not handled
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        if( v == mItemsListView )
        {
            int position = mOverrideContextMenuPosition;
            
            if( position < 0 )
            {
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
                
                position = info.position;
            }
            
            long enclosureId = mItemsAdapter.getEnclosureId(position);
            long itemId      = mItemsAdapter.getFeedItemId(position);
            
            FeedManager feedManager = FeedManager.getInstance(this);
            
            FeedItem item = feedManager.getItemById(itemId);
            Enclosure enclosure = feedManager.getEnclosure(enclosureId);
            feedManager.verifyEnclosure(enclosure);
                
            menu.setHeaderTitle(item.mCleanTitle);
            
            if( enclosure.mDownloadTime > 0 )
            {
                menu.add(0, MENU_ITEM_PLAY, 0, R.string.menu_play);
                menu.add(0, MENU_ITEM_ADD_PLAYLIST, 1, R.string.menu_add_playlist);
                
                menu.add(0, MENU_ITEM_DELETE_FILE, 2, R.string.menu_delete_download);
            }
            else
            {
                String downloadString;
                if( enclosure.mLength > 0 )
                {
                    downloadString = getResources().getString(R.string.menu_download_size, Utilities.formatFileSize( enclosure.mLength ) );
                }
                else
                {
                    downloadString = getResources().getString(R.string.menu_download);
                }
                menu.add(0, MENU_ITEM_DOWNLOAD, 0, downloadString );
                menu.getItem(menu.size() - 1).setEnabled( ! feedManager.isDownloading( enclosure ) );
                menu.add(0, MENU_ITEM_STREAM, 1, R.string.menu_stream);
                menu.add(0, MENU_ITEM_ADD_PLAYLIST_STREAM, 2, R.string.menu_add_playlist_stream);
            }
            
            menu.add(0, MENU_ITEM_PLAY_EXTERNAL, 9, R.string.menu_play_external);
            
            menu.add(0, MENU_ITEM_DELETE, 10, R.string.menu_delete);
            
            menu.add(0, MENU_ITEM_SHOWNOTES, 12, R.string.menu_shownotes);
            
            if( (item.mFlags & FeedItem.FLAG_READ) == 0 )
            {
                menu.add(0, MENU_ITEM_MARK_READ, 13, R.string.menu_mark_read);
            }
            else
            {
                menu.add(0, MENU_ITEM_MARK_UNREAD, 13, R.string.menu_mark_unread);
            }
        }
        else if( v == mPodcastsListView )
        {
            menu.add(0, MENU_ITEM_REFRESH_FEED, 0, R.string.menu_refresh_feed);
            menu.add(0, MENU_ITEM_DELETE_FEED, 0, R.string.menu_delete_feed);
            menu.add(0, MENU_ITEM_RENAME_FEED, 0, R.string.menu_rename_feed);
        }
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
            break;
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
                text.setText(mPodcastsAdapter.getFeedNameById(mSelectedFeedId));
                text.selectAll();
            }
            break;
        }
        super.onPrepareDialog(id, dialog);
    }
    
    private void deleteFeed()
    {
        FeedManager feedManager = FeedManager.getInstance(this);
        Feed feed = feedManager.getFeed(mSelectedFeedId);
        feedManager.deleteFeed( feed, true );
        mItemsAdapter.update();
        mPodcastsAdapter.update();
    }
    
    private void renameFeed(String newName)
    {
        if(! newName.equals(""))
        {
            FeedManager feedManager = FeedManager.getInstance(this);
            feedManager.setFeedName(mSelectedFeedId, newName);
            
            mItemsAdapter.update();
            mPodcastsAdapter.update();
        }
    }
    
    @Override
    public void onContextMenuClosed(Menu menu)
    {
        super.onContextMenuClosed(menu);
        mOverrideContextMenuPosition = -1;
    }
    
    
    private void playVideo(FeedItem item, Enclosure enclosure)
    {
        if( Globals.LOGGING )
        {
            Log.d(Globals.LOG_TAG, "playing video enclosure - itemId: " + item.mId + " enclosure id: " + enclosure.mId);
            Log.d(Globals.LOG_TAG, "playing video enclosure - path: " + enclosure.mDownloadPath + " enclosure download: " + enclosure.mDownloadTime);
            Log.d(Globals.LOG_TAG, "playing video enclosure - item name: " + item.mCleanTitle + " enclosure download: " + enclosure.mDownloadTime);
        }
        Intent intent = new Intent(this, PlayVideoActivity.class);
        if( enclosure.mDownloadTime > 0 )
        {
            intent.putExtra(PlayVideoActivity.EXTRA_PATH, enclosure.mDownloadPath);
        }
        else
        {
            intent.putExtra(PlayVideoActivity.EXTRA_URL, enclosure.mURL);
        }
        
        intent.putExtra(PlayVideoActivity.EXTRA_SEEK_TO, enclosure.mPosition);
        intent.putExtra(PlayVideoActivity.EXTRA_ENCLOSURE_ID, enclosure.mId);
        intent.putExtra(PlayVideoActivity.EXTRA_ITEM_ID, item.mId);
        
        startActivity( intent );
    }
    
    private void playAudio(FeedItem item, Enclosure enclosure)
    {
        if( Globals.LOG.d() )
        {
            Globals.LOG.d("playing audio enclosure - itemId: " + item.mId + " enclosure id: " + enclosure.mId);
            Globals.LOG.d("playing audio enclosure - path: " + enclosure.mDownloadPath + " enclosure download: " + enclosure.mDownloadTime);
            Globals.LOG.d("playing audio enclosure - item name: " + item.mCleanTitle + " enclosure download: " + enclosure.mDownloadTime);
        }
        
        AudioPlayer player = AudioPlayer.getInstance(this);
        
        Intent intent = new Intent(this, PlayAudioActivity.class);
        if( player.isPlaying() && player.getItemId() == item.mId )
        {
            intent.putExtra(PlayAudioActivity.EXTRA_FROM_NOTIFICATION, true);
        }
        else
        {
            if( enclosure.mDownloadTime > 0 )
            {
                intent.putExtra(PlayAudioActivity.EXTRA_PATH, enclosure.mDownloadPath);
            }
            else
            {
                intent.putExtra(PlayAudioActivity.EXTRA_URL, enclosure.mURL);
            }
            
            intent.putExtra(PlayAudioActivity.EXTRA_SEEK_TO, enclosure.mPosition);
            intent.putExtra(PlayAudioActivity.EXTRA_ENCLOSURE_ID, enclosure.mId);
            intent.putExtra(PlayAudioActivity.EXTRA_ITEM_ID, item.mId);
        }
        startActivity( intent );
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putInt(KEY_CURRENT_VIEW, mViewFlipper.getDisplayedChild() );
        outState.putLong(KEY_CURRENT_FEED, mItemsAdapter.getFeedId());
        outState.putLong(KEY_CURRENT_ITEM, mCurrentItem);
        outState.putInt(KEY_FIRST_ITEM, mItemsListView.getFirstVisiblePosition());
        outState.putInt(KEY_FIRST_FEED, mPodcastsListView.getFirstVisiblePosition());
    }
    


    private void updateTitle()
    {
        int child = mViewFlipper.getDisplayedChild();
        
        String newTitle = getResources().getString(R.string.app_name);
        
        if (child == 1 )
        {
            newTitle += ": " + mItemsAdapter.getFeedName();
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
    
    private void onTimer()
    {
        int timeout = updateDisplay();
        
        Globals.LOG.v("PodcastsActivity updateDisplay: timeout: " + timeout);
        
        mHandler.removeCallbacks(mUpdateTask);
        
        if(timeout > 0 )
        {
            mHandler.postDelayed(mUpdateTask, timeout);
        }
    }
    

    private int updateDisplay()
    {
        AudioPlayer player = AudioPlayer.getInstance(this);
        
        if( ! player.hasStarted() )
        {
            return -1;
        }
        
        if( ! player.isPaused() )
        {
            int position = player.getCurrentPosition();
            
            long itemId = player.getItemId();
            
            mItemsAdapter.setItemProgress(itemId, position);
            
            int remaining = 1000 - (position % 1000);
            
            return remaining;
        }
        else
        {
            mItemsAdapter.setPlayingItemId(-1);
            
            return 500;
        }
    }
    
}
