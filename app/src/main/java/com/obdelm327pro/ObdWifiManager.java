package com.obdelm327pro;

/**
 * Created by tbiliyor on 12.01.2017.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class ObdWifiManager extends ObdManager {

    public static final String CMD_WARM_START = "AT WS";
    public static final String CMD_FAST_INIT = "AT FI";
    public static final String CMD_SET_AUTO = "AT SP 00";
    public static final String CMD_DEVICE_DESCRIPTION = "AT @1";
    public static final String CMD_DESCRIBE_PROTOCOL = "AT DP";
    private static final String TAG = "OBDWifiManager";

    private final Context mContext;
    private final HandlerThread mOBDThread;
    private final Handler mOBDHandler;
    private Socket mSocket;
    private boolean mConnecting = false;
    private WifiManager.WifiLock wifiLock;

    private Runnable mConnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
//                        mSocket = new Socket("192.168.0.10", 35000);
                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress("192.168.0.10", 35000), 5000);
                describe();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                fastInit();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                reset();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setAuto();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                describe();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onConnected();
                        }
                    }
                });
                mConnecting = false;
                startRunnable();
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

    public ObdWifiManager(Context context) {
        this.mContext = context;
        mOBDThread = new HandlerThread("OBDII", Thread.NORM_PRIORITY);
        mOBDThread.start();
        mOBDHandler = new Handler(mOBDThread.getLooper());
    }

    public boolean connect() {
        if (mConnecting || isConnected() || sDisconnected){
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


    private void runCommand(String command) throws IOException, InterruptedException, NumberFormatException {
        String rawData;
        byte b;
        InputStream in = mSocket.getInputStream();
        OutputStream out = mSocket.getOutputStream();
        out.write((command + '\r').getBytes());
        out.flush();
        StringBuilder res = new StringBuilder();

        // read until '>' arrives
        long start = System.currentTimeMillis();
        while ((char) (b = (byte) in.read()) != '>' && res.length() < 60 && System.currentTimeMillis() - start < 1000) { // && System.currentTimeMillis()-start<500
            res.append((char) b);
        }

        rawData = res.toString().trim();
    }

    public void fastInit() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
                        Log.d(TAG, "fastInit: ");
                        runCommand(CMD_FAST_INIT);
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void warmStart() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
                        Log.d(TAG, "warmStart: ");
                        runCommand(CMD_WARM_START);
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void reset() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
                        Log.d(TAG, "reset: ");
                        runCommand("AT D");//set Default
                        runCommand("AT Z");//reset
                        runCommand("AT E0");//echo off/on *1
                        runCommand("AT L0");//linefeeds off
                        runCommand("AT S0");//spaces off/on *1
                        setAuto();
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void setAuto() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
                        Log.d(TAG, "setAuto: ");
                        runCommand(CMD_SET_AUTO);
                        runCommand("AT SS");
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void describe() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
                        Log.d(TAG, "describe: ");
                        runCommand(CMD_DEVICE_DESCRIPTION);
                        runCommand(CMD_DESCRIBE_PROTOCOL);
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
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
                        reset();
                        mSocket.close();
                        mSocket = null;
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

    public void disconnect(final boolean reconnect) {
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
        mOBDHandler.removeCallbacksAndMessages(null);
        mConnecting = false;
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSocket != null && mSocket.isConnected()) {
                    try {
                        reset();
                        mSocket.close();
                        mSocket = null;
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
                if (reconnect){
                    connect();
                }
            }
        });
    }

    public void startRunnable() {

    }

    public void stopRunnable() {
    }

    public boolean isConnected() {
        return (mSocket != null && mSocket.isConnected());
    }

    @Override
    public boolean isBluetooth() {
        return false;
    }

    @Override
    public boolean isBle() {
        return false;
    }

    @Override
    public boolean isWifi() {
        return true;
    }

    @Override
    protected boolean isConnecting() {
        return mConnecting;
    }
}