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
package net.oddsoftware.android.feedscribe;

import java.io.IOException;

import net.oddsoftware.android.feedscribe.data.Enclosure;
import net.oddsoftware.android.feedscribe.data.FeedItem;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;

public class AudioPlayer
{
    private static AudioPlayer mInstance = null;

    public static synchronized AudioPlayer getInstance(Context context)
    {
        if(mInstance == null)
        {
            mInstance  = new AudioPlayer(context.getApplicationContext());
        }
        
        return mInstance;
    }

    private MediaPlayer mMediaPlayer = null;
    private boolean mPaused;
    private boolean mStarted;
    private Context mContext;
    private long mItemId;
    private long mEnclosureId;
    private String mCurrentPath;
    
    NoisyAudioStreamReceiver mNoisyAudioStreamReceiver;
    
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    
    
    private AudioPlayer(Context context)
    {
        mContext = context;
        mPaused = false;
        mStarted = false;
        
        mItemId = -1;
        mEnclosureId = -1;
        
        mNoisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
    }

    private MediaPlayer getMediaPlayer()
    {
        if(mMediaPlayer == null)
        {
            mMediaPlayer = new MediaPlayer();
            
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) { onAudioFinished(); }
            });
        }
        
        return mMediaPlayer;
    }
    
    private void onAudioFinished()
    {
        playbackStopped();
        
        mPaused = false;
        mStarted = false;
        
        if( mItemId > 0 )
        {
            FeedManager feedManager = FeedManager.getInstance(mContext);
            FeedItem item = feedManager.getItemById(mItemId);
            
            if( item != null && (item.mFlags & FeedItem.FLAG_READ) == 0 )
            {
                item.mFlags = item.mFlags | FeedItem.FLAG_READ;
                feedManager.updateItemFlags(item);
            }
            
            Enclosure enclosure = feedManager.getEnclosure( mEnclosureId );
            if( enclosure != null)
            {
                enclosure.mPosition = 0;
                feedManager.updateEnclosure(enclosure);
            }
            
        }
    }
    
    
    public void savePosition()
    {
        if( mEnclosureId > 0 )
        {
            FeedManager feedManager = FeedManager.getInstance(mContext);
            
            Enclosure enclosure = feedManager.getEnclosure( mEnclosureId );
            
            MediaPlayer mediaPlayer = getMediaPlayer();
            
            if( enclosure != null )
            {
                enclosure.mPosition = mediaPlayer.getCurrentPosition();
                
                // update duration
                long duration = mediaPlayer.getDuration();
                if( duration > 0)
                {
                    enclosure.mDuration = duration;
                    
                    // check to see if audio is 97% finished and if so, mark it as read
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
                
                if( enclosure.mPosition >= mediaPlayer.getDuration() - 2000)
                {
                    enclosure.mPosition = 0;
                }
                
                feedManager.updateEnclosure( enclosure );
            }
        }
    }

    public boolean isPaused()
    {
        Globals.LOG.d("AudioPlayer.isPaused: " + mPaused + " " + getMediaPlayer().isPlaying() + " started " + mStarted );
        return mPaused && mStarted;
    }

    public int getCurrentPosition()
    {
        return getMediaPlayer().getCurrentPosition();
    }

    public void seekTo(int position)
    {
        getMediaPlayer().seekTo(position);
    }
    
    public int getDuration()
    {
        return getMediaPlayer().getDuration();
    }

    public void seekBy(int i)
    {
        MediaPlayer mediaPlayer = getMediaPlayer();
        int pos = mediaPlayer.getCurrentPosition();
        pos += i;
        mediaPlayer.seekTo(pos);
    }

    public void pause()
    {
        getMediaPlayer().pause();
        mPaused = true;
        playbackStopped();
    }

    public void resume()
    {
        if(mPaused)
        {
            getMediaPlayer().start();
            mPaused = false;
            playbackStarted();
        }
    }

    public boolean playPath(String path)
    {
        return playThing(path, true);
    }
    
    public boolean playUrl(String url)
    {
        return playThing(url, false);
    }
    

    private boolean playThing(String thing, boolean isLocal)
    {
        try
        {
            MediaPlayer mediaPlayer = getMediaPlayer();
            
            mediaPlayer.reset();
            if( isLocal )
            {
                mediaPlayer.setDataSource(thing);
                mCurrentPath = thing;
            }
            else
            {
                mediaPlayer.setDataSource(mContext, Uri.parse(thing));
                mCurrentPath = null;
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            mediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.start();
            
            playbackStarted();
            
            mPaused = false;
            mStarted = true;
            
            return true;
        }
        catch(IOException exc)
        {
            Globals.LOG.e("Error playing track " + thing + " isLocal " + isLocal, exc);
            
            return false;
        }            
    }

    public void setItem(long itemId, long enclosureId)
    {
        mItemId = itemId;
        mEnclosureId = enclosureId;
    }

    public long getItemId()
    {
        return mItemId;
    }

    public boolean isPlaying()
    {
        return getMediaPlayer().isPlaying();
    }

    public boolean hasStarted()
    {
        return mStarted;
    }
    
    private void playbackStarted()
    {
        mContext.registerReceiver(mNoisyAudioStreamReceiver, intentFilter);
    }
    
    public String getCurrentPath() {
        return mCurrentPath;
    }

    private void playbackStopped()
    {
        // cancel any current playing notification
        savePosition();
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService( Context.NOTIFICATION_SERVICE );
        notificationManager.cancel(Globals.NOTIFICATION_PLAYING);
        
        try
        {
            mContext.unregisterReceiver(mNoisyAudioStreamReceiver);
        }
        catch(IllegalArgumentException exc)
        {
            Globals.LOG.w("AudioPlayer.playbackStopped - Error unregistering receiver", exc);
        }
    }
    
    
    private class NoisyAudioStreamReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                Globals.LOG.d("got headphone plug event");
                
                if(isPlaying())
                {
                    pause();
                }
            }
        }
    }
    
}
