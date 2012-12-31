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

import java.util.ArrayList;

import net.oddsoftware.android.feedscribe.data.FeedItem;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedItemEnclosureInfo;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.utils.Utilities;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PodcastItemsListAdapter extends BaseAdapter
{
    
    private Context mContext;
    
    private ArrayList<FeedItemEnclosureInfo> mFeedItemEnclosures;
    
    private FeedManager mFeedManager;
    
    private String mEnclosureType;
    
    private long mFeedId;

    private long mPlayingItemId;

    public PodcastItemsListAdapter(Context ctx, String enclosureType)
    {
        mContext = ctx;
        mFeedManager = FeedManager.getInstance(ctx);
        mEnclosureType = enclosureType;
        mFeedItemEnclosures = new ArrayList<FeedItemEnclosureInfo>();
        mFeedId = 0;
        mPlayingItemId = -1;
    }
    
    public void setFeedId(long feedId)
    {
        mFeedId = feedId;
        update();
    }
    
    public void update()
    {
        mFeedItemEnclosures = mFeedManager.getFeedItemEnclosureInfo(mFeedId, mEnclosureType );
        notifyDataSetInvalidated();
    }

    @Override
    public int getCount() 
    {
        return mFeedItemEnclosures.size();
    }

    @Override
    public Object getItem(int position) 
    {
        if( position < mFeedItemEnclosures.size() )
        {
            return mFeedItemEnclosures.get(position);
        }
        else
        {
            return null;
        }
    }

    @Override
    public long getItemId(int position)
    {
        if( position < mFeedItemEnclosures.size() )
        {
            return mFeedItemEnclosures.get(position).mEnclosureId;
        }
        else
        {
            return -1000;
        }
    }
    
    private class ViewHolder
    {
        TextView mNameTextView;
        TextView mDurationTextView;
        TextView mNoteTextView;
        ImageView mImageView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if( position < 0 && position >= mFeedItemEnclosures.size() )
        {
            return null;
        }
        
        View view = null;
        FeedItemEnclosureInfo info = mFeedItemEnclosures.get( position );
        
        if( convertView == null )
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.feeds_list_item, parent, false);
        }
        else
        {
            view = convertView;
        }
        
        ViewHolder holder = (ViewHolder) view.getTag();
        
        if( holder == null )
        {
            holder = new ViewHolder();
            holder.mNameTextView = (TextView) view.findViewById(R.id.textView1);
            holder.mNoteTextView = (TextView) view.findViewById(R.id.textView2);
            holder.mDurationTextView = (TextView) view.findViewById(R.id.textView3);
            holder.mImageView = (ImageView) view.findViewById(R.id.imageView1);
            
            view.setTag( holder );
        }
        
        holder.mNameTextView.setText( info.mCleanTitle );
        
        String durationText = "";
        
        if( info.mSeekPosition > 500 )
        {
            //durationText = Utilities.formatDuration((int) Math.round(info.mSeekPosition / 1000.0 )) + "\n";
            durationText = Utilities.formatDuration((int) Math.floor(info.mSeekPosition / 1000.0 )) + "\n";
        }
        durationText += Utilities.formatDuration((int) Math.round(info.mDuration / 1000.0 ));
        
        holder.mDurationTextView.setText( durationText );
        holder.mNoteTextView.setText( info.mCleanDescription );
        
        if( info.mDownloaded )
        {
            if( mEnclosureType.equals("video") )
            {
                holder.mImageView.setImageResource(R.drawable.ic_video_downloaded);
            }
            else
            {
                holder.mImageView.setImageResource(R.drawable.ic_audio_downloaded);
            }
        }
        else
        {
            if( mEnclosureType.equals("video") )
            {
                holder.mImageView.setImageResource(R.drawable.ic_video_not_downloaded);
            }
            else
            {
                holder.mImageView.setImageResource(R.drawable.ic_audio_not_downloaded);
            }
        }
        
        if(info.mItemId == mPlayingItemId && mPlayingItemId != -1)
        {
            holder.mImageView.setImageResource(android.R.drawable.ic_media_play);
        }
        
        if( (info.mItemFlags & FeedItem.FLAG_READ) == 0)
        {
            holder.mNameTextView.setTextAppearance(mContext, R.style.ItemTitleUnread);
            view.setBackgroundResource(R.drawable.news_list_item_background_unread);
        }
        else
        {
            holder.mNameTextView.setTextAppearance(mContext, R.style.ItemTitleRead);
            view.setBackgroundResource(R.drawable.news_list_item_background_read);
        }
        
        
        return view;
    }

    @Override
    public int getViewTypeCount()
    {
        return 1;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }
    
    public long getEnclosureId(int position)
    {
        if( position < mFeedItemEnclosures.size() )
        {
            return mFeedItemEnclosures.get(position).mEnclosureId;
        }
        else
        {
            return -1000;
        }
    }
    
    public long getFeedItemId(int position)
    {
        if( position < mFeedItemEnclosures.size() )
        {
            return mFeedItemEnclosures.get(position).mItemId;
        }
        else
        {
            return -1000;
        }
    }

    public long getFeedId()
    {
        return mFeedId;
    }

    public String getFeedName()
    {
        Feed feed = mFeedManager.getFeed( mFeedId );
        if( feed != null)
        {
            return feed.mName;
        }
        else
        {
            return "";
        }
    }
    
    public void setPlayingItemId(long itemId)
    {
        if(mPlayingItemId != itemId)
        {
            mPlayingItemId = itemId;
            notifyDataSetChanged();
        }
    }
    
    public void setItemProgress(long itemId, long progress)
    {
        boolean updated = false;
        
        for(FeedItemEnclosureInfo info: mFeedItemEnclosures)
        {
            if(info.mItemId == itemId)
            {
                info.mSeekPosition = progress;
                updated = true;
                break;
            }
        }
        
        if(updated)
        {
            notifyDataSetChanged();
        }
    }

}
