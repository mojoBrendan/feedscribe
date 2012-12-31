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

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Download;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class DownloadsActivity extends ListActivity
{
    
    private DownloadsListAdapter mDownloadsListAdapter;
    
    private Handler mHandler;
    
    private Runnable mUpdateTask;
    
    private static final int MENU_ITEM_CANCEL = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    
        mDownloadsListAdapter = new DownloadsListAdapter(this);
        setListAdapter( mDownloadsListAdapter );
        
        registerForContextMenu( getListView() );
        
        if( mHandler == null )
        {
            mHandler = new Handler();
        }
        
        if( mUpdateTask == null )
        {
            mUpdateTask = new Runnable() {
                
                @Override
                public void run() {
                    mDownloadsListAdapter.update();
                    mHandler.postDelayed(mUpdateTask, 2000);
                }
            };
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "DownloadsActivity.onResume()");
        
        mDownloadsListAdapter.update();
        
        mHandler.postDelayed(mUpdateTask, 2000);
        
        updateTitle();
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(mUpdateTask);
        super.onPause();
    }
    
    private void updateTitle()
    {
        String newTitle = getResources().getString(R.string.app_name);
        
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
    public boolean onContextItemSelected(MenuItem menuItem)
    {
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "DownloadsActivity: onContextItemSelected " + menuItem);
        
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuItem.getMenuInfo();
        
        if( menuItem.getItemId() == MENU_ITEM_CANCEL)
        {
            Download download = null;
            
            try
            {
                download = (Download) mDownloadsListAdapter.getItem( info.position );
            }
            catch( ClassCastException exc )
            {
                if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "downloadsActivity - onContextItemSelected", exc);
            }
            
            if( download != null )
            {
                download.mCancelled = true;
            }
            
            return true;
        }
        else
        {
            return super.onContextItemSelected(menuItem);
        }
    }
    

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        // AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        
        menu.add(0, MENU_ITEM_CANCEL, 0, R.string.menu_cancel_download);
    }

}
