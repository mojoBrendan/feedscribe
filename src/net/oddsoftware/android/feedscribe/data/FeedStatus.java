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
