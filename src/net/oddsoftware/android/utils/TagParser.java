package net.oddsoftware.android.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import net.oddsoftware.android.feedscribe.Globals;


public class TagParser
{
    
    private File mFile;
    private String mReason;

    private byte[] mImageData = null;
    private String mMimeType = null;


    public TagParser(File f)
    {
        mFile = f;
    }
    
    
    public boolean findImage()
    {
        FileInputStream stream = null;
        boolean success = false;
        
        try
        {
            if(mFile != null)
            {
                stream = new FileInputStream(mFile);
            }
        
            if(stream != null)
            {
                success = findImage(stream);
            }
            
            stream.close();
        }
        catch(IOException exc)
        {
            Globals.LOG.w("io error parsing file for image", exc);
        }
        
        
        return success;
    }


    private boolean findImage(FileInputStream stream) throws IOException
    {
        short[] fileHeader = new short[10];
        int pos = 0;
        
        if( ! readAll(stream, fileHeader) )
        {
            mReason = "unable to read header";
            return false;
        }
        
        if( 
                fileHeader[0] != 'I' ||
                fileHeader[1] != 'D' ||
                fileHeader[2] != '3' ||
                fileHeader[3] != 0x03
            )
        {
            mReason = "no header match";
            return false;
        }
        
        int flags = fileHeader[5] << 8;
        
        if( (flags & 0x80) != 0)
        {
            mReason = "unsyncronisation";
            return false;
        }
        
        boolean extendedHeader = false;
        
        if( (flags & 0x40) != 0)
        {
            extendedHeader = true;
        }
        
        if( (flags & 0x1F) != 0)
        {
            mReason = "unknown flags";
            return false;
        }
        
        int tagSize = 0;
        
        for(int i = 6; i < 10; ++i)
        {
            short b = fileHeader[i];
            
            if( (b & 0x80) != 0)
            {
                mReason = "invalid length byte";
                return false;
            }
            tagSize = tagSize << 7;
            tagSize |= b;
        }
        
        if( tagSize > 1024 * 1024)
        {
            mReason = "tag size too large " + tagSize;
            return false;
        }
        
        if(extendedHeader)
        {
            short[] extendedHeaderLength = new short[4];
            
            if(!readAll(stream, extendedHeaderLength))
            {
                mReason = "unable to read extended header length";
                return false;
            }
            
            int len = (extendedHeaderLength[0] << 24) | (extendedHeaderLength[1] << 16) | (extendedHeaderLength[2] << 8) | extendedHeaderLength[3];
            
            stream.skip(len);
            
            pos += 4 + len;
        }
        
        while( pos < tagSize )
        {
            short[] frameHeader = new short[10];
            
            if(! readAll(stream, frameHeader))
            {
                mReason = "unable to read frame at " + pos;
                return false;
            }
            pos += 10;
            
            int frameSize = 
                ( frameHeader[4] << 24 ) | 
                ( frameHeader[5] << 16 ) | 
                ( frameHeader[6] <<  8 ) | 
                ( frameHeader[7] <<  0 );
            
            if (
                    frameHeader[0] == 'A' &&
                    frameHeader[1] == 'P' &&
                    frameHeader[2] == 'I' &&
                    frameHeader[3] == 'C'
                    )
            {
                int textEncoding = stream.read();
                String mimeType = readString(stream, 0);
                int pictureType = stream.read();
                String description = readString(stream, textEncoding);
                
                if( textEncoding == -1 || pictureType == -1 || mimeType == null || description == null)
                {
                    mReason = "unable to read picture frame";
                    return false;
                }
                
                int framePos = 3; // textEncoding + pictureType + null from mime type
                framePos += mimeType.length();
                framePos += description.length() + 1;
                if(textEncoding == 1) // unicode? then add more
                {
                    framePos += description.length() + 1;
                }
                
                int remaining = frameSize - framePos;
                
                if( (remaining < 1024 * 1024) && (remaining > 0) )
                {
                    mImageData = new byte[remaining];
                    if( readAll(stream, mImageData) )
                    {
                        mMimeType = mimeType;
                        return true;
                    }
                    else
                    {
                        mReason = "unable to read picture data";
                        return false;
                    }
                }
                
                pos += frameSize;
            }
            else
            {
                stream.skip(frameSize);
                pos += frameSize;
            }
        }
        
        return false;
    }

    private boolean readAll(FileInputStream stream, short[] bytes) throws IOException
    {
        for(int i = 0; i < bytes.length; ++i)
        {
            int b = stream.read();
            if(b == -1)
            {
                return false;
            }
            else
            {
                bytes[i] = (short) b;
            }
        }
        return true;
    }
    
    private boolean readAll(FileInputStream stream, byte[] buffer) throws IOException
    {
        int pos = 0;
        
        while(pos < buffer.length)
        {
            int retval = stream.read(buffer, pos, buffer.length - pos);
            
            if( retval == -1)
            {
                return false;
            }
            else
            {
                pos += retval;
            }
        }
        
        return true;
    }
    
    private String readString(FileInputStream stream, int encoding) throws IOException
    {
        StringBuilder builder = new StringBuilder();
        if(encoding == 0)
        {
            while(true)
            {
                int b = stream.read();
                if( b == -1)
                {
                    return null;
                }
                else if( b == 0 )
                {
                    return builder.toString();
                }
                else
                {
                    builder.append('.');
                }
            }
        }
        else if(encoding == 1)
        {
            while(true)
            {
                int b1 = stream.read();
                int b2 = stream.read();
                if( b1 == -1 || b2 == -1)
                {
                    return null;
                }
                else if( b1 == 0 && b2 == 0)
                {
                    return builder.toString();
                }
                else
                {
                    builder.append('.');
                }
            }
        }
        return null;
    }


    public String getImageMimeType()
    {
        return mMimeType;
    }


    public byte[] getImageData()
    {
        return mImageData;
    }
    
    public String getReason()
    {
        return mReason;
    }

}
