/*
T�RKAY B�L�YOR turkaybiliyor@hotmail.com
 */
package com.obdelm327pro;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.obdelm327pro.R;
public class Prefs extends PreferenceActivity {		
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        addPreferencesFromResource(R.xml.preference);
    }	 
}