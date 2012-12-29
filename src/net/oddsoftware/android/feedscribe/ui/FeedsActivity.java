package net.oddsoftware.android.feedscribe.ui;

import com.flurry.android.FlurryAgent;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.FeedConfig;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.feedscribe.service.FeedService;
import net.oddsoftware.android.feedscribe.service.ScheduleReceiver;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;

public class FeedsActivity extends TabActivity
{

    private int DIALOG_INFO = 1;
    private int DIALOG_FIRSTRUN = 2;
    
    public static final String EXTRA_CMD = "cmd";
    public static final String EXTRA_ITEM_ID = "item_id";
    public static final int CMD_NEWS_ITEM = 1;
    

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        super.onCreate(savedInstanceState);
        
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        intent = new Intent().setClass(this, PodcastsActivity.class);
        intent.putExtra("type", "audio");
        spec = tabHost.newTabSpec("podcasts").setIndicator("Podcasts",
                          res.getDrawable(R.drawable.ic_tab_audio))
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, PodcastsActivity.class);
        intent.putExtra("type", "video");
        spec = tabHost.newTabSpec("videos").setIndicator("Video",
                          res.getDrawable(R.drawable.ic_tab_video))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        intent = new Intent().setClass(this, NewsActivity.class);
        spec = tabHost.newTabSpec("news").setIndicator("News",
                          res.getDrawable(R.drawable.ic_tab_news))
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, DownloadsActivity.class);
        spec = tabHost.newTabSpec("downloads").setIndicator("Downloads",
                          res.getDrawable(R.drawable.ic_tab_download))
                      .setContent(intent);
        tabHost.addTab(spec);

        
        // poke the downloader
        FeedService.downloadAdded(this);
        
        if( savedInstanceState == null )
        {
            if( FeedManager.getInstance(this).getDownloads().size() > 0 )
            {
                tabHost.setCurrentTab(3);
            }
            else
            {
                tabHost.setCurrentTab(1);
            }
            
            processIntent( getIntent() );
        }
        
        if( Globals.TRACKING )
        {
            FlurryAgent.setVersionName(getResources().getString(R.string.version));
        }
        
    }
    
    private void processIntent( Intent intent )
    {
        int command = intent.getIntExtra(EXTRA_CMD, 0);
        
        if( command == CMD_NEWS_ITEM )
        {
            long itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0);
            if( itemId > 0 )
            {
                getTabHost().setCurrentTabByTag("news");
                Activity activity = getCurrentActivity();
                
                if( activity instanceof NewsActivity)
                {
                    ((NewsActivity) activity).launchNewsItem( itemId );
                }
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.podcast_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        Log.e(Globals.LOG_TAG, "FeedsActivity.onPrepareOptionsMenu");
        menu.removeItem(R.id.add);
        menu.removeItem(R.id.refresh);
        menu.removeItem(R.id.info);
        menu.removeItem(R.id.preferences);
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.podcast_list_menu, menu);
            
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if( item.getItemId() == R.id.refresh )
        {
            if( Globals.TRACKING ) FlurryAgent.onEvent("feedsActivity.refresh");
            FeedService.updateFeeds(this, true);
            return true;
        }
        else if( item.getItemId() == R.id.preferences )
        {
            if( Globals.TRACKING ) FlurryAgent.onEvent("feedsActivity.preferences");
            startActivity( new Intent( this, NewsPreferencesActivity.class ) );
            return true;
        }
        else if( item.getItemId() == R.id.info )
        {
            if( Globals.TRACKING ) FlurryAgent.onEvent("feedsActivity.info");
            showDialog( DIALOG_INFO );
            return true;
        }
        else if( item.getItemId() == R.id.add )
        {
            if( Globals.TRACKING ) FlurryAgent.onEvent("feedsActivity.subscribe");
            startActivity( new Intent( this, SubscribeActivity.class ) );
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume()
    {
        ScheduleReceiver.scheduleSync(this);
        
        FeedManager feedManager = FeedManager.getInstance(this);
        if( feedManager.isFirstRun() )
        {
            showDialog(DIALOG_FIRSTRUN);
        }
        
        FeedService.clearNotifications(this);
        FeedConfig.getInstance(this).clearNewItemCount();
        super.onResume();
    }

    @Override
    protected void onStart()
    {
        if( Globals.TRACKING ) FlurryAgent.onStartSession(this, Globals.FLURRY_KEY);
        FlurryAgent.setLogEnabled(Globals.LOGGING);
        super.onStart();
    }
    
    @Override
    protected void onStop()
    {
        if( Globals.TRACKING ) FlurryAgent.onEndSession(this);
        super.onStop();
    }



    @Override
    protected Dialog onCreateDialog(int id)
    {
        if( id == DIALOG_INFO )
        {
            return createInfoDialog();
        }
        else if( id == DIALOG_FIRSTRUN )
        {
            return createFirstRunDialog();
        }
        return super.onCreateDialog(id);
    }
    
    private Dialog createInfoDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
            .setMessage(R.string.about_text)
            .setCancelable(false)
            .setTitle(R.string.about_title)
            .setPositiveButton(R.string.btn_changelog, new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    showDialog(DIALOG_FIRSTRUN);
                }
            })
            .setNegativeButton(R.string.btn_close, new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        
        return builder.create();
    }
    
    private Dialog createFirstRunDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
            .setMessage(R.string.info_text)
            .setCancelable(false)
            .setTitle(R.string.info_title)
            .setNegativeButton(R.string.btn_close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    FeedManager.getInstance(FeedsActivity.this).clearFirstRun();
                }
            });
        
        return builder.create();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        
        processIntent( intent );
    }
    
}
