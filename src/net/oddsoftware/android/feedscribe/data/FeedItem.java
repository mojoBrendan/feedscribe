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
