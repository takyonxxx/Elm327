package com.obdelm327pro;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.PowerManager;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Message;
import android.os.Handler;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;

import java.util.ArrayList;
import java.util.List;

import com.obdelm327pro.R;

public class MainActivity extends AppCompatActivity {

    private PowerManager.WakeLock wl;
    private Menu menu;
    private EditText mOutEditText;
    private Button mSendButton, mPidsButton, mTroublecodes, mClearTroublecodes, mClearlist;
    private ListView mConversationView;
    private TextView Load, Fuel, Volt, Temp, Status, Loadtext, Volttext, Temptext, Centertext, Info, Airtemp_text, Airtemp, Maf_text, Maf;
    private String mConnectedDeviceName = null;

    GaugeSpeed speed;
    GaugeRpm rpm;

    BluetoothDevice currentdevice;
    boolean commandmode = false, initialized = false, m_getPids = false, tryconnect = false;

    String devicename, deviceprotocol, lastsend, tmpmsg;

    private int rpmval = 0, currenttemp = 0, intakeairtemp = 0, contmodulevolt = 0, ambientairtemp = 0,
            engineoiltemp = 0, b1s1temp = 0, Enginetype = 0, FaceColor = 0,
            whichCommand = 0, m_dedectPids = 0, connectcount = 0, trycount = 0;
    private double  Enginedisplacement = 1500;


    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mChatService = null;

    final List<String> commandslist = new ArrayList<String>();
    final List<Double> avgconsumption = new ArrayList<Double>();

    //String[] commandslist;
    String[] initializeCommands;

