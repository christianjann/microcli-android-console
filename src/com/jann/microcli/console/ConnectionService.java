
package com.jann.microcli.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * This class does all the work for sending and receiving data.
 * It has a thread that listens for incoming packets.
 */
public class ConnectionService
{
    // Debugging
    private static final String TAG = "ConnectionService";
    private static final boolean D = true;

    // Member fields
    private final Handler mHandler;
    private ComThread mConnectedThread;

    Context mContext ;
    /**
     * Constructor. Prepares a new ConnectionService.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public ConnectionService(Context context, Handler handler)
    {
        mContext = context;
        mHandler = handler;
    }

    /**
     * Start the ConnectionService. Specifically start ComThread to begin
     * listening incoming data.
     */
    public synchronized void start()
    {
        if (D) Log.d(TAG, "start");

        mConnectedThread = new ComThread();
        mConnectedThread.start();
    }

    /**
     * Stop thread
     */
    public synchronized void stop()
    {
        if (D) Log.d(TAG, "stop");
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    public void write(byte[] out)
    {
        mConnectedThread.write(out);
    }

    public void sendMessage(String message)
    {
        mConnectedThread.write(new String(message + "\r\n").getBytes());
    }

    /**
     * This thread handles all incoming and outgoing transmissions.
     */
    private class ComThread extends Thread
    {
        private Socket mSocket ;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ComThread()
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                mSocket = new Socket("192.168.1.1", 2000);

                // Get the Socket input and output streams
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Could not make socket", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            try
            {
                BufferedReader buf = new BufferedReader(new InputStreamReader(mmInStream), 1024);
                String line;

                //Listen on socket to receive messages
                while (true)
                {
                    if ((line = buf.readLine()) != null)
                        mHandler.obtainMessage(MicroConsole.MESSAGE_READ,
                                               line.length(), -1, line.getBytes()).sendToTarget();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        /**
          * Write data.
          */
        public void write(byte[] buffer)
        {
            try
            {
                mmOutStream.write(buffer);
                mmOutStream.flush();

                // Share the sent data back to the UI Activity
                mHandler.obtainMessage(MicroConsole.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            try
            {
                mSocket.shutdownInput();
                mSocket.shutdownOutput();
                mmInStream.close();
                mmOutStream.close();
                mSocket.close();
                mSocket = null;
            }
            catch (Exception e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
