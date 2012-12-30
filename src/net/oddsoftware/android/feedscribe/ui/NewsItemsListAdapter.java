package net.oddsoftware.android.feedscribe.ui;

import java.util.ArrayList;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedItem;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.feedscribe.data.ShortFeedItem;
import net.oddsoftware.android.utils.Utilities;

public class NewsItemsListAdapter extends BaseAdapter
{
    
    private FeedManager mFeedManager;
    private Context mContext;
    private ArrayList<ShortFeedItem> mShortFeedItems;
    private long mFeedId;
    
    
    public NewsItemsListAdapter(Context ctx)
    {
        mFeedManager = FeedManager.getInstance(ctx);
        
        mContext = ctx;
        
        mFeedId = 0;
        
        update();
    }
    
    public void setFeedId(long feedId)
    {
        mFeedId = feedId;
        update();
    }

    public void update()
    {
        mShortFeedItems = mFeedManager.getShortItems(mFeedId);
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "got feed enclosure: " + mShortFeedItems.size());
        
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return mShortFeedItems.size();
    }

    

    @Override
    public Object getItem(int position)
    {
        if( position < mShortFeedItems.size() )
        {
            return mShortFeedItems.get(position);
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
        if( position < mShortFeedItems.size() )
        {
            return mShortFeedItems.get(position).mId;
        }
        else
        {
            return 0;
        }
    }
    
    private static class ViewHolder
    {
        TextView mTitleTextView;
        TextView mDateTextView;
        StyleSpan mBoldSpan;
    }
    
    private class StarClickListener implements OnClickListener
    {
        private long mId;
        
        public StarClickListener(long id)
        {
            mId = id;
        }

        @Override
        public void onClick(View v)
        {
            NewsItemsListAdapter.this.onStarClicked(mId);
        }
    }
    
    private void onStarClicked(long itemId)
    {
        if(Globals.LOGGING) Log.d(Globals.LOG_TAG, "onStarClicked: " + itemId);
        
        FeedItem item = mFeedManager.getItemById(itemId);
        
        if( item != null )
        {
            if( (item.mFlags & FeedItem.FLAG_STARRED) == 0)
            {
                item.mFlags |= FeedItem.FLAG_STARRED;
            }
            else
            {
                item.mFlags &= ~(FeedItem.FLAG_STARRED);
            }
            
            mFeedManager.updateItemFlags(item);
            notifyDataSetChanged();
        }
    }
    
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;
        ShortFeedItem shortFeedItem = mShortFeedItems.get(position);
        
        if( convertView == null )
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.news_list_item, parent, false);
        }
        else
        {
            view = convertView;
        }
        
        ViewHolder holder = (ViewHolder) view.getTag();
        
        if( holder == null )
        {
            holder = new ViewHolder();
            holder.mTitleTextView = (TextView) view.findViewById(R.id.TitleText);
            holder.mDateTextView = (TextView) view.findViewById(R.id.DateText);
            holder.mBoldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
            
            view.setTag( holder );
        }
        
        FeedItem feedItem = mFeedManager.getItemById( shortFeedItem.mId );
        
        if( feedItem == null )
        {
            return view;
        }
        
        Spannable text = new SpannableString(feedItem.mCleanTitle + " - " + feedItem.mAuthor);
        text.setSpan( holder.mBoldSpan, 0, feedItem.mCleanTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        holder.mTitleTextView.setText( text );
        holder.mDateTextView.setText( Utilities.formatDate( feedItem.mPubDate ) );
        
        if( (feedItem.mFlags & FeedItem.FLAG_READ) == 0 )
        {
            holder.mTitleTextView.setTextAppearance(mContext, R.style.ItemTitleUnread);
            view.setBackgroundResource(R.drawable.news_list_item_background_unread);
        }
        else
        {
            holder.mTitleTextView.setTextAppearance(mContext, R.style.ItemTitleRead);
            view.setBackgroundResource(R.drawable.news_list_item_background_read);
        }
        
        if( (feedItem.mFlags & FeedItem.FLAG_STARRED) == 0 )
        {
            holder.mDateTextView.setCompoundDrawablesWithIntrinsicBounds(0, android.R.drawable.btn_star_big_off, 0, 0);
        }
        else
        {
            holder.mDateTextView.setCompoundDrawablesWithIntrinsicBounds(0, android.R.drawable.btn_star_big_on, 0, 0);
        }
        
        holder.mDateTextView.setOnClickListener(new StarClickListener(feedItem.mId));
        
        return view;
    }

    public long getFeedId()
    {
        return mFeedId;
    }

    public String getFeedName()
    {
        Feed feed = mFeedManager.getFeed(mFeedId);
        if( feed != null )
        {
            return feed.mName;
        }
        else
        {
            return "";
        }
    }
    

    public long getPreviousItem(long itemId)
    {
        boolean found = false;
        for( int i = mShortFeedItems.size() - 1; i >= 0; --i)
        {
            ShortFeedItem item = mShortFeedItems.get(i);
            if( found )
            {
                return item.mId;
            }
            else
            {
                found = (item.mId == itemId);
            }
        }
        // not found
        return 0;
    }
    
    public long getNextItem(long itemId)
    {
        boolean found = false;
        for( int i = 0; i < mShortFeedItems.size(); ++i)
        {
            ShortFeedItem item = mShortFeedItems.get(i);
            if( found )
            {
                return item.mId;
            }
            else
            {
                found = (item.mId == itemId);
            }
        }
        // not found
        return 0;
    }

}
