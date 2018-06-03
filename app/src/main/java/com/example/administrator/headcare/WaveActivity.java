package com.example.administrator.headcare;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.administrator.headcare.toolBars.LD_WaveView;
import com.example.administrator.headcare.toolBars.roundPower;

public class WaveActivity extends Activity {

    LD_WaveView waveView;//方形
    LD_WaveView waveCircleView;//圆形
    private int progrees=0;//进度
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (progrees==100) progrees=0;
            Log.i("progress",progrees+"");
            waveView.setmProgress(progrees++);
            waveCircleView.setmProgress(progrees++);
            mHandler.sendEmptyMessageDelayed(0,100);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave);
        waveView= (LD_WaveView) findViewById(R.id.waveView);
        waveCircleView= (LD_WaveView) findViewById(R.id.waveViewCircle);
        mHandler.sendEmptyMessageDelayed(0,10);
    }

}
