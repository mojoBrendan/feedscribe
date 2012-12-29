package net.oddsoftware.android.feedscribe.data;

import java.util.Date;

public class FeedItem implements Comparable<FeedItem>
{
    
    public static final int FLAG_DELETED = 0x01;
    public static final int FLAG_STARRED = 0x02;
    public static final int FLAG_READ    = 0x04;
    
    
    public long mId;
    public long mFeedId;
    public String mTitle;
    public String mLink;
    public String mDescription;
    public String mAuthor;
    public String mEnclosureURL;
    public String mGUID;
    public Date   mPubDate;
    public long   mFlags;
    public String mOriginalLink;
    public String mCleanDescription;
    public String mCleanTitle;
    public String mImageURL;
    public Enclosure mEnclosure;
    public long  mPosition;
    
    
    
    public FeedItem()
    {
        mId = -1;
        mFeedId = -1;
        
        mTitle = "";
        mLink = "";
        mDescription = "";
        mAuthor = "";
        mEnclosureURL = "";
        mGUID  = "";
        mPubDate = new Date(0l);
        
        mCleanDescription = mDescription;
        
        mCleanTitle = mTitle;
        
        mImageURL = "";
        
        mFlags = 0;
        
        mPosition = -1;
        
        mEnclosure = null;
    }
    
    public FeedItem(
            long id,
            int feedId,
            String title,
            String cleanTitle,
            String link,
            String originalLink,
            String description,
            String cleanDescription,
            String author,
            String enclosureURL,
            String guid,
            Date pubDate,
            long flags,
            String imageURL,
            long position
        )
    {
        mId = id;
        mFeedId = feedId;
        
        mTitle = title;
        mCleanTitle = cleanTitle;
        mLink = link;
        mOriginalLink = originalLink;
        mDescription = description;
        mCleanDescription = cleanDescription;
        mAuthor = author;
        mEnclosureURL = enclosureURL;
        mGUID  = guid;
        mPubDate = pubDate;
        mFlags = flags;
        mImageURL = imageURL;
        mPosition = position;
        
        mEnclosure = null;
    }

    @Override
    public int compareTo(FeedItem another)
    {
        return mPubDate.compareTo(another.mPubDate);
    }
}
