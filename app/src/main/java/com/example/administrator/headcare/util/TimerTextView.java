package com.example.administrator.headcare.util;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class TimerTextView extends AppCompatTextView implements Runnable{
    public TimerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    private long  mmin, msecond;//天，小时，分钟，秒
    private boolean run=false; //是否启动了

    public void setTimes(long[] times) {
        mmin = times[0];
        msecond = times[1];
    }

    /**
     * 倒计时计算
     */
    private void ComputeTime() {
        msecond--;
        if (msecond < 0) {
            mmin--;
            msecond = 59;
            if(mmin<0)
            {
                this.run=false;
            }
        }

    }

    public boolean isRun() {
        return run;
    }

    public void beginRun() {
        this.run = true;
        run();
    }

    public void stopRun(){
        this.run = false;
    }


    @Override
    public void run() {
        //标示已经启动
        if(run){
            ComputeTime();
            String strTime=  "剩余照射时间"+mmin+"分钟:"+msecond+"秒";
            this.setText(strTime);
            postDelayed(this, 1000);
        }else {
            removeCallbacks(this);
        }
    }
}
