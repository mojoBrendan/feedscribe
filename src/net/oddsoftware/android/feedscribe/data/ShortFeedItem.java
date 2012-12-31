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
