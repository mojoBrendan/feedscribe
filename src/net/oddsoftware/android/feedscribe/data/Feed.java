package net.oddsoftware.android.feedscribe.data;

public class Feed implements Comparable<Feed>
{
    public long mId;
    public int mType;
    public String mName;
    public String mURL;
    public String mLink;
    public String mDescription;
    public String mImageURL;
    
    public final static long DEFAULT_ID = 0;
    
    public final static int TYPE_NEWS = 1;
    public final static int TYPE_PODCAST = 2;
    public final static int TYPE_PHOTO = 4;
    public final static int TYPE_VIDEO = 8;
    
    public final static String SCHEME_LOCAL = "vnd.net.oddsoftware.local";
    public final static String SCHEME_BOOKMARKS = "vnd.net.oddsoftware.gmarks";
    
    /**
     * 
     * @param id
     * @param type
     * @param url
     * @param name
     * @param link
     * @param description
     * @param imageURL
     */
    public Feed(long id, int type, String url, String name, String link, String description, String imageURL) 
    {
        mId = id;
        mType = type;
        mURL = url;
        mName = name;
        mLink = link;
        mDescription = description;
        mImageURL = imageURL;
    }
    
    public Feed(int type)
    {
        this(DEFAULT_ID, type, "", "", "", "", "");
    }

    @Override
    public int compareTo(Feed another) {
        int nameResult = mName.compareTo(another.mName);
        if( mId < 0 || another.mId < 0 || nameResult == 0)
        {
            return (int) (mId - another.mId);
        }
        else
        {
            return nameResult;
        }
    }
    
}
