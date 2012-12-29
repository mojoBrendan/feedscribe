package net.oddsoftware.android.feedscribe.ui;

import net.oddsoftware.android.feedscribe.Globals;
import android.content.Context;
import android.util.Log;
import android.widget.ViewFlipper;

public class MyViewFlipper extends ViewFlipper {

    public MyViewFlipper(Context context) {
        super(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        try
        {
            super.onDetachedFromWindow();
        }
        catch(IllegalArgumentException exc)
        {
            if( Globals.LOGGING ) Log.w(Globals.LOG_TAG, "viewflipper silliness", exc);
        }
    }
    
    

}
