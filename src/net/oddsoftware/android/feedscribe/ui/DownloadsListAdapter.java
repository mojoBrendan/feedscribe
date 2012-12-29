package net.oddsoftware.android.feedscribe.ui;

import java.util.ArrayList;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Download;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.utils.Utilities;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadsListAdapter extends BaseAdapter {

    ArrayList<Download> mDownloads; 
    
    Context mContext;
    
    public DownloadsListAdapter(Context ctx)
    {
        mContext = ctx;
        update();
    }
    
    public void update()
    {
        mDownloads = FeedManager.getInstance(mContext).getDownloads();
        
        Globals.LOG.v("DownloadsListAdapter.update() - " + mDownloads.size() );
        notifyDataSetChanged();
    }
    
    @Override
    public int getCount()
    {
        return mDownloads.size();
    }

    @Override
    public Object getItem(int position)
    {
        if( position < mDownloads.size())
        {
            return mDownloads.get(position);
        }
        else
        {
            return null;
        }
    }

    @Override
    public long getItemId(int position)
    {
        if( position < mDownloads.size() )
        {
            return mDownloads.get(position).mId;
        }
        else
        {
            return 0;
        }
    }

    private class ViewHolder
    {
        TextView mNameTextView;
        TextView mDurationTextView;
        ProgressBar mProgressBar;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;
        
        if( convertView == null )
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.downloads_list_item, parent, false);
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
            holder.mDurationTextView = (TextView) view.findViewById(R.id.textView2);
            holder.mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            
            view.setTag( holder );
        }
        
        
        if( position < mDownloads.size() )
        {
            Download download = mDownloads.get( position );
            
            holder.mNameTextView.setText( download.mName );
            holder.mDurationTextView.setText( Utilities.formatFileSize( download.mSize ) );
            
            long progress = download.mDownloaded;
            
            long divisor = download.mSize + 1;
            
            if( divisor > 0 )
            {
                progress = progress * 1000 / divisor;
            }
            holder.mProgressBar.setProgress( (int) progress );
        }
        
        return view;
    }
        

    @Override
    public int getItemViewType(int position)
    {
        return 0;
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

}
