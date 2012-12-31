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
package net.oddsoftware.android.feedscribe.ui;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

import net.oddsoftware.android.feedscribe.*;
import net.oddsoftware.android.feedscribe.data.FeedManager;

public class NewsPreferencesActivity extends PreferenceActivity
{
    
    public static final int DIALOG_CREDITS = 1;
    
    private static final int REQUEST_IMPORT_OPML = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.preferences_title);
        
        addPreferencesFromResource(R.xml.preferences);
    
        findPreference("contact").setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.contact_subject) );
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ getResources().getString(R.string.contact_email) } );
                intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.contact_text) );
                
                try
                {
                    startActivity(intent);
                }
                catch( ActivityNotFoundException exc)
                {
                    Toast.makeText(NewsPreferencesActivity.this, R.string.error_email, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
        
    
        findPreference("credits").setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                showDialog(DIALOG_CREDITS);
                return true;
            }
        });
        
        findPreference("export_opml").setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                doOPMLExport();
                return true;
            }
        });
        
        findPreference("import_opml").setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                doOPMLImport();
                return true;
            }
        });
        
        /*
        findPreference("view_log").setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                startActivity( new Intent(NewsPreferencesActivity.this, LogListActivity.class) );
                return true;
            }
        });
        */
    } 
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
        if( id == DIALOG_CREDITS )
        {
            return createCreditsDialog();
        }
        return super.onCreateDialog(id);
    }
    
    private Dialog createCreditsDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder
            .setMessage(R.string.credits_text)
            .setCancelable(false)
            .setTitle(R.string.credits_title)
            /*
            .setPositiveButton("Changelog", new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            })
            */
            .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        
        return builder.create();
    }
    

    protected void doOPMLExport()
    {
        try
        {
            FeedManager feedManager = FeedManager.getInstance(this);
            String opml = feedManager.exportOPML();
            
            FileWriter writer = new FileWriter( Environment.getExternalStorageDirectory().toString() + "/feedscribe-subscriptions.xml");
            
            writer.append(opml);
            writer.close();
         
            Toast.makeText(this, R.string.msg_opml_export_success, Toast.LENGTH_LONG).show();
            
            return;
        }
        catch(IOException exc)
        {
            if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "NewsPreferencesActivity.doOPMLExport", exc);
        }
        
        Toast.makeText(this, R.string.msg_opml_export_failure, Toast.LENGTH_LONG).show();
    }
    
    protected void doOPMLImport()
    {
        Intent intent = new Intent(this, FileDialog.class);
        intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().toString() );
        
        startActivityForResult(intent, REQUEST_IMPORT_OPML);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if( requestCode == REQUEST_IMPORT_OPML )
        {
            if( resultCode == Activity.RESULT_OK )
            {
                String path = data.getStringExtra(FileDialog.RESULT_PATH);
                if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "NewsPreferencesActivity - importing opml from " + path);
                
                int importCount = -1;
                try
                {
                    importCount = FeedManager.getInstance(this).importOPML(new FileReader(path));
                }
                catch (FileNotFoundException exc)
                {
                    if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "NewsPreferencesActivity - importing opml from " + path, exc);
                }
                
                if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "NewsPreferencesActivity - imported " + importCount + " items from " + path);
                
                if(importCount >= 0 )
                {
                    Toast.makeText(
                            this,
                            String.format(getResources().getString(R.string.msg_opml_import_success), importCount),
                            Toast.LENGTH_LONG
                            ).show();
                }
                else
                {
                    Toast.makeText(this, R.string.msg_opml_import_failure, Toast.LENGTH_LONG).show();
                }
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
