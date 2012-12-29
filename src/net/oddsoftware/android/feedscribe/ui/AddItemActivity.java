package net.oddsoftware.android.feedscribe.ui;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flurry.android.FlurryAgent;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedItem;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.html.HttpCache;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AddItemActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.add_item_activity);
        
        Intent intent = getIntent();
        
        if( intent != null && intent.getAction() != null && intent.getAction().equals( Intent.ACTION_VIEW ) )
        {
            EditText textEdit = (EditText) findViewById(R.id.addItemEditText);
            textEdit.setText( intent.getDataString() );
        }
        
        if( intent != null && intent.getAction() != null && intent.getAction().equals( Intent.ACTION_SEND ) )
        {
            String text = intent.getExtras().getString(Intent.EXTRA_TEXT);
            if( text != null )
            {
                Pattern p = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(text);
                if( m.find() )
                {
                    EditText textEdit = (EditText) findViewById(R.id.addItemEditText);
                    textEdit.setText( m.group() );
                }
            }
        }
        
        if( Globals.TRACKING ) FlurryAgent.onEvent("addItemActivity.onCreate");
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
    
    public void onReadNowClicked(View view)
    {
        long itemId = addItem();
        
        if( Globals.TRACKING ) FlurryAgent.onEvent("addItemActivity.readNow");
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "AddItemActivity - read now " + itemId);
        
        if( itemId > 0 )
        {
            Toast.makeText(AddItemActivity.this, R.string.add_item_success, Toast.LENGTH_LONG).show();
            
            Intent intent = new Intent(this, FeedsActivity.class);
            intent.putExtra(FeedsActivity.EXTRA_CMD, FeedsActivity.CMD_NEWS_ITEM );
            intent.putExtra(FeedsActivity.EXTRA_ITEM_ID, itemId);
            // intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        else
        {
            Toast.makeText(AddItemActivity.this, R.string.error_add_item, Toast.LENGTH_LONG).show();
        }
        finish();
    }
    
    public void onReadLaterClicked(View view)
    {
        long itemId = addItem();
        
        if( Globals.TRACKING ) FlurryAgent.onEvent("addItemActivity.readLater");
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "AddItemActivity - read later " + itemId);
        
        if( itemId > 0)
        {
            Toast.makeText(AddItemActivity.this, R.string.add_item_success, Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(AddItemActivity.this, R.string.error_add_item, Toast.LENGTH_LONG).show();
        }
        
        finish();
    }
    
    
    private long addItem()
    {
        EditText editText = (EditText) findViewById(R.id.addItemEditText);
        String uriString = editText.getText().toString();
        
        uriString = uriString.trim();
        
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "onAddClicked - uri is " + uriString);
        
        if( ! uriString.startsWith("http") )
        {
            uriString = "http://" + uriString;
        }
        
        editText.setText( uriString );
        
        FeedManager feedManager = FeedManager.getInstance(this);
        
        Feed localFeed = feedManager.getLocalFeed();
        FeedItem item = new FeedItem();
        
        item.mFeedId = localFeed.mId;
        item.mOriginalLink = uriString;
        item.mLink = uriString;
        item.mPubDate = new Date();
        item.mTitle = uriString;
        item.mCleanTitle = uriString;
        
        feedManager.updateItem(item);
        
        HttpCache httpCache = new HttpCache(this);
        httpCache.seed(item.mLink);
        
        return item.mId;
    }
    
    
    @Override
    protected void onStart()
    {
        if( Globals.TRACKING ) FlurryAgent.onStartSession(this, Globals.FLURRY_KEY);
        super.onStart();
    }
    
    @Override
    protected void onStop()
    {
        if( Globals.TRACKING ) FlurryAgent.onEndSession(this);
        super.onStop();
    }


}
