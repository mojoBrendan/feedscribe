package net.oddsoftware.android.feedscribe.ui;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.R;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.feedscribe.data.FeedSettings;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class FeedSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
    
    public static final String EXTRA_FEED_ID = "feed_id";
    
    private FeedSettings mFeedSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);
        getPreferenceManager().setSharedPreferencesName("tmp");
        
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        
        if( savedInstanceState == null )
        {
            long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, 0);
            FeedSettings feedSettings = FeedManager.getInstance(this).getFeedSettings(feedId);
            
            if( feedSettings != null )
            {
                SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
                mFeedSettings = feedSettings;
                editor.putBoolean("tmp_feed_update_automatic", feedSettings.mUpdateAutomatically);
                editor.putBoolean("tmp_feed_display_full", feedSettings.mDisplayFullArticle);
                editor.putBoolean("tmp_feed_cache_article", feedSettings.mCacheFullArticle);
                editor.putBoolean("tmp_feed_cache_images", feedSettings.mCacheImages);
                editor.putBoolean("tmp_feed_article_textview", feedSettings.mTextify);
                editor.commit();
            }
        }
        
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        setTitle(R.string.preferences_title);
        
        addPreferencesFromResource(R.xml.feed_preferences);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        // steal the preferences back
        if( mFeedSettings != null )
        {
            try
            {
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                mFeedSettings.mUpdateAutomatically = prefs.getBoolean("tmp_feed_update_automatic", mFeedSettings.mUpdateAutomatically);
                mFeedSettings.mDisplayFullArticle = prefs.getBoolean("tmp_feed_display_full",  mFeedSettings.mDisplayFullArticle);
                mFeedSettings.mCacheFullArticle = prefs.getBoolean("tmp_feed_cache_article", mFeedSettings.mCacheFullArticle);
                mFeedSettings.mCacheImages = prefs.getBoolean("tmp_feed_cache_images", mFeedSettings.mCacheImages);
                mFeedSettings.mTextify = prefs.getBoolean("tmp_feed_article_textview", mFeedSettings.mTextify);
                        
                FeedManager.getInstance(this).updateFeedSettings(mFeedSettings);
            }
            catch(ClassCastException exc)
            {
                if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "FeedSettingsActivity.onPause", exc);
            }
        }
    }

}
