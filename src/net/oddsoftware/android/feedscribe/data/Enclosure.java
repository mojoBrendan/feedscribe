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
