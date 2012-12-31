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
package net.oddsoftware.android.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FlushedInputStream extends FilterInputStream
{
    public FlushedInputStream(InputStream inputStream)
    {
        super(inputStream);
    }

    @Override
    public long skip(long n) throws IOException
    {
        long totalBytesSkipped = 0L;
        while (totalBytesSkipped < n)
        {
            long bytesSkipped = in.skip(n - totalBytesSkipped);
            if (bytesSkipped == 0L)
            {
                  int b = read();
                  if (b < 0)
                  {
                      break;  // we reached EOF
                  }
                  else
                  {
                      bytesSkipped = 1; // we read one byte
                  }
            }
            totalBytesSkipped += bytesSkipped;
        }
        return totalBytesSkipped;
    }
}
