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

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.utils.LogEntry;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class LogListActivity extends ListActivity
{

    private ArrayAdapter<LogEntry> mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mListAdapter = new ArrayAdapter<LogEntry>(this, android.R.layout.simple_list_item_1);
        
        setListAdapter(mListAdapter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        ArrayList<LogEntry> entries = new ArrayList<LogEntry>();
        Globals.LOG.getEntries(entries);
        
        mListAdapter.clear();
        
        for(LogEntry entry: entries)
        {
            mListAdapter.add(entry);
        }
    }
    
    
    
}
