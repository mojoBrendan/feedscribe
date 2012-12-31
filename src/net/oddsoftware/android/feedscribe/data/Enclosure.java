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

public class Enclosure
{

    public long mId;
    public long mItemId;
    public String mURL;
    public long mLength;
    public String mContentType;
    public String mDownloadPath;
    public long mDownloadTime;
    public long mDuration;
    public long mPosition;
    
    
    /**
     * 
     * @param enclosureId
     * @param itemId
     * @param url
     * @param length
     * @param contentType
     * @param path
     * @param downloadTime
     * @param duration
     * @param position
     */
    public Enclosure(
            long enclosureId, long itemId, String url, long length, String contentType, 
            String path, long downloadTime, long duration, long position)
    {
        mId = enclosureId;
        mItemId = itemId;
        mURL = url;
        mLength = length;
        mContentType = contentType;
        mDownloadPath = path;
        mDownloadTime = downloadTime;
        mDuration = duration;
        mPosition = position;
    }
    
    
    public Enclosure()
    {
        mId = 0;
        mItemId = 0;
        mURL = "";
        mLength = -1;
        mContentType = "";
        mDownloadPath = "";
        mDownloadTime = 0;
        mDuration = 0;
        mPosition = 0;
    }

}
