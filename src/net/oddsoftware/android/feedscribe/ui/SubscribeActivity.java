package net.oddsoftware.android.feedscribe.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flurry.android.FlurryAgent;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.Feed;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.utils.WorkerThread;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SubscribeActivity extends Activity {

    private WorkerThread mWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.subscribe_activity);
        
        Intent intent = getIntent();
        
        if( intent != null && intent.getAction() != null && intent.getAction().equals( Intent.ACTION_VIEW ) )
        {
            EditText textEdit = (EditText) findViewById(R.id.subscribeURLEditText);
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
                    EditText textEdit = (EditText) findViewById(R.id.subscribeURLEditText);
                    textEdit.setText( m.group() );
                }
            }
        }
        
        Linkify.addLinks( (TextView) findViewById(R.id.textView2), Linkify.ALL );
        
        mWorker = new WorkerThread();
        mWorker.start();
    }
    
    @Override
    public void onDestroy()
    {
        mWorker.kill();
        super.onDestroy();
    }
    
    public void onAddClicked(View view)
    {
        EditText editText = (EditText) findViewById(R.id.subscribeURLEditText);
        String uriString = editText.getText().toString();
        
        uriString = uriString.trim();
        
        if( Globals.LOGGING ) Log.d(Globals.LOG_TAG, "onAddClicked - uri is " + uriString);
        
        if( ! uriString.startsWith("http") )
        {
            uriString = "http://" + uriString;
        }
        
        editText.setText( uriString );
        
        addFeed( uriString );
    }
    
    void addFeed(final String uriString)
    {
        final FeedManager feedManager = FeedManager.getInstance(this);
        
        if( Globals.TRACKING )
        {
            HashMap<String, String> args = new HashMap<String, String>();
            args.put("url", uriString);
            FlurryAgent.onEvent("subscribeActivty.addBegin", args);
        }
        
        
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
        findViewById(R.id.subscribeButton).setEnabled(false);
        
        mWorker.addTask( new WorkerThread.Task() {
            
            private boolean ok = false;

            @Override
            public void doInBackground()
            {
                super.doInBackground();
                ok = false;
                
                URL url = null;
                
                try
                {
                    url = new URL( uriString );
                }
                catch(MalformedURLException exc)
                {
                    if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "Error parsing url", exc);
                    return;
                }
                
                ok = feedManager.addFeed( url, null, Feed.TYPE_PODCAST );
            }

            @Override
            public void onPostExecute()
            {
                super.onPostExecute();
                
                ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
                findViewById(R.id.subscribeButton).setEnabled(true);
                
                if( Globals.TRACKING )
                {
                    HashMap<String, String> args = new HashMap<String, String>();
                    args.put("url", uriString);
                    args.put("success", "" + ok);
                    FlurryAgent.onEvent("subscribeActivty.addComplete", args);
                }
                
                if( ok )
                {
                    Toast.makeText(SubscribeActivity.this, R.string.add_feed_success, Toast.LENGTH_LONG).show();
                    finish();
                }
                else
                {
                    Toast.makeText(SubscribeActivity.this, R.string.error_add_feed, Toast.LENGTH_LONG).show();
                    if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "Error adding feed");
                }
            }
        });
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
