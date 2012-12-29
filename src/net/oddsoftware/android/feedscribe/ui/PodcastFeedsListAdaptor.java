package net.oddsoftware.android.feedscribe.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedEnclosureInfo;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;

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

public class PodcastFeedsListAdaptor extends BaseAdapter {
    
    private FeedManager mFeedManager = null;
    
    private HashMap<Long, FeedEnclosureInfo> mFeedEnclosures;
    
    private HashMap<Long, Bitmap> mImageMap;
    
    private ArrayList<Feed> mFeeds;
    
    private String mFeedType;
    
    private Context mContext;
    
    public PodcastFeedsListAdaptor(Context ctx, String type)
    {
        mContext = ctx;
        mFeedManager = FeedManager.getInstance(ctx);
        mFeedType = type;
        
        update();
    }
    
    public void update()
    {
        mImageMap = new HashMap<Long, Bitmap>();
        
        mFeedEnclosures = mFeedManager.getFeedEnclosureInfo(mFeedType);
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "got feed enclosure: " + mFeedEnclosures.size() + " type " + mFeedType);
        mFeeds = new ArrayList<Feed>();
        
        ArrayList<Feed> feeds = mFeedManager.getFeeds();
        
        for(Feed feed: feeds)
        {
            if( mFeedEnclosures.containsKey(feed.mId) )
            {
                mFeeds.add(feed);
            }
        }
        
        Collections.sort( mFeeds );
        
        notifyDataSetChanged();
    }
    
    private static class ViewHolder
    {
        TextView mNameTextView;
        TextView mItemCountTextView;
        TextView mNoteTextView;
        ImageView mImageView;
    }
    
    public long getFeedId(int position)
    {
        if( position >= 0 && position < mFeeds.size() )
        {
            return mFeeds.get(position).mId;
        }
        else
        {
            return 0;
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;
        Feed feed = mFeeds.get(position);
        FeedEnclosureInfo info = mFeedEnclosures.get( feed.mId );
        
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
        holder.mItemCountTextView.setText( "" + info.mDownloadedCount );
        holder.mNoteTextView.setText( mContext.getResources().getString(R.string.feed_note, info.mNewCount, info.mUnplayedDownloadCount ));
        
        Bitmap bitmap = mImageMap.get(feed.mId);
        
        if( bitmap == null && feed.mImageURL != null && feed.mImageURL.length() > 0 )
        {
            byte[] imageData = mFeedManager.getImage(feed.mImageURL);
            
            if( imageData != null )
            {
                bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            
                mImageMap.put(feed.mId, bitmap);
            }
        }
        
        if( bitmap != null )
        {
            holder.mImageView.setImageBitmap( bitmap );
        }
        else if( mFeedType.equals("video") )
        {
            holder.mImageView.setImageResource(R.drawable.ic_video_downloaded);
        }
        else
        {
            holder.mImageView.setImageResource(R.drawable.ic_audio_downloaded);
        }
        
        
        return view;
    }


    @Override
    public int getCount()
    {
        return mFeeds.size();
    }

    @Override
    public Object getItem(int position)
    {
        return mFeeds.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return mFeeds.get(position).mId;
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

    public String getFeedNameById(long feedId)
    {
        for(Feed feed: mFeeds)
        {
            if(feed.mId == feedId)
            {
                return feed.mName;
            }
        }
        return null;
    }
}
