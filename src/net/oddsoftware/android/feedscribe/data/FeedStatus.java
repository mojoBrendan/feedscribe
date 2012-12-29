package net.oddsoftware.android.feedscribe.data;

import java.util.Date;

public class FeedStatus
{
    public long mFeedId;
    public Date mLastHit;
    public int mTTL;
    public String mETag;
    public Date mLastModified;
    public String mLastURL;
    
    public FeedStatus(long feedId, Date lastHit, int ttl, String etag, Date lastModified, String lastURL)
    {
        mFeedId = feedId;
        mLastHit = lastHit;
        mTTL = ttl;
        mETag = etag;
        mLastModified = lastModified;
        mLastURL = lastURL;
    }
    
    public FeedStatus()
    {
        mFeedId = 0;
        mLastHit = new Date(0);
        mTTL = 0;
        mETag = "";
        mLastModified = new Date(0);
        mLastURL = "";
    }
}