    private static final String[] PIDS = {
            "01", "02", "03", "04", "05", "06", "07", "08",
            "09", "0A", "0B", "0C", "0D", "0E", "0F", "10",
            "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "1A", "1B", "1C", "1D", "1E", "1F", "20"};

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    Intent serverIntent = null;

    String VOLTAGE = "ATRV",
            PROTOCOL = "ATDP",
            ENGINE_COOLANT_TEMP = "0105",  //A-40
            ENGINE_RPM = "010C",  //((A*256)+B)/4
            ENGINE_LOAD = "0104",  // A*100/255
            VEHICLE_SPEED = "010D",  //A
            INTAKE_AIR_TEMP = "010F",  //A-40
            MAF_AIR_FLOW = "0110", //MAF air flow rate 0 - 655.35	grams/sec ((256*A)+B) / 100  [g/s]
            ENGINE_OIL_TEMP = "015C",  //A-40
            FUEL_RAIL_PRESSURE = "0122", // ((A*256)+B)*0.079
            INTAKE_MAN_PRESSURE = "010B", //Intake manifold absolute pressure 0 - 255 kPa
            CONT_MODULE_VOLT = "0142",  //((A*256)+B)/1000
            AMBIENT_AIR_TEMP = "0146",  //A-40
            CATALYST_TEMP_B1S1 = "013C",  //(((A*256)+B)/10)-40
            STATUS_DTC = "0101", //Status since DTC Cleared
            THROTTLE_POSITION = "0111", //Throttle position 0 -100 % A*100/255
            OBD_STANDARDS = "011C", //OBD standards this vehicle
            PIDS_SUPPORTED = "0120"; //PIDs supported

    /*PIDS_SUPPORTED: For example, if the car response is BE1FA813, it can be decoded like this:
    Hexadecimal 	B 	E 	1 	F 	A 	8 	1 	3
    Binary 	1 	0 	1 	1 	1 	1 	1 	0 	0 	0 	0 	1 	1 	1 	1 	1 	1 	0 	1 	0 	1 	0 	0 	0 	0 	0 	0 	1 	0 	0 	1 	1
    Supported? 	Yes 	No 	Yes 	Yes 	Yes 	Yes 	Yes 	No 	No 	No 	No 	Yes 	Yes 	Yes 	Yes 	Yes 	Yes 	No 	Yes 	No 	Yes 	No 	No 	No 	No 	No 	No 	Yes 	No 	No 	Yes 	Yes
    PID number 	01 	02 	03 	04 	05 	06 	07 	08 	09 	0A 	0B 	0C 	0D 	0E 	0F 	10 	11 	12 	13 	14 	15 	16 	17 	18 	19 	1A 	1B 	1C 	1D 	1E 	1F 	20

    So, supported PIDs are: 01, 03, 04, 05, 06, 07, 0C, 0D, 0E, 0F, 10, 11, 13, 15, 1C, 1F and 20*/

    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;

    private static boolean actionbar = true;

    private static final float APPBAR_ELEVATION = 14f;

    Toolbar toolbar;
    AppBarLayout appbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        appbar = (AppBarLayout) findViewById(R.id.appbar);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire();

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Status = (TextView) findViewById(R.id.Status);
        Load = (TextView) findViewById(R.id.Load);
        Fuel = (TextView) findViewById(R.id.Fuel);
        Temp = (TextView) findViewById(R.id.Temp);
        Volt = (TextView) findViewById(R.id.Volt);
        Loadtext = (TextView) findViewById(R.id.Load_text);
        Temptext = (TextView) findViewById(R.id.Temp_text);
        Volttext = (TextView) findViewById(R.id.Volt_text);
        Centertext = (TextView) findViewById(R.id.Center_text);
        Info = (TextView) findViewById(R.id.info);
        Airtemp_text = (TextView) findViewById(R.id.Airtemp_text);
        Airtemp = (TextView) findViewById(R.id.Airtemp);
        Maf_text = (TextView) findViewById(R.id.Maf_text);
        Maf = (TextView) findViewById(R.id.Maf);
        speed = (GaugeSpeed) findViewById(R.id.GaugeSpeed);
        rpm = (GaugeRpm) findViewById(R.id.GaugeRpm);

        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mPidsButton = (Button) findViewById(R.id.button_pids);
        mSendButton = (Button) findViewById(R.id.button_send);
        mClearTroublecodes = (Button) findViewById(R.id.button_clearcodes);
        mClearlist = (Button) findViewById(R.id.button_clearlist);
        mTroublecodes = (Button) findViewById(R.id.button_troublecodes);
        mConversationView = (ListView) findViewById(R.id.in);

        invisiblecmd();

        //ATZ reset all
        //ATDP Describe the current Protocol
        //ATAT0-1-2 Adaptive Timing Off - daptive Timing Auto1 - daptive Timing Auto2
        //ATE0-1 Echo Off - Echo On
        //ATSP0 Set Protocol to Auto and save it
        //ATMA Monitor All
        //ATL1-0 Linefeeds On - Linefeeds Off
        //ATH1-0 Headers On - Headers Off
        //ATS1-0 printing of Spaces On - printing of Spaces Off
        //ATAL Allow Long (>7 byte) messages

        initializeCommands = new String[]{"ATZ", "ATDP", "ATS0", "ATL0", "ATH1", "ATS1", "ATE0", "ATAL","ATRV"};
        //initializeCommands = new String[]{"ATZ","ATDP","ATS0","ATL0","ATAT0","ATST10","ATSPA0","ATE0"};

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();

            return;
        }
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                mChatService.start();
            }
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupChat();
        }

        mPidsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String sPIDs = "0100";
                m_getPids = false;
                sendMessage(sPIDs);
            }
        });
        // Initialize the send button with a listener that for click events

        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                String message = mOutEditText.getText().toString();
                sendMessage(message);
            }
        });

        mClearTroublecodes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String clearCodes = "04";
                sendMessage(clearCodes);
            }
        });

        mClearlist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConversationArrayAdapter.clear();
            }
        });

        mTroublecodes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String troubleCodes = "03";
                sendMessage(troubleCodes);
            }
        });

        mOutEditText.setOnEditorActionListener(mWriteListener);

        RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.mainscreen);
        rlayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    ActionBar actionBar = getSupportActionBar();
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) Status.getLayoutParams();
                    if (actionbar) {
                        //toolbar.setVisibility(View.GONE);
                        actionBar.hide();
                        actionbar = false;

                        lp.setMargins(0, 5, 0, 0);
                    } else {
                        //toolbar.setVisibility(View.VISIBLE);
                        actionBar.show();
                        actionbar = true;

                        lp.setMargins(0, Integer.parseInt("?android:attr/actionBarSize"), 0, 0);
                    }

                    setgaugesize();

                    Status.setLayoutParams(lp);

                } catch (Exception e) {
                }
            }
        });

        speed.setTargetValue(220);
        rpm.setTargetValue(80);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speed.setTargetValue(0);
                        rpm.setTargetValue(0);
                    }
                });
            }
        }).start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        this.menu = menu;

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_connect_scan:

                if (item.getTitle().equals("Connect")) {
                    // Launch the DeviceListActivity to see devices and do scan
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    if (mChatService != null) mChatService.stop();
                    item.setTitle("Connect");
                    Status.setText("Not Connected");
                    resetvalues();
                }

                return true;
            case R.id.menu_command:

                //mConversationArrayAdapter.clear();

                if (item.getTitle().equals("Terminal")) {
                    commandmode = true;
                    visiblecmd();
                    item.setTitle("Gauges");
                } else {
                    invisiblecmd();
                    item.setTitle("Terminal");
                    commandmode = false;
                    resetvalues();
                }
                return true;

            case R.id.menu_settings:

                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, Prefs.class);
                startActivity(serverIntent);

                return true;
            case R.id.menu_exit:
                exit();

                return true;
            case R.id.menu_reset:

                resetvalues();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == MainActivity.RESULT_OK) {
                    connectDevice(data);
                }
                break;

            case REQUEST_ENABLE_BT:

                if (resultCode == MainActivity.RESULT_OK) {
                    if (mChatService == null) setupChat();
                } else {
                    Toast.makeText(this, "BT not enabled", Toast.LENGTH_SHORT).show();
                    if (mChatService == null) setupChat();
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setDefaultOrientation();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatService != null) mChatService.stop();

        wl.release();
    }

    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Elm327") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferences();
        setDefaultOrientation();

        if (initialized) {
            resetvalues();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (!commandmode) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage("Are you sure you want exit?");
                alertDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                exit();
                            }
                        });

                alertDialogBuilder.setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else {
                commandmode = false;
                invisiblecmd();
                MenuItem item = menu.findItem(R.id.menu_command);
                item.setTitle("Terminal");
                resetvalues();
            }

            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    ///////////////////////////////////////////////////////////////////////

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message);
                    }
                    return true;
                }
            };

    private void exit() {
        if (mChatService != null) mChatService.stop();
        wl.release();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void getPreferences() {
        try {
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());

            FaceColor = Integer.parseInt(preferences.getString("FaceColor", "0"));

            rpm.setFace(FaceColor);
            speed.setFace(FaceColor);

            Enginedisplacement = Integer.parseInt(preferences.getString("Enginedisplacement", "0"));
            Enginetype = Integer.parseInt(preferences.getString("EngineType", "0"));

            Enginedisplacement = Enginedisplacement / 1000;

            //Toast.makeText(this,String.valueOf(Enginedisplacement),Toast.LENGTH_SHORT).show();

            m_dedectPids = Integer.parseInt(preferences.getString("DedectPids", "0"));

            rpm.refreshDrawableState();
            speed.refreshDrawableState();
            rpm.invalidate();
            speed.invalidate();

            if (m_dedectPids == 0) {
                commandslist.clear();

                int i = 0;

                if (preferences.getBoolean("checkboxVOLTAGE", false)) {
                    commandslist.add(i, VOLTAGE);
                    i++;
                }

                if (preferences.getBoolean("checkboxENGINE_RPM", false)) {
                    commandslist.add(i, ENGINE_RPM);
                    i++;
                }

                if (preferences.getBoolean("checkboxVEHICLE_SPEED", false)) {
                    commandslist.add(i, VEHICLE_SPEED);
                    i++;
                }

                if (preferences.getBoolean("checkboxENGINE_LOAD", false)) {
                    commandslist.add(i, ENGINE_LOAD);
                    i++;
                }

                if (preferences.getBoolean("checkboxENGINE_COOLANT_TEMP", false)) {
                    commandslist.add(i, ENGINE_COOLANT_TEMP);
                    i++;
                }

                if (preferences.getBoolean("checkboxINTAKE_AIR_TEMP", false)) {
                    commandslist.add(i, INTAKE_AIR_TEMP);
                    i++;
                }

                if (preferences.getBoolean("checkboxMAF_AIR_FLOW", false)) {
                    commandslist.add(i, MAF_AIR_FLOW);
                }

                if (preferences.getBoolean("checkboxINTAKE_MAN_PRESSURE", false)) {
                    commandslist.add(i, INTAKE_MAN_PRESSURE);
                }

                if (preferences.getBoolean("checkboxENGINE_OIL_TEMP", false)) {
                    commandslist.add(i, ENGINE_OIL_TEMP);
                }

                if (preferences.getBoolean("checkboxFUEL_RAIL_PRESSURE", false)) {
                    commandslist.add(i, FUEL_RAIL_PRESSURE);
                }
            }

        } catch (Exception e) {
        }
    }

    private void setDefaultOrientation() {

        try {

            settextsixe();
            setgaugesize();

        } catch (Exception e) {
        }
    }

    private void hideVirturalKeyboard() {
        try {

            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        } catch (Exception e) {
        }
    }

    private void settextsixe() {
        int txtsize = 16;
        int sttxtsize = 12;
        int fueltxtsize = 16;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int densityDpi = displayMetrics.densityDpi;
        //Status.setText(String.valueOf(densityDpi));

        if (densityDpi <= DisplayMetrics.DENSITY_XXHIGH) {
            Status.setTextSize(sttxtsize);
            Fuel.setTextSize(fueltxtsize);
            Temp.setTextSize(txtsize);
            Load.setTextSize(txtsize);
            Volt.setTextSize(txtsize);
            Temptext.setTextSize(txtsize);
            Loadtext.setTextSize(txtsize);
            Volttext.setTextSize(txtsize);
            Airtemp_text.setTextSize(txtsize);
            Airtemp.setTextSize(txtsize);
            Maf_text.setTextSize(txtsize);
            Maf.setTextSize(txtsize);
            Info.setTextSize(sttxtsize);
        } else {
            Status.setTextSize(sttxtsize * 3 / 2);
            Fuel.setTextSize(fueltxtsize * 3 / 2);
            Temp.setTextSize(txtsize * 3 / 2);
            Load.setTextSize(txtsize * 3 / 2);
            Volt.setTextSize(txtsize * 3 / 2);
            Temptext.setTextSize(txtsize * 3 / 2);
            Loadtext.setTextSize(txtsize * 3 / 2);
            Volttext.setTextSize(txtsize * 3 / 2);
            Info.setTextSize(sttxtsize * 3 / 2);
            Airtemp_text.setTextSize(txtsize * 3 / 2);
            Airtemp.setTextSize(txtsize * 3 / 2);
            Maf_text.setTextSize(txtsize * 3 / 2);
            Maf.setTextSize(txtsize * 3 / 2);
        }
    }

    public void invisiblecmd() {
        mConversationView.setVisibility(View.INVISIBLE);
        mOutEditText.setVisibility(View.INVISIBLE);
        mSendButton.setVisibility(View.INVISIBLE);
        mPidsButton.setVisibility(View.INVISIBLE);
        mTroublecodes.setVisibility(View.INVISIBLE);
        mClearTroublecodes.setVisibility(View.INVISIBLE);
        mClearlist.setVisibility(View.INVISIBLE);
        rpm.setVisibility(View.VISIBLE);
        speed.setVisibility(View.VISIBLE);
        Load.setVisibility(View.VISIBLE);
        Fuel.setVisibility(View.VISIBLE);
        Volt.setVisibility(View.VISIBLE);
        Temp.setVisibility(View.VISIBLE);
        Loadtext.setVisibility(View.VISIBLE);
        Volttext.setVisibility(View.VISIBLE);
        Temptext.setVisibility(View.VISIBLE);
        Centertext.setVisibility(View.VISIBLE);
        Info.setVisibility(View.VISIBLE);
        Airtemp_text.setVisibility(View.VISIBLE);
        Airtemp.setVisibility(View.VISIBLE);
        Maf_text.setVisibility(View.VISIBLE);
        Maf.setVisibility(View.VISIBLE);
    }

    public void visiblecmd() {
        rpm.setVisibility(View.INVISIBLE);
        speed.setVisibility(View.INVISIBLE);
        Load.setVisibility(View.INVISIBLE);
        Fuel.setVisibility(View.INVISIBLE);
        Volt.setVisibility(View.INVISIBLE);
        Temp.setVisibility(View.INVISIBLE);
        Loadtext.setVisibility(View.INVISIBLE);
        Volttext.setVisibility(View.INVISIBLE);
        Temptext.setVisibility(View.INVISIBLE);
        Centertext.setVisibility(View.INVISIBLE);
        Info.setVisibility(View.INVISIBLE);
        Airtemp_text.setVisibility(View.INVISIBLE);
        Airtemp.setVisibility(View.INVISIBLE);
        Maf_text.setVisibility(View.INVISIBLE);
        Maf.setVisibility(View.INVISIBLE);
        mConversationView.setVisibility(View.VISIBLE);
        mOutEditText.setVisibility(View.VISIBLE);
        mSendButton.setVisibility(View.VISIBLE);
        mPidsButton.setVisibility(View.VISIBLE);
        mTroublecodes.setVisibility(View.VISIBLE);
        mClearTroublecodes.setVisibility(View.VISIBLE);
        mClearlist.setVisibility(View.VISIBLE);
    }

    private void setgaugesize() {
        Display display = getWindow().getWindowManager().getDefaultDisplay();
        int width = 0;
        int height = 0;

        width = display.getWidth();
        height = display.getHeight();

        if (actionbar) {
            width = 3 * width / 4;
            height = 3 * height / 4;
        } else {
            width = 4 * width / 5;
            height = 4 * height / 5;
        }

        if (width > height) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(height, height);

            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Load).getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.setMargins(100, 50, 100, 0);
            rpm.setLayoutParams(lp);

            lp = new RelativeLayout.LayoutParams(height, height);
            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Load).getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.setMargins(100, 50, 100, 0);
            speed.setLayoutParams(lp);
        } else if (width < height) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, width);

            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Fuel).getId());
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.setMargins(25, 5, 25, 5);
            speed.setLayoutParams(lp);

            lp = new RelativeLayout.LayoutParams(width, width);
            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.GaugeSpeed).getId());
            //lp.addRule(RelativeLayout.ABOVE,findViewById(R.id.info).getId());
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.setMargins(25, 5, 25, 5);
            rpm.setLayoutParams(lp);
        }
    }

    public void resetvalues() {
        Load.setText("0 %");
        Volt.setText("0 V");
        Temp.setText("0 C°");
        Info.setText("");
        Airtemp.setText("0 C°");
        Maf.setText("0 g/s");
        initialized = false;
        m_getPids = false;
        whichCommand = 0;
        trycount = 0;

        speed.setTargetValue(220);
        rpm.setTargetValue(80);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speed.setTargetValue(0);
                        rpm.setTargetValue(0);
                    }
                });
            }
        }).start();

        MainActivity.this.sendMessage("ATZ");
    }

    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            // Attempt to connect to the device
            mChatService.connect(device);
            tryconnect = true;
            currentdevice = device;

        } catch (Exception e) {
        }
    }

    private void setupChat() {

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            if (message.length() > 0) {

                message = message + "\r";
                // Get the message bytes and tell the BluetoothChatService to write
                byte[] send = message.getBytes();
                mChatService.write(send);
                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                //mOutEditText.setText(mOutStringBuffer);
            }
        } catch (Exception e) {
        }
    }


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:

                            Status.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
                            Info.setText("Connected.");

                            MenuItem itemtemp = menu.findItem(R.id.menu_connect_scan);
                            itemtemp.setTitle("Disconnect");

                            avgconsumption.clear();
                            Fuel.setText("0 - 0 L/h");
                            m_getPids = false;
                            speed.setTargetValue(0);
                            rpm.setTargetValue(0);
                            mConversationArrayAdapter.clear();
                            tryconnect = false;

                            MainActivity.this.sendMessage("ATZ");

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                            Info.setText("Trying to connect.");
                            break;
                        case BluetoothService.STATE_LISTEN:

                        case BluetoothService.STATE_NONE:

                            Status.setText(R.string.title_not_connected);

                            itemtemp = menu.findItem(R.id.menu_connect_scan);
                            itemtemp.setTitle("Connect");

                            avgconsumption.clear();
                            Fuel.setText("0 - 0 L/h");
                            m_getPids = false;

                            if (tryconnect) {
                                mChatService.connect(currentdevice);
                                connectcount++;
                                if (connectcount >= 2) {
                                    tryconnect = false;
                                }
                            }

                            resetvalues();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    if (!initialized) {
                        if (whichCommand == initializeCommands.length - 1) {
                            initialized = true;
                            whichCommand = 0;
                        } else {
                            whichCommand++;
                        }
                    } else {
                        if (whichCommand >= commandslist.size() - 1) {
                            whichCommand = 0;
                        } else {
                            whichCommand++;
                        }
                    }

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    mConversationArrayAdapter.add("Command:  " + writeMessage);

                    break;
                case MESSAGE_READ:

                    try {

                        tmpmsg = msg.obj.toString();

                        tmpmsg = tmpmsg.replace("null", "");
                        tmpmsg = tmpmsg.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
                        tmpmsg = tmpmsg.replaceAll(">", "");
                        tmpmsg = tmpmsg.replaceAll("SEARCHING...", "");


                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);

                        try {

                            int index = tmpmsg.indexOf("41");

                            String pidmsg = tmpmsg.substring(index, tmpmsg.length());

                            if (pidmsg.contains("4100")) {

                                setPidsSupported(pidmsg);
                                return;
                            }

                        } catch (Exception e) {
                        }


                        if (tmpmsg.indexOf("V") != -1)//battery voltage
                        {
                            if (tmpmsg.length() <= 5 && tmpmsg.contains(".")) {
                                Volt.setText(tmpmsg);
                            }
                        }

                        if (!initialized) {
                            if (msg.obj.toString().contains("ELM")) {

                                tmpmsg = tmpmsg.replaceAll("ATZ", "");
                                tmpmsg = tmpmsg.replaceAll("ATI", "");

                                devicename = tmpmsg;

                                //Status.setText(devicename);
                            } else if (msg.obj.toString().contains("SAE") || msg.obj.toString().contains("ISO")) {

                                tmpmsg = tmpmsg.replaceAll("ATDP", "");
                                tmpmsg = tmpmsg.replaceAll("atdp", "");

                                deviceprotocol = tmpmsg;

                                Status.setText(devicename + " " + deviceprotocol);
                            }

                            String send = initializeCommands[whichCommand];
                            MainActivity.this.sendMessage(send);

                        } else {


                            if (!m_getPids && m_dedectPids == 1 && !commandmode) {
                                String sPIDs = "0100";
                                MainActivity.this.sendMessage(sPIDs);
                                Info.setText("Trying to get available pids : " + String.valueOf(trycount));
                                trycount++;
                                return;
                            }

                            if (commandmode) {
                                String strerror = "no error";
                                if (tmpmsg.substring(0, 2).equals("43")) {
                                    strerror = tmpmsg.substring(2, tmpmsg.length());
                                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCodes:\n\r" + strerror);
                                } else
                                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);

                            } else {
                                try {

                                    String send = commandslist.get(whichCommand);
                                    MainActivity.this.sendMessage(send);

                                    if (whichCommand != 0) {

                                        lastsend = commandslist.get(whichCommand - 1);
                                        Info.setText(lastsend + " : " + tmpmsg);

                                        if (tmpmsg.contains("NODATA") || tmpmsg.contains("?")) {
                                            commandslist.remove(whichCommand - 1);
                                            Info.setText("Removing pid: " + lastsend);
                                        }

                                    } else {
                                        lastsend = commandslist.get(commandslist.size() - 1);
                                        Info.setText(lastsend + " : " + tmpmsg);

                                        if (tmpmsg.contains("NODATA") || tmpmsg.contains("?")) {
                                            commandslist.remove(commandslist.size() - 1);
                                            Info.setText("Removing pid: " + lastsend);
                                        }
                                    }

                                } catch (Exception e) {
                                }
                            }

                            compileMessage(tmpmsg);

                        }

                    } catch (Exception e) {
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void setPidsSupported(String buffer) {

        byte[] pidSupported = null;

        StringBuilder flags = new StringBuilder();
        String buf = buffer.toString();
        buf = buf.trim();
        buf = buf.replace("\t", "");
        buf = buf.replace(" ", "");
        buf = buf.replace(">", "");
        pidSupported = buf.getBytes();

        if (buf.indexOf("4100") == 0 || buf.indexOf("4120") == 0) {

            for (int i = 0; i < 8; i++) {
                String tmp = buf.substring(i + 4, i + 5);
                int data = Integer.valueOf(tmp, 16).intValue();
//                String retStr = Integer.toBinaryString(data);
                if ((data & 0x08) == 0x08) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x04) == 0x04) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x02) == 0x02) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x01) == 0x01) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
            }

            commandslist.clear();
            commandslist.add(0, VOLTAGE);
            int pid = 1;

            StringBuilder supportedPID = new StringBuilder();
            supportedPID.append("Supported PIDS:\n");
            for (int j = 0; j < flags.length(); j++) {
                if (flags.charAt(j) == '1') {
                    supportedPID.append(" " + PIDS[j] + " ");
                    if (!PIDS[j].contains("11") && !PIDS[j].contains("01") && !PIDS[j].contains("20")) {
                        commandslist.add(pid, "01" + PIDS[j]);
                        pid++;
                    }
                }
            }
            m_getPids = true;
            MainActivity.this.sendMessage("ATRV");
            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + supportedPID.toString());

        } else {

            return;
        }
    }

    private double calculateAverage(List<Double> listavg) {
        Double sum = 0.0;
        for (Double val : listavg) {
            sum += val;
        }
        return sum.doubleValue() / listavg.size();
    }

    private String hexToBin(String hex) {
        String bin = "";
        String binFragment = "";
        int iHex;
        hex = hex.trim();
        hex = hex.replaceFirst("0x", "");

        for (int i = 0; i < hex.length(); i++) {
            iHex = Integer.parseInt("" + hex.charAt(i), 16);
            binFragment = Integer.toBinaryString(iHex);

            while (binFragment.length() < 4) {
                binFragment = "0" + binFragment;
            }
            bin += binFragment;
        }
        return bin;
    }

    private void compileMessage(String msg) {

        int index = msg.indexOf("41");

        tmpmsg = msg.substring(index, msg.length());

        if (tmpmsg.substring(0, 2).equals("41")) {

            int A = 0;
            int B = 0;

            try {

                A = Integer.parseInt(tmpmsg.substring(4, 6), 16);
                B = Integer.parseInt(tmpmsg.substring(6, 8), 16);

            } catch (NumberFormatException nFE) {
            }

            if (tmpmsg.contains("410C")) {
                //((A*256)+B)/4
                double val =((A*256)+B)/4;
                int intval = (int) val;
                rpmval = intval;
                rpm.setTargetValue(intval / 100);

            } else if (tmpmsg.contains("410D")) {
                // A
                speed.setTargetValue(A);
            } else if (tmpmsg.contains("4105")) {
                // A-40
                int tempC = A - 40;
                currenttemp = tempC;
                Temp.setText(Integer.toString(tempC) + " C°");
                mConversationArrayAdapter.add("Enginetemp: " + Integer.toString(tempC) + " C°");
            } else if (tmpmsg.contains("410F")) {
                // A - 40
                int tempC = A - 40;
                intakeairtemp = tempC;
                Airtemp.setText(Integer.toString(intakeairtemp) + " C°");
                mConversationArrayAdapter.add("Intakeairtemp: " + Integer.toString(intakeairtemp) + " C°");
            } else if (tmpmsg.contains("4146")) {
                // A-40 [DegC]
                int tempC = A - 40;
                ambientairtemp = tempC;
                mConversationArrayAdapter.add("Ambientairtemp: " + Integer.toString(ambientairtemp) + " C°");
            } else if (tmpmsg.contains("415C")) {
                //A-40
                int tempC = A - 40;
                engineoiltemp = tempC;
                mConversationArrayAdapter.add("Engineoiltemp: " + Integer.toString(engineoiltemp) + " C°");
            } else if (tmpmsg.contains("410B")) {
                // A
                mConversationArrayAdapter.add("Intake Man Pressure: " + Integer.toString(A) + " kPa");
            } else if (tmpmsg.contains("4110")) {
                // ((256*A)+B) / 100  [g/s]
                double val = ((256*A)+B) / 100;
                int intval = (int) val;
                Maf.setText(Integer.toString(intval) + " g/s");
                mConversationArrayAdapter.add("Maf Air Flow: " + Integer.toString(intval) + " g/s");
            } else if (tmpmsg.contains("4123")) {
                // ((A*256)+B)*0.079
                double val = ((A*256)+B)*0.079;
                int intval = (int) val;
                mConversationArrayAdapter.add("Fuel Rail Pressure: " + Integer.toString(intval) + " kPa");
            } else if (tmpmsg.contains("4101")) {
                //Status since DTC Cleared
            } else if (tmpmsg.contains("4111")) {
                //Throttle position 0 -100 % A*100/255
                double val = A*100/255;
                int intval = (int) val;
                mConversationArrayAdapter.add("Fuel Rail Pressure: " + Integer.toString(intval) + " %");
            } else if (tmpmsg.contains("4104")) {
                // A*100/255
                double val = A*100/255;
                int calcLoad = (int) val;

                Load.setText(Integer.toString(calcLoad) + " %");
                mConversationArrayAdapter.add("Engine Load: " + Integer.toString(calcLoad) + " %");

                String consumption = null;

                if (Enginetype == 0) {
                    consumption = String.format("%10.1f", (0.001 * 0.004 * 4.5 * Enginedisplacement * rpmval * 60 * calcLoad / 20)).trim();
                    avgconsumption.add((0.001 * 0.004 * 4 * Enginedisplacement * rpmval * 60 * calcLoad / 20));

                } else if (Enginetype == 1) {
                    consumption = String.format("%10.1f", (0.001 * 0.004 * 4.5 * 1.35 * Enginedisplacement * rpmval * 60 * calcLoad / 20)).trim();
                    avgconsumption.add((0.001 * 0.004 * 4 * 1.35 * Enginedisplacement * rpmval * 60 * calcLoad / 20));

                }
                Fuel.setText(consumption + " - " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " L/h");
                mConversationArrayAdapter.add("Fuel Consumption: " + consumption + " - " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " L/h");
            }
        }
    }
}