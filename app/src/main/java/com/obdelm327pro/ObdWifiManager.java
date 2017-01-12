package com.obdelm327pro;

/**
 * Created by tbiliyor on 12.01.2017.
 */

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ObdWifiManager {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private static final String TAG = "OBDWifiManager";
    private final Context mContext;
    private final HandlerThread mOBDThread;
    private final Handler mOBDHandler;
    protected ConcurrentLinkedQueue<ConnectionListener> mConnectionListeners = new ConcurrentLinkedQueue<>();
    protected Handler mUIHandler = new Handler(Looper.getMainLooper());
    private Socket mSocket;
    private boolean mConnecting = false;
    private WifiManager.WifiLock wifiLock;
    private int mState;
    private Runnable mConnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
//                        mSocket = new Socket("192.168.0.10", 35000);
                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress("192.168.0.10", 35000), 5000);

                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onConnected();
                        }
                    }
                });

                setState(STATE_CONNECTED);

                mConnecting = false;
                return;
            } catch (IOException e) {
                e.printStackTrace();
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onDisconnected();
                        }
                    }
                });
            }
            mConnecting = false;
            mOBDHandler.postDelayed(mConnectRunnable, 10000);
        }
    };

    public ObdWifiManager(Context context, Handler handler) {
        this.mContext = context;
        mOBDThread = new HandlerThread("OBDII", Thread.NORM_PRIORITY);
        mOBDThread.start();
        mOBDHandler = handler;//new Handler(mOBDThread.getLooper());
    }

    private synchronized void setState(int state) {

        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mOBDHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public boolean connect() {
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
        if (wifi.isWifiEnabled() && (name.contains("OBD") || name.contains("obd") || name.contains("link") || name.contains("LINK"))) {
            mConnecting = true;
            setState(STATE_CONNECTING);
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (ConnectionListener listener : mConnectionListeners) {
                        listener.onConnecting();
                    }
                }
            });
            mOBDHandler.removeCallbacksAndMessages(null);
            mOBDHandler.post(mConnectRunnable);
            return true;
        }
        mConnecting = false;
        return false;
    }

    public void disconnect() {
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
        mOBDHandler.removeCallbacksAndMessages(null);
        mConnecting = false;
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSocket != null && mSocket.isConnected()) {
                    try {
                        mSocket.close();
                        mSocket = null;
                        setState(STATE_NONE);
                        Log.d(TAG, "disconnect: OBD disconnected.");
                    } catch (Exception e) {
                        Log.d(TAG, "disconnect: " + Log.getStackTraceString(e));
                    }
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onDisconnected();
                        }
                    }
                });
            }
        });
    }

    public void runCommand(String command) throws IOException, InterruptedException, NumberFormatException {
        String rawData;
        byte b;
        InputStream in = mSocket.getInputStream();
        OutputStream out = mSocket.getOutputStream();
        out.write((command + '\r').getBytes());
        out.flush();
        mOBDHandler.obtainMessage(MainActivity.MESSAGE_WRITE, command.length(), -1, command).sendToTarget();
        StringBuilder res = new StringBuilder();

        // read until '>' arrives
        long start = System.currentTimeMillis();
        while ((char) (b = (byte) in.read()) != '>' && res.length() < 60 && System.currentTimeMillis() - start < 1000) { // && System.currentTimeMillis()-start<500
            res.append((char) b);
        }

        rawData = res.toString().trim();
        mOBDHandler.obtainMessage(MainActivity.MESSAGE_READ, rawData.length(), -1, rawData).sendToTarget();

    }

    public boolean isConnected() {
        return (mSocket != null && mSocket.isConnected());
    }

    public interface ConnectionListener {
        void onConnected();

        void onDisconnected();

        void onConnecting();
    }

}