/*
 * This file is part of MicroCLI.
 * 
 * Copyright (C) 2012 Christian Jann <christian.jann@ymail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jann.microcli.console;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity
 */
public class MicroConsole extends Activity
{
    // Debugging
    private static final String TAG = "MicroConsole";
    private static final boolean D = true;

    // Message types sent from the ConnectionService Handler
    public static final int MESSAGE_READ    = 1;
    public static final int MESSAGE_WRITE   = 2;
    public static final int MESSAGE_TOAST   = 3;

    // Key names received from the ConnectionService Handler
    public static final String TOAST = "toast";

    // Layout Views
    private TextView mTitle;
    private ListView    mConsoleView;
    private EditText    mOutEditText;
    private Button      mSendButton;

    // Array adapter for the console log
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Member object for the connection services
    private ConnectionService mConnectionService = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
    }

    public void onStart()
    {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        setupConsole();
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        mConnectionService.start();
    }

    private void setupConsole()
    {
        Log.d(TAG, "setupConsole()");

        // Initialize the array adapter for the console log
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConsoleView = (ListView) findViewById(R.id.in);
        mConsoleView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                if (D) Log.e(TAG, "[sendButton clicked]");
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the ConnectionService
        if (mConnectionService == null)
            mConnectionService = new ConnectionService(this, mHandler);

        // Initialize the buffer for outgoing messages
        if (mOutStringBuffer == null)
            mOutStringBuffer = new StringBuffer("");
    }

    public synchronized void onPause()
    {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
        if (mConnectionService != null) mConnectionService.stop();
    }

    public void onStop()
    {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
        if (mConnectionService != null) mConnectionService.stop();
    }

    public void onDestroy()
    {
        super.onDestroy();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
        // Stop the ConnectionService
        if (mConnectionService != null) mConnectionService.stop();
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message)
    {
        // Check that there's actually something to send
        if (message.length() > 0)
        {
            try
            {
                mConnectionService.sendMessage(message);

                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                mOutEditText.setText(mOutStringBuffer);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener()
    {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event)
        {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP)
            {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if (D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };


    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode)
        {
        case 0:
            break;

        }// end switch
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.toggle_led3:
            mConnectionService.sendMessage("toggle LED3");
            return true;
        case R.id.enable_toggle_leds:
            mConnectionService.sendMessage("enable TOGGLE_LEDS");
            return true;
        case R.id.disable_toggle_leds:
            mConnectionService.sendMessage("disable TOGGLE_LEDS");
            return true;
        }

        return false;
    }

    // The Handler that gets information back from the ConnectionService
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add("Dev:  " + readMessage);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
}