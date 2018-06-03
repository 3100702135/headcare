package com.example.administrator.headcare;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.headcare.toolBars.roundPower;
import com.example.administrator.headcare.util.BluetoothManager;
import com.example.administrator.headcare.util.BluetoothReceiver;
import com.example.administrator.headcare.util.TimerTextView;
import com.example.administrator.headcare.util.VerticalSeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class testActivity extends Activity {

    private roundPower mSinkingView;

    private float percent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.power);
        mSinkingView = (roundPower) findViewById(R.id.sinking);
        Button btn_test = findViewById(R.id.btn_test);
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
            }
        });
        percent = 0.56f;
        mSinkingView.setPercent(percent);
    }

    private void test() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                percent = 0;
                while (percent <= 1) {
                    mSinkingView.setPercent(percent);
                    percent += 0.01f;
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                percent = 0.56f;
                mSinkingView.setPercent(percent);
                // mSinkingView.clear();
            }
        });
        thread.start();
        percent = 0.56f;
        mSinkingView.setPercent(percent);
    }
}
