package net.oddsoftware.android.feedscribe.data;

public class ShortFeedItem implements Comparable<ShortFeedItem>
{
    public long mId;
    public String mLink;
    public long mPubDate;
    public long mFeedId;
    public String mTitle;
    public String mEnclosureURL;
    public String mGUID;
    public long mFlags;
    
    public ShortFeedItem()
    {
        mId = -1;
        mLink = "";
        mPubDate = 0l;
        mFeedId = 0;
        mTitle = "";
        mEnclosureURL = "";
        mGUID = "";
        mFlags = 0;
    }
    
    public ShortFeedItem(long id, String link, long pubDate, String title, String enclosure, String guid, long flags)
    {
        mId = id;
        mLink = link;
        mPubDate = pubDate;
        mTitle = title;
        mEnclosureURL = enclosure;
        mGUID = guid;
        mFlags = flags;
    }

    @Override
    public int compareTo(ShortFeedItem another)
    {
        if( mPubDate < another.mPubDate)
        {
            return -1;
        }
        else if( mPubDate > another.mPubDate)
        {
            return 1;
        }
        else
        {
            return mLink.compareTo(another.mLink);
        }
    }
}
