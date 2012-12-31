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
import java.util.Collections;
import java.util.HashMap;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedEnclosureInfo;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NewsFeedsListAdapter extends BaseAdapter
{
    private FeedManager mFeedManager = null;
    
    private HashMap<Long, FeedEnclosureInfo> mFeedInfo;
    
    private ArrayList<Feed> mFeeds;
    
    private Context mContext;

    private HashMap<Long, Bitmap> mImageMap;

    public NewsFeedsListAdapter(Context ctx)
    {
        mFeedManager = FeedManager.getInstance(ctx);
        
        mContext = ctx;
        
        update();
    }
    
    public void update()
    {
        mImageMap = new HashMap<Long, Bitmap>();
        mFeedInfo = mFeedManager.getFeedsWithoutEnclosuresInfo();
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "got feed enclosure: " + mFeedInfo.size());
        mFeeds = new ArrayList<Feed>();
    
        ArrayList<Feed> feeds = mFeedManager.getFeeds();
        
        for(Feed feed: feeds)
        {
            if( mFeedInfo.containsKey(feed.mId) )
            {
                mFeeds.add(feed);
            }
        }
        
        Collections.sort( mFeeds );
        
        notifyDataSetChanged();
            
    }

    @Override
    public int getCount()
    {
        return mFeeds.size();
    }

    @Override
    public Object getItem(int position)
    {
        if( position < mFeeds.size() )
        {
            return mFeeds.get(position);
        }
        else
        {
            return null;
        }
    }
    
    @Override
    public int getItemViewType(int position)
    {
        return 0;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public long getItemId(int position)
    {
        if( position < mFeeds.size() )
        {
            return mFeeds.get(position).mId;
        }
        else
        {
            return 0;
        }
    }
    
    private static class ViewHolder
    {
        TextView mNameTextView;
        TextView mItemCountTextView;
        TextView mNoteTextView;
        ImageView mImageView;
    }
    
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;
        Feed feed = mFeeds.get(position);
        FeedEnclosureInfo info = mFeedInfo.get( feed.mId );
        
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
            holder.mItemCountTextView = (TextView) view.findViewById(R.id.textView3);
            holder.mImageView = (ImageView) view.findViewById(R.id.imageView1);
            
            view.setTag( holder );
        }
        
        holder.mNameTextView.setText( feed.mName );
        holder.mItemCountTextView.setText( "" + info.mNewCount);
        holder.mNoteTextView.setText( mContext.getResources().getString(R.string.news_feed_note, info.mNewCount, info.mItemCount) );
        
        Bitmap bitmap = mImageMap.get(feed.mId);
        
        if( bitmap == null && feed.mImageURL != null && feed.mImageURL.length() > 0 )
        {
            byte[] imageData = mFeedManager.getImage(feed.mImageURL);
            if(imageData != null)
            {
                bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            
                mImageMap.put(feed.mId, bitmap);
            }
        }
        
        if( bitmap != null )
        {
            holder.mImageView.setImageBitmap(bitmap);
        }
        else
        {
            holder.mImageView.setImageResource(R.drawable.ic_default_feed);
        }
        
        return view;
    }

    public int getPosition(long feedId)
    {
        for( int i = 0; i < mFeeds.size(); ++i )
        {
            if( mFeeds.get(i).mId == feedId )
            {
                return i;
            }
        }
        return 0;
    }

    public long getFeedId(int position)
    {
        return getItemId(position);
    }

    public String getFeedName(int position)
    {
        if( position < mFeeds.size() )
        {
            return mFeeds.get(position).mName;
        }
        else
        {
            return "";
        }
    }

    public String getFeedNameById(long selectedFeedId)
    {
        for(Feed feed: mFeeds)
        {
            if(feed.mId == selectedFeedId)
            {
                return feed.mName;
            }
        }
        return null;
    }

}

