package com.vjs.ypn.vjs;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.android.gms.location.LocationServices;

public class SplashScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        //View v = findViewById(R.id.iv_logo);
        //startFadeInAnimation();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isServiceRunning(AppService.class)){
                    Intent i =  new Intent(SplashScreenActivity.this,CaseTrackingActivity.class);
                    startActivity(i);
                }else{
                    startActivity(new Intent(SplashScreenActivity.this,InTrackingActivity.class));
                }
                finish();
            }
        },1000);

    }
    public void startFadeInAnimation() {
        ImageView imageView = findViewById(R.id.iv_logo);
        Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_animation);
        imageView.startAnimation(startAnimation);
    }

    private boolean isServiceRunning(Class<?> serviceClass){
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
