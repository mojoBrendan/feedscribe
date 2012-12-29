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
