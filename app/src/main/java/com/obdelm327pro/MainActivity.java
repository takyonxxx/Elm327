package com.obdelm327pro;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_STATE_CHANGE = 1;

    /*0	Automatic protocol detection
   1	SAE J1850 PWM (41.6 kbaud)
   2	SAE J1850 VPW (10.4 kbaud)
   3	ISO 9141-2 (5 baud init, 10.4 kbaud)
   4	ISO 14230-4 KWP (5 baud init, 10.4 kbaud)
   5	ISO 14230-4 KWP (fast init, 10.4 kbaud)
   6	ISO 15765-4 CAN (11 bit ID, 500 kbaud)
   7	ISO 15765-4 CAN (29 bit ID, 500 kbaud)
   8	ISO 15765-4 CAN (11 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo
   9	ISO 15765-4 CAN (29 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo


    01 04 - ENGINE_LOAD
    01 05 - ENGINE_COOLANT_TEMPERATURE
    01 0C - ENGINE_RPM
    01 0D - VEHICLE_SPEED
    01 0F - INTAKE_AIR_TEMPERATURE
    01 10 - MASS_AIR_FLOW
    01 11 - THROTTLE_POSITION_PERCENTAGE
    01 1F - ENGINE_RUN_TIME
    01 2F - FUEL_LEVEL
    01 46 - AMBIENT_AIR_TEMPERATURE
    01 51 - FUEL_TYPE
    01 5E - FUEL_CONSUMPTION_1
    01 5F - FUEL_CONSUMPTION_2

   */

    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    protected final static char[] dtcLetters = {'P', 'C', 'B', 'U'};
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static final String[] PIDS = {
            "01", "02", "03", "04", "05", "06", "07", "08",
            "09", "0A", "0B", "0C", "0D", "0E", "0F", "10",
            "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "1A", "1B", "1C", "1D", "1E", "1F", "20"};

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final float APPBAR_ELEVATION = 14f;
    private static boolean actionbar = true;
    final List<String> commandslist = new ArrayList<String>();
    ;
    final List<Double> avgconsumption = new ArrayList<Double>();
    final List<String> troubleCodesArray = new ArrayList<String>();
    MenuItem itemtemp;
    GaugeSpeed speed;
    GaugeRpm rpm;
    BluetoothDevice currentdevice;
    boolean commandmode = false, initialized = false, m_getPids = false, tryconnect = false, defaultStart = false;
    String devicename = null, deviceprotocol = null;

    String[] initializeCommands;
    Intent serverIntent = null;
    TroubleCodes troubleCodes;
    String VOLTAGE = "ATRV",
            PROTOCOL = "ATDP",
            RESET = "ATZ",
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
    Toolbar toolbar;
    AppBarLayout appbar;
    String trysend = null;
    private PowerManager.WakeLock wl;
    private Menu menu;
    private EditText mOutEditText;
    private Button mSendButton, mPidsButton, mTroublecodes, mClearTroublecodes, mClearlist;
    private ListView mConversationView;
    private TextView engineLoad, Fuel, voltage, coolantTemperature, Status, Loadtext, Volttext, Temptext, Centertext, Info, Airtemp_text, airTemperature, Maf_text, Maf;
    private String mConnectedDeviceName = "Ecu";
    private int rpmval = 0, intakeairtemp = 0, ambientairtemp = 0, coolantTemp = 0,
            engineoiltemp = 0, b1s1temp = 0, Enginetype = 0, FaceColor = 0,
            whichCommand = 0, m_dedectPids = 0, connectcount = 0, trycount = 0;
    private double Enginedisplacement = 1500;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBtService = null;
    private ObdWifiManager mWifiService = null;

    StringBuilder inStream = new StringBuilder();

    // The Handler that gets information back from the BluetoothChatService
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;

    private final Handler mWifiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case ObdWifiManager.STATE_CONNECTED:
                            Status.setText(getString(R.string.title_connected_to, "ELM327 WIFI"));
                            try {
                                itemtemp = menu.findItem(R.id.menu_connect_wifi);
                                itemtemp.setTitle(R.string.disconnectwifi);
                            } catch (Exception e) {
                            }
                            tryconnect = false;
                            resetvalues();
                            sendEcuMessage(RESET);

                            break;
                        case ObdWifiManager.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                            Info.setText(R.string.tryconnectwifi);
                            break;
                        case ObdWifiManager.STATE_NONE:
                            Status.setText(R.string.title_not_connected);
                            itemtemp = menu.findItem(R.id.menu_connect_wifi);
                            itemtemp.setTitle(R.string.connectwifi);
                            if (mWifiService != null)mWifiService.disconnect();
                            mWifiService = null;

                            resetvalues();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;

                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);

                    Info.setText(tmpmsg);

                    if (tmpmsg.contains(RSP_ID.NODATA.response) || tmpmsg.contains(RSP_ID.ERROR.response)) {

                        try{
                            String command = tmpmsg.substring(0,4);

                            if(isHexadecimal(command))
                            {
                                removePID(command);
                            }

                        }catch(Exception e)
                        {
                            Toast.makeText(getApplicationContext(), e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }

                    analysMsg(msg);
                    break;

                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:

                            Status.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
                            Info.setText(R.string.title_connected);
                            try {
                                itemtemp = menu.findItem(R.id.menu_connect_bt);
                                itemtemp.setTitle(R.string.disconnectbt);
                                Info.setText(R.string.title_connected);
                            } catch (Exception e) {
                            }

                            tryconnect = false;
                            resetvalues();
                            sendEcuMessage(RESET);

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                            Info.setText(R.string.tryconnectbt);
                            break;
                        case BluetoothService.STATE_LISTEN:

                        case BluetoothService.STATE_NONE:

                            Status.setText(R.string.title_not_connected);
                            itemtemp = menu.findItem(R.id.menu_connect_bt);
                            itemtemp.setTitle(R.string.connectbt);
                            if (tryconnect) {
                                mBtService.connect(currentdevice);
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

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;
                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);

                    Info.setText(tmpmsg);

                    if (tmpmsg.contains(RSP_ID.NODATA.response) || tmpmsg.contains(RSP_ID.ERROR.response)) {

                        try{
                            String command = tmpmsg.substring(0,4);

                            if(isHexadecimal(command))
                            {
                                removePID(command);
                            }

                        }catch(Exception e)
                        {
                            Toast.makeText(getApplicationContext(), e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        }
                    }

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }

                    analysMsg(msg);

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

    private void removePID(String pid)
    {
        int index = commandslist.indexOf(pid);

        if (index != -1)
        {
            commandslist.remove(index);
            Info.setText("Removed pid: " + pid);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendEcuMessage(message);
                    }
                    return true;
                }
            };

    public static boolean isHexadecimal(String text) {
        text = text.trim();

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};

        int hexDigitsCount = 0;

        for (char symbol : text.toCharArray()) {
            for (char hexDigit : hexDigits) {
                if (symbol == hexDigit) {
                    hexDigitsCount++;
                    break;
                }
            }
        }

        return true ? hexDigitsCount == text.length() : false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gauges);

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
        engineLoad = (TextView) findViewById(R.id.Load);
        Fuel = (TextView) findViewById(R.id.Fuel);
        coolantTemperature = (TextView) findViewById(R.id.Temp);
        voltage = (TextView) findViewById(R.id.Volt);
        Loadtext = (TextView) findViewById(R.id.Load_text);
        Temptext = (TextView) findViewById(R.id.Temp_text);
        Volttext = (TextView) findViewById(R.id.Volt_text);
        Centertext = (TextView) findViewById(R.id.Center_text);
        Info = (TextView) findViewById(R.id.info);
        Airtemp_text = (TextView) findViewById(R.id.Airtemp_text);
        airTemperature = (TextView) findViewById(R.id.Airtemp);
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


        troubleCodes = new TroubleCodes();

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
        //ATRD Read the stored data
        //ATSTFF Set time out to maximum
        //ATSTHH Set timeout to 4ms

        initializeCommands = new String[]{"ATZ", "ATL0", "ATE1", "ATH1", "ATAT1", "ATSTFF", "ATI", "ATDP", "ATSP0", "ATSP0"};

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        else
        {
            if (mBtService != null) {
                if (mBtService.getState() == BluetoothService.STATE_NONE) {
                    mBtService.start();
                }
            }
        }

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);

                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(R.id.listText);

                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.parseColor("#3ADF00"));
                tv.setTextSize(10);

                // Generate ListView Item using TextView
                return view;
            }
        };

        mConversationView.setAdapter(mConversationArrayAdapter);

        mPidsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String sPIDs = "0100";
                m_getPids = false;
                sendEcuMessage(sPIDs);
            }
        });
        // Initialize the send button with a listener that for click events

        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                String message = mOutEditText.getText().toString();
                sendEcuMessage(message);
            }
        });

        mClearTroublecodes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String clearCodes = "04";
                sendEcuMessage(clearCodes);
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
                sendEcuMessage(troubleCodes);
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
                        lp.setMargins(0, 0, 0, 0);
                    }

                    setgaugesize();
                    Status.setLayoutParams(lp);

                } catch (Exception e) {
                }
            }
        });

        getPreferences();

        resetgauges();
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

            case R.id.menu_connect_bt:

                if( mWifiService != null)
                {
                    if (mWifiService.isConnected())
                    {
                        Toast.makeText(getApplicationContext(), "First Disconnect WIFI Device.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                    return false;
                }

                if (mBtService == null) setupChat();

                if (item.getTitle().equals("ConnectBT")) {
                    // Launch the DeviceListActivity to see devices and do scan
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    if (mBtService != null)
                    {
                        mBtService.stop();
                        item.setTitle(R.string.connectbt);
                    }
                }

                return true;
            case R.id.menu_connect_wifi:

                if (item.getTitle().equals("ConnectWIFI")) {

                    if (mWifiService == null)
                    {
                        mWifiService = new ObdWifiManager(this, mWifiHandler);
                    }

                    if (mWifiService != null) {
                        if (mWifiService.getState() == ObdWifiManager.STATE_NONE) {
                            mWifiService.connect();
                        }
                    }
                } else {
                    if (mWifiService != null)
                    {
                        mWifiService.disconnect();
                        item.setTitle(R.string.connectwifi);
                    }
                }

                return true;
            case R.id.menu_terminal:

                if (item.getTitle().equals("Terminal")) {
                    commandmode = true;
                    visiblecmd();
                    item.setTitle(R.string.gauges);
                } else {
                    invisiblecmd();
                    item.setTitle(R.string.terminal);
                    commandmode = false;
                    sendEcuMessage(VOLTAGE);
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

                if (mBtService == null) setupChat();

                if (resultCode == MainActivity.RESULT_OK) {
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    Toast.makeText(getApplicationContext(), "BT device not enabled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setDefaultOrientation();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        getPreferences();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBtService != null) mBtService.stop();
        if (mWifiService != null)mWifiService.disconnect();

        wl.release();
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferences();
        setDefaultOrientation();
        resetvalues();
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
                MenuItem item = menu.findItem(R.id.menu_terminal);
                item.setTitle(R.string.terminal);
                sendEcuMessage(VOLTAGE);
            }

            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (mBtService != null) mBtService.stop();
        wl.release();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void getPreferences() {

            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());

            FaceColor = Integer.parseInt(preferences.getString("FaceColor", "0"));

            rpm.setFace(FaceColor);
            speed.setFace(FaceColor);

            Enginedisplacement = Integer.parseInt(preferences.getString("Enginedisplacement", "1400"));
            Enginetype = Integer.parseInt(preferences.getString("EngineType", "0"));

            Enginedisplacement = Enginedisplacement / 1000;

            //Toast.makeText(this,String.valueOf(Enginedisplacement),Toast.LENGTH_SHORT).show();

            m_dedectPids = Integer.parseInt(preferences.getString("DedectPids", "0"));

            if (m_dedectPids == 0) {

                commandslist.clear();

                int i = 0;

                commandslist.add(i, VOLTAGE);

                if (preferences.getBoolean("checkboxENGINE_RPM", true)) {
                    commandslist.add(i, ENGINE_RPM);
                    i++;
                }

                if (preferences.getBoolean("checkboxVEHICLE_SPEED", true)) {
                    commandslist.add(i, VEHICLE_SPEED);
                    i++;
                }

                if (preferences.getBoolean("checkboxENGINE_LOAD", true)) {
                    commandslist.add(i, ENGINE_LOAD);
                    i++;
                }

                if (preferences.getBoolean("checkboxENGINE_COOLANT_TEMP", true)) {
                    commandslist.add(i, ENGINE_COOLANT_TEMP);
                    i++;
                }

                if (preferences.getBoolean("checkboxINTAKE_AIR_TEMP", true)) {
                    commandslist.add(i, INTAKE_AIR_TEMP);
                    i++;
                }

                if (preferences.getBoolean("checkboxMAF_AIR_FLOW", true)) {
                    commandslist.add(i, MAF_AIR_FLOW);
                }

                whichCommand = 0;
            }
    }

    private void setDefaultOrientation() {

        try {

            settextsixe();
            setgaugesize();

        } catch (Exception e) {
        }
    }

    private void settextsixe() {
        int txtsize = 14;
        int sttxtsize = 12;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Status.setTextSize(sttxtsize);
        Fuel.setTextSize(txtsize + 2);
        coolantTemperature.setTextSize(txtsize);
        engineLoad.setTextSize(txtsize);
        voltage.setTextSize(txtsize);
        Temptext.setTextSize(txtsize);
        Loadtext.setTextSize(txtsize);
        Volttext.setTextSize(txtsize);
        Airtemp_text.setTextSize(txtsize);
        airTemperature.setTextSize(txtsize);
        Maf_text.setTextSize(txtsize);
        Maf.setTextSize(txtsize);
        Info.setTextSize(sttxtsize);
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
        engineLoad.setVisibility(View.VISIBLE);
        Fuel.setVisibility(View.VISIBLE);
        voltage.setVisibility(View.VISIBLE);
        coolantTemperature.setVisibility(View.VISIBLE);
        Loadtext.setVisibility(View.VISIBLE);
        Volttext.setVisibility(View.VISIBLE);
        Temptext.setVisibility(View.VISIBLE);
        Centertext.setVisibility(View.VISIBLE);
        Info.setVisibility(View.VISIBLE);
        Airtemp_text.setVisibility(View.VISIBLE);
        airTemperature.setVisibility(View.VISIBLE);
        Maf_text.setVisibility(View.VISIBLE);
        Maf.setVisibility(View.VISIBLE);
    }

    public void visiblecmd() {
        rpm.setVisibility(View.INVISIBLE);
        speed.setVisibility(View.INVISIBLE);
        engineLoad.setVisibility(View.INVISIBLE);
        Fuel.setVisibility(View.INVISIBLE);
        voltage.setVisibility(View.INVISIBLE);
        coolantTemperature.setVisibility(View.INVISIBLE);
        Loadtext.setVisibility(View.INVISIBLE);
        Volttext.setVisibility(View.INVISIBLE);
        Temptext.setVisibility(View.INVISIBLE);
        Centertext.setVisibility(View.INVISIBLE);
        Info.setVisibility(View.INVISIBLE);
        Airtemp_text.setVisibility(View.INVISIBLE);
        airTemperature.setVisibility(View.INVISIBLE);
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

        if (width > height) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(height, height);

            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Load).getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.setMargins(0, 0, 50, 0);
            rpm.setLayoutParams(lp);
            rpm.getLayoutParams().height = height;
            rpm.getLayoutParams().width = (int) (width - 100) / 2;

            lp = new RelativeLayout.LayoutParams(height, height);
            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Load).getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.setMargins(50, 0, 0, 0);
            speed.setLayoutParams(lp);
            speed.getLayoutParams().height = height;
            speed.getLayoutParams().width = (int) (width - 100) / 2;

        } else if (width < height) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, width);

            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.Fuel).getId());
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.setMargins(25, 5, 25, 5);
            speed.setLayoutParams(lp);
            speed.getLayoutParams().width = (int) (width - 50);

            lp = new RelativeLayout.LayoutParams(width, width);
            lp.addRule(RelativeLayout.BELOW, findViewById(R.id.GaugeSpeed).getId());
            //lp.addRule(RelativeLayout.ABOVE,findViewById(R.id.info).getId());
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.setMargins(25, 5, 25, 5);
            rpm.setLayoutParams(lp);
            rpm.getLayoutParams().width = (int) (width - 50);
        }
    }

    public void resetgauges() {

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
    }

    public void resetvalues() {

        engineLoad.setText("0 %");
        voltage.setText("0 V");
        coolantTemperature.setText("0 C°");
        Info.setText("");
        airTemperature.setText("0 C°");
        Maf.setText("0 g/s");
        Fuel.setText("0 - 0 l/h");

        m_getPids = false;
        whichCommand = 0;
        trycount = 0;
        initialized = false;
        defaultStart = false;
        avgconsumption.clear();
        mConversationArrayAdapter.clear();

        resetgauges();

        sendEcuMessage(RESET);
    }

    private void connectDevice(Intent data) {
        tryconnect = true;
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            // Attempt to connect to the device
            mBtService.connect(device);
            currentdevice = device;

        } catch (Exception e) {
        }
    }

    private void setupChat() {

        // Initialize the BluetoothChatService to perform bluetooth connections
        mBtService = new BluetoothService(this, mBtHandler);

    }

    private void sendEcuMessage(String message) {

        if( mWifiService != null)
        {
            if(mWifiService.isConnected())
            {
                try {
                    if (message.length() > 0) {
                        message = message + "\r";
                        byte[] send = message.getBytes();
                        mWifiService.write(send);
                    }
                } catch (Exception e) {
                }
            }
        }
        else if (mBtService != null)
        {
            // Check that we're actually connected before trying anything
            if (mBtService.getState() != BluetoothService.STATE_CONNECTED) {
                //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                if (message.length() > 0) {

                    message = message + "\r";
                    // Get the message bytes and tell the BluetoothChatService to write
                    byte[] send = message.getBytes();
                    mBtService.write(send);
                }
            } catch (Exception e) {
            }
        }
    }

    private void sendInitCommands() {
        if (initializeCommands.length != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = initializeCommands[whichCommand];
            sendEcuMessage(send);

            if (whichCommand == initializeCommands.length - 1) {
                initialized = true;
                whichCommand = 0;
                sendDefaultCommands();
            } else {
                whichCommand++;
            }
        }
    }

    private void sendDefaultCommands() {

        if (commandslist.size() != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = commandslist.get(whichCommand);
            sendEcuMessage(send);

            if (whichCommand >= commandslist.size() - 1) {
                whichCommand = 0;
            } else {
                whichCommand++;
            }
        }
    }

    private String clearMsg(Message msg) {
        String tmpmsg = msg.obj.toString();

        tmpmsg = tmpmsg.replace("null", "");
        tmpmsg = tmpmsg.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
        tmpmsg = tmpmsg.replaceAll(">", "");
        tmpmsg = tmpmsg.replaceAll("SEARCHING...", "");
        tmpmsg = tmpmsg.replaceAll("ATZ", "");
        tmpmsg = tmpmsg.replaceAll("ATI", "");
        tmpmsg = tmpmsg.replaceAll("atz", "");
        tmpmsg = tmpmsg.replaceAll("ati", "");
        tmpmsg = tmpmsg.replaceAll("ATDP", "");
        tmpmsg = tmpmsg.replaceAll("atdp", "");
        tmpmsg = tmpmsg.replaceAll("ATRV", "");
        tmpmsg = tmpmsg.replaceAll("atrv", "");

        return tmpmsg;
    }

    private void checkPids(String tmpmsg) {
        if (tmpmsg.indexOf("41") != -1) {
            int index = tmpmsg.indexOf("41");

            String pidmsg = tmpmsg.substring(index, tmpmsg.length());

            if (pidmsg.contains("4100")) {

                setPidsSupported(pidmsg);
                return;
            }
        }
    }

    private void analysMsg(Message msg) {

        String tmpmsg = clearMsg(msg);

        generateVolt(tmpmsg);

        getElmInfo(tmpmsg);

        if (!initialized) {

            sendInitCommands();

        } else {

            checkPids(tmpmsg);

            if (!m_getPids && m_dedectPids == 1) {
                String sPIDs = "0100";
                sendEcuMessage(sPIDs);
                return;
            }

            if (commandmode) {
                getFaultInfo(tmpmsg);
                return;
            }

            try {
                analysPIDS(tmpmsg);
            } catch (Exception e) {
                Info.setText("Error : " + e.getMessage());
            }

            sendDefaultCommands();
        }
    }

    private void getFaultInfo(String tmpmsg) {

        try{
            int index = tmpmsg.indexOf("43");

            if (index != -1) {

                tmpmsg = tmpmsg.substring(index, tmpmsg.length());

                if (tmpmsg.substring(0, 2).equals("43")) {

                    performCalculations(tmpmsg);

                    String faultCode = null;
                    String faultDesc = null;

                    if (troubleCodesArray.size() > 0) {
                        for (int i = 0; i < troubleCodesArray.size(); i++) {
                            faultCode = troubleCodesArray.get(i);
                            faultDesc = troubleCodes.getFaultCode(faultCode);

                            if (faultCode != null && faultDesc != null) {
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode + "\n" + faultDesc);
                            } else if (faultCode != null && faultDesc == null) {
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode +
                                        "\n" + "No description found for code: " + faultCode);
                            }
                        }
                    } else {
                        faultCode = "No error found...";
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode);
                    }
                }
            }
        }catch(Exception e)
        {}
    }

    protected void performCalculations(String fault) {

        final String result = fault;
        String workingData;
        int startIndex = 0;//Header size.

        troubleCodesArray.clear();

        String canOneFrame = result.replaceAll("[\r\n]", "");
        int canOneFrameLength = canOneFrame.length();
        if (canOneFrameLength <= 16 && canOneFrameLength % 4 == 0) {//CAN(ISO-15765) protocol one frame.
            workingData = canOneFrame;//43yy{codes}
            startIndex = 4;//Header is 43yy, yy showing the number of data items.
        } else if (result.contains(":")) {//CAN(ISO-15765) protocol two and more frames.
            workingData = result.replaceAll("[\r\n].:", "");//xxx43yy{codes}
            startIndex = 7;//Header is xxx43yy, xxx is bytes of information to follow, yy showing the number of data items.
        } else {//ISO9141-2, KWP2000 Fast and KWP2000 5Kbps (ISO15031) protocols.
            workingData = result.replaceAll("^43|[\r\n]43|[\r\n]", "");
        }
        for (int begin = startIndex; begin < workingData.length(); begin += 4) {
            String dtc = "";
            byte b1 = hexStringToByteArray(workingData.charAt(begin));
            int ch1 = ((b1 & 0xC0) >> 6);
            int ch2 = ((b1 & 0x30) >> 4);
            dtc += dtcLetters[ch1];
            dtc += hexArray[ch2];
            dtc += workingData.substring(begin + 1, begin + 4);

            if (dtc.equals("P0000")) {
                continue;
            }

            troubleCodesArray.add(dtc);
        }
    }

    private byte hexStringToByteArray(char s) {
        return (byte) ((Character.digit(s, 16) << 4));
    }

    private void getElmInfo(String tmpmsg) {

        if (tmpmsg.contains("ELM") || tmpmsg.contains("elm")) {
            devicename = tmpmsg;
        }

        if (tmpmsg.contains("SAE") || tmpmsg.contains("ISO")
                || tmpmsg.contains("sae") || tmpmsg.contains("iso") || tmpmsg.contains("AUTO")) {
            deviceprotocol = tmpmsg;
        }

        if (deviceprotocol != null && devicename != null) {
            devicename = devicename.replaceAll("STOPPED", "");
            deviceprotocol = deviceprotocol.replaceAll("STOPPED", "");
            Status.setText(devicename + " " + deviceprotocol);
        }
    }


    private void setPidsSupported(String buffer) {

        Info.setText("Trying to get available pids : " + String.valueOf(trycount));
        trycount++;

        StringBuilder flags = new StringBuilder();
        String buf = buffer.toString();
        buf = buf.trim();
        buf = buf.replace("\t", "");
        buf = buf.replace(" ", "");
        buf = buf.replace(">", "");

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
            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + supportedPID.toString());
            whichCommand = 0;
            sendEcuMessage("ATRV");

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

    private void analysPIDS(String dataRecieved) {

        int A = 0;
        int B = 0;
        int PID = 0;

        if ((dataRecieved != null) && (dataRecieved.matches("^[0-9A-F]+$"))) {

            dataRecieved = dataRecieved.trim();

            int index = dataRecieved.indexOf("41");

            String tmpmsg = null;

            if (index != -1) {

                tmpmsg = dataRecieved.substring(index, dataRecieved.length());

                if (tmpmsg.substring(0, 2).equals("41")) {

                    PID = Integer.parseInt(tmpmsg.substring(2, 4), 16);
                    A = Integer.parseInt(tmpmsg.substring(4, 6), 16);
                    B = Integer.parseInt(tmpmsg.substring(6, 8), 16);

                    calculateEcuValues(PID, A, B);
                }
            }
        }
    }

    private void generateVolt(String msg) {

        String VoltText = null;

        if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})\\s*"))) {

            VoltText = msg + "V";

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg + "V");

        } else if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})?V\\s*"))) {

            VoltText = msg;

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg);
        }

        if (VoltText != null) {
            voltage.setText(VoltText);
        }
    }

    private void calculateEcuValues(int PID, int A, int B) {

        double val = 0;
        int intval = 0;
        int tempC = 0;

        switch (PID) {

            case 4://PID(04): Engine Load

                // A*100/255
                val = A * 100 / 255;
                int calcLoad = (int) val;

                engineLoad.setText(Integer.toString(calcLoad) + " %");
                mConversationArrayAdapter.add("Engine Load: " + Integer.toString(calcLoad) + " %");

                String consumption = null;

                if (Enginetype == 0) {
                    consumption = String.format("%10.1f", (0.001 * 0.004 * 4.5 * Enginedisplacement * rpmval * 60 * calcLoad / 20)).trim();
                    avgconsumption.add((0.001 * 0.004 * 4 * Enginedisplacement * rpmval * 60 * calcLoad / 20));

                } else if (Enginetype == 1) {
                    consumption = String.format("%10.1f", (0.001 * 0.004 * 4.5 * 1.35 * Enginedisplacement * rpmval * 60 * calcLoad / 20)).trim();
                    avgconsumption.add((0.001 * 0.004 * 4 * 1.35 * Enginedisplacement * rpmval * 60 * calcLoad / 20));

                }
                Fuel.setText(consumption + " - " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h");
                mConversationArrayAdapter.add("Fuel Consumption: " + consumption + " - " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h");

                break;

            case 5://PID(05): Coolant Temperature

                // A-40
                tempC = A - 40;
                coolantTemp = tempC;
                coolantTemperature.setText(Integer.toString(coolantTemp) + " C°");
                mConversationArrayAdapter.add("Enginetemp: " + Integer.toString(tempC) + " C°");

                break;

            case 11://PID(0B)

                // A
                mConversationArrayAdapter.add("Intake Man Pressure: " + Integer.toString(A) + " kPa");

                break;

            case 12: //PID(0C): RPM

                //((A*256)+B)/4
                val = ((A * 256) + B) / 4;
                intval = (int) val;
                rpmval = intval;
                rpm.setTargetValue(intval / 100);

                break;


            case 13://PID(0D): KM

                // A
                speed.setTargetValue(A);

                break;

            case 15://PID(0F): Intake Temperature

                // A - 40
                tempC = A - 40;
                intakeairtemp = tempC;
                airTemperature.setText(Integer.toString(intakeairtemp) + " C°");
                mConversationArrayAdapter.add("Intakeairtemp: " + Integer.toString(intakeairtemp) + " C°");

                break;

            case 16://PID(10): Maf

                // ((256*A)+B) / 100  [g/s]
                val = ((256 * A) + B) / 100;
                intval = (int) val;
                Maf.setText(Integer.toString(intval) + " g/s");
                mConversationArrayAdapter.add("Maf Air Flow: " + Integer.toString(intval) + " g/s");

                break;

            case 17://PID(11)

                //A*100/255
                val = A * 100 / 255;
                intval = (int) val;
                mConversationArrayAdapter.add(" Throttle position: " + Integer.toString(intval) + " %");

                break;

            case 35://PID(23)

                // ((A*256)+B)*0.079
                val = ((A * 256) + B) * 0.079;
                intval = (int) val;
                mConversationArrayAdapter.add("Fuel Rail Pressure: " + Integer.toString(intval) + " kPa");

                break;

            case 49://PID(31)

                //(256*A)+B km
                val = (A * 256) + B;
                intval = (int) val;
                mConversationArrayAdapter.add("Distance traveled: " + Integer.toString(intval) + " km");

                break;

            case 70://PID(46)

                // A-40 [DegC]
                tempC = A - 40;
                ambientairtemp = tempC;
                mConversationArrayAdapter.add("Ambientairtemp: " + Integer.toString(ambientairtemp) + " C°");

                break;

            case 92://PID(5C)

                //A-40
                tempC = A - 40;
                engineoiltemp = tempC;
                mConversationArrayAdapter.add("Engineoiltemp: " + Integer.toString(engineoiltemp) + " C°");

                break;

            default:
        }
    }

    enum RSP_ID {
        PROMPT(">"),
        OK("OK"),
        MODEL("ELM"),
        NODATA("NODATA"),
        SEARCH("SEARCHING"),
        ERROR("ERROR"),
        NOCONN("UNABLE"),
        NOCONN_MSG("UNABLE TO CONNECT"),
        NOCONN2("NABLETO"),
        CANERROR("CANERROR"),
        CONNECTED("ECU CONNECTED"),
        BUSBUSY("BUSBUSY"),
        BUSY("BUSY"),
        BUSERROR("BUSERROR"),
        BUSINIERR("BUSINIT:ERR"),
        BUSINIERR2("BUSINIT:BUS"),
        BUSINIERR3("BUSINIT:...ERR"),
        BUS("BUS"),
        FBERROR("FBERROR"),
        DATAERROR("DATAERROR"),
        BUFFERFULL("BUFFERFULL"),
        STOPPED("STOPPED"),
        RXERROR("<"),
        QMARK("?"),
        UNKNOWN("");
        private String response;

        RSP_ID(String response) {
            this.response = response;
        }

        @Override
        public String toString() {
            return response;
        }
    }
}