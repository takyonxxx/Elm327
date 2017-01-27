package com.obdelm327pro;

/**
 * Created by tbiliyor on 12.01.2017.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ObdWifiManager {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private static final String TAG = "OBDWifiManager";
    private final Context mContext;
    private final HandlerThread mOBDThread;
    private final Handler mWIFIHandler;
    private Socket mSocket;
    private boolean mConnecting = false;
    private WifiManager.WifiLock wifiLock;
    private int mState;

    OutputStream outStream;
    InputStream inStream;

    InetAddress serverAddr = null;
    String serverIpAddress = "192.168.0.10";
    public static final int SERVERPORT = 35000;
    String deviceName = "Elm327";
    private ObdWifiManager.ConnectedThread mConnectedThread;

    private class SocketTask extends AsyncTask<Void, Void, Boolean> {        ;
        IOException ioException;
        Context context;
        SocketTask(Context context) {
            super();
            this.ioException = null;
            this.context = context;
        }
        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(serverIpAddress, SERVERPORT), 5000);
                mSocket.setKeepAlive(true);
                setState(STATE_CONNECTED);
                mConnecting = false;

                // Send the name of the connected device back to the UI Activity
                Message msg = mWIFIHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
                Bundle bundle = new Bundle();
                bundle.putString(MainActivity.DEVICE_NAME, deviceName);
                msg.setData(bundle);
                mWIFIHandler.sendMessage(msg);

                if (mConnectedThread != null) {
                    mConnectedThread.cancel();
                    mConnectedThread = null;
                }

                // Start the thread to manage the connection and perform transmissions
                mConnectedThread = new ObdWifiManager.ConnectedThread(mSocket);
                mConnectedThread.start();

                return true;
            } catch (IOException e) {
                this.ioException = e;
                connectionFailed();
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean result) {

            if (this.ioException != null) {
                new AlertDialog.Builder(context)
                        .setTitle("An error occurrsed")
                        .setMessage(this.ioException.toString())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
            else
            {
                Toast.makeText(context,"Elm327 wifi connected...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mWIFIHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect wifi device");
        msg.setData(bundle);
        mWIFIHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mWIFIHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Wifi device connection was lost");
        msg.setData(bundle);
        mWIFIHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private Runnable mConnectRunnable = new Runnable() {
    @Override
    public void run() {

        if(!isConnected() && mConnecting)
        {

            SocketTask task = new SocketTask(mContext);
            task.execute();
        }

        mWIFIHandler.postDelayed(mConnectRunnable, 10000);
    }
};

    public ObdWifiManager(Context context, Handler handler) {
        this.mContext = context;
        mOBDThread = new HandlerThread("OBDII", Thread.NORM_PRIORITY);
        mOBDThread.start();
        mWIFIHandler = handler;//new Handler(mOBDThread.getLooper());
    }

    public synchronized int getState() {
        return mState;
    }

    private synchronized void setState(int state) {

        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mWIFIHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public boolean connect() {

        setState(STATE_CONNECTING);

        if (mConnecting || isConnected()) {
            return false;
        }

        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiLock == null) {
            this.wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HighPerf wifi lock");
        }

        wifiLock.acquire();
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        String name = wifiInfo.getSSID();

        if (wifi.isWifiEnabled() && (name.contains("OBD") || name.contains("obd") || name.contains("Obd") ||
                name.contains("ELM") || name.contains("elm") || name.contains("Elm") ||
                name.contains("ECU") || name.contains("ecu") || name.contains("Ecu") ||
                name.contains("LINK") || name.contains("link") || name.contains("Link"))) {
            mConnecting = true;
            deviceName = name.replace("\"","");

            mWIFIHandler.removeCallbacksAndMessages(null);
            mWIFIHandler.post(mConnectRunnable);

            return true;
        }

        Message msg = mWIFIHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect wifi device");
        msg.setData(bundle);
        mWIFIHandler.sendMessage(msg);

        setState(STATE_NONE);

        mConnecting = false;
        return false;
    }

    public void disconnect() {

        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();

        mWIFIHandler.removeCallbacksAndMessages(null);
        mConnecting = false;
        mWIFIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSocket != null && mSocket.isConnected()) {
                    try {
                        mSocket.close();
                        mSocket = null;
                        setState(STATE_NONE);
                    } catch (Exception e) {
                        Log.d(TAG, "disconnect: " + Log.getStackTraceString(e));
                    }
                }
            }
        });
    }

    public boolean isConnected() {
        return (mSocket != null && mSocket.isConnected());
    }

    public void write(byte[] out) {
        // Create temporary object
        ObdWifiManager.ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }


    private class ConnectedThread extends Thread {
        private final Socket mmSocket;

        public ConnectedThread(Socket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            readDataFromOBD();
        }

        public void write(byte[] buffer) {
            writeDataToOBD(buffer);
        }

        public void writeDataToOBD(byte[] buffer) {
            try {
                if(mSocket != null)
                {
                    outStream = mSocket.getOutputStream();
                    byte[] arrayOfBytes = buffer;
                    outStream.write(arrayOfBytes);
                    outStream.flush();
                    mWIFIHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                }

            } catch (Exception localIOException1) {
                localIOException1.printStackTrace();
                connectionLost();
            }
        }

        public void readDataFromOBD() {

            while (true) {
                try {
                    if(mSocket != null)
                    {
                        String rawData;
                        byte b;
                        StringBuilder res = new StringBuilder();
                        inStream = mSocket.getInputStream();
                        // read until '>' arrives
                        long start = System.currentTimeMillis();
                        // while ((char) (b = (byte) in.read()) != '>' && res.length() < 60 && System.currentTimeMillis() - start < 1000)
                        while ((char) (b = (byte) inStream.read()) != '>') { // && System.currentTimeMillis()-start<500
                            res.append((char) b);
                        }
                        rawData = res.toString().trim();
                        mWIFIHandler.obtainMessage(MainActivity.MESSAGE_READ, rawData.length(), -1, rawData).sendToTarget();
                    }

                } catch (IOException localIOException) {
                    connectionLost();
                } catch (Exception e) {
                    e.printStackTrace();
                    connectionLost();
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocket + " socket failed", e);
            }
        }
    }
}