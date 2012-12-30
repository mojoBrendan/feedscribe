package net.oddsoftware.android.feedscribe.ui;

import java.io.File;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Enclosure;
import net.oddsoftware.android.feedscribe.data.FeedItem;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

public class PlayVideoActivity extends Activity {
    
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_SEEK_TO = "seek_to";
    public static final String EXTRA_PLAYING = "playing";
    public static final String EXTRA_ENCLOSURE_ID = "enclosure_id";
    public static final String EXTRA_ITEM_ID = "item_id";

    private long mEnclosureId;
    private long mItemId;
    
    private int mSeekTo;
    private boolean mPlaying;
    private boolean mFullscreen;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags( 0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.play_video_activity);
        
        VideoView videoView = (VideoView) findViewById(R.id.videoView1);
        
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView( videoView );
        videoView.setMediaController( mediaController );
        
        videoView.setOnCompletionListener( new OnCompletionListener() {
            
            @Override
            public void onCompletion(MediaPlayer mp) {
                onVideoFinished();
            }
        });
        
        String path = getIntent().getStringExtra(EXTRA_PATH );
        String url = getIntent().getStringExtra(EXTRA_URL);
        
        long seekTo = getIntent().getLongExtra(EXTRA_SEEK_TO, 0);
        mEnclosureId = getIntent().getLongExtra(EXTRA_ENCLOSURE_ID, 0);
        mItemId = getIntent().getLongExtra(EXTRA_ITEM_ID, 0);
        
        if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "PlayVideoActivity - path '" + path + "' url: '" + url + "' seekTo: " + seekTo );
        
        boolean playing = true;
        
        if( savedInstanceState != null )
        {
           seekTo = savedInstanceState.getLong( EXTRA_SEEK_TO, seekTo );
           playing = savedInstanceState.getBoolean( EXTRA_PLAYING, playing );
        }
        
        if( path != null )
        {
            videoView.setVideoURI( Uri.fromFile(new File(path)) );   
        }
        else if( url != null )
        {
            videoView.setVideoURI( android.net.Uri.parse( url ) ); 
        }
        
        if( seekTo != 0 )
        {
            videoView.seekTo( (int) seekTo );
        }
        
        if( playing )
        {
            videoView.start();
        }
        
        mSeekTo = (int)seekTo;
        mPlaying = playing;
        mFullscreen = false;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        VideoView videoView = (VideoView) findViewById(R.id.videoView1);
        
        outState.putLong( EXTRA_SEEK_TO, videoView.getCurrentPosition() );
        outState.putBoolean( EXTRA_PLAYING, videoView.isPlaying() );
    }

    @Override
    protected void onPause()
    {
        VideoView videoView = (VideoView) findViewById(R.id.videoView1);
        
        mSeekTo = videoView.getCurrentPosition();
        mPlaying = videoView.isPlaying();
        
        if( mEnclosureId > 0 )
        {
            FeedManager feedManager = FeedManager.getInstance(this);
            
            Enclosure enclosure = feedManager.getEnclosure( mEnclosureId );
            
            if( enclosure != null )
            {
                enclosure.mPosition = mSeekTo;
                
                // update duration
                long duration = videoView.getDuration();
                if( duration > 0)
                {
                    enclosure.mDuration = duration;
                    
                    // check to see if video is 97% finished and if so, mark it as read
                    if( (enclosure.mPosition * 100 / enclosure.mDuration) >= 97 )
                    {
                        FeedItem item = feedManager.getItemById(mItemId);
                        
                        if( item != null && (item.mFlags & FeedItem.FLAG_READ) == 0 )
                        {
                            item.mFlags = item.mFlags | FeedItem.FLAG_READ;
                            feedManager.updateItemFlags(item);
                        }
                    }
                }
                
                if( enclosure.mPosition >= videoView.getDuration() - 2000)
                {
                    enclosure.mPosition = 0;
                }
                
                feedManager.updateEnclosure( enclosure );
            }
        }
        super.onPause();
    }
    
    private void onVideoFinished()
    {
        if( mItemId > 0 )
        {
            FeedManager feedManager = FeedManager.getInstance(this);
            FeedItem item = feedManager.getItemById(mItemId);
            
            if( item != null && (item.mFlags & FeedItem.FLAG_READ) == 0 )
            {
                item.mFlags = item.mFlags | FeedItem.FLAG_READ;
                feedManager.updateItemFlags(item);
            }
        }
        
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        VideoView videoView = (VideoView) findViewById(R.id.videoView1);
        
        if( mSeekTo != 0 )
        {
            videoView.seekTo( (int) mSeekTo );
        }
        
        if( mPlaying )
        {
            videoView.start();
        }
    }
    
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.play_video_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if( item.getItemId() == R.id.toggle_fullscreen)
        {
            if( mFullscreen )
            {
                mFullscreen = false;
                getWindow().setFlags( 0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR );
            }
            else
            {
                mFullscreen = true;
                getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        
        if( mFullscreen )
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
    
}
