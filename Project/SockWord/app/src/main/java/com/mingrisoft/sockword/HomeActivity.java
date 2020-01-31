package com.mingrisoft.sockword;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Administrator on 2016/10/24.
 */
public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    private ScreenListener screenListener;     //绑定此页面与手机屏幕状态的监听
    private SharedPreferences sharedPreferences;    //定义一个轻量级数据库
    private FragmentTransaction transaction;        //定义用于加载复习与设置的界面
    private StudyFragment studyFragment;            //绑定复习界面
    private SetFragment setFragment;                //绑定设置界面
    private Button wrongBtn;                        //定义错词本按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.home_layout);         //绑定布局文件
        init();                                        //初始化控件
    }

    /**
     * 初始化控件的方法
     */
    private void init() {
        sharedPreferences = getSharedPreferences("share",
                Context.MODE_PRIVATE); //初始化数据库
        wrongBtn = (Button) findViewById(R.id.wrong_btn); //绑定id
        wrongBtn.setOnClickListener(this);              //对按钮设置监听事件
        //设置editer用于网数据库里面添加数据和修改数据
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        screenListener = new ScreenListener(this); //屏幕状态进行监听
        screenListener.begin(new ScreenListener.ScreenStateListener() {
            @Override
            public void onScreenOn() {            //手机已点亮屏幕的操作
                //判断是否在设置界面开启了锁屏按钮
                if (sharedPreferences.getBoolean("btnTf", false)) {
                    //判断屏幕是否解锁
                    if (sharedPreferences.getBoolean("tf", false)) {
                        Intent intent = new Intent(HomeActivity.this,
                                MainActivity.class);            //启动锁屏页面
                        startActivity(intent);        //开始跳转
                    }
                }
            }

            @Override
            public void onScreenOff() {                //手机已锁屏的操作
                /**
                 * 如果手机已经锁了
                 * 就把数据库面的tf字段改成true
                 * */
                editor.putBoolean("tf", true);
                editor.commit();
                //销毁锁屏界面
                BaseApplication.destroyActivity("mainActivity");

            }

            @Override
            public void onUserPresent() {            //手机已解锁的操作
                /**
                 * 如果手机已经解锁了
                 * 就把数据库面的tf字段改成false
                 * */
                editor.putBoolean("tf", false);
                editor.commit();
            }
        });
        //当此页面加载的时候  显示复习界面的fragment
        studyFragment = new StudyFragment();
        setFragment(studyFragment);                //设置不同的fragment
    }

    /**
     * 点击不同的按钮显示不同的fragment
     */
    public void setFragment(Fragment fragment) {
        transaction = getFragmentManager().beginTransaction();
//初始化transaction
        transaction.replace(R.id.frame_layout, fragment);
        //绑定id
        transaction.commit();
    }

    //点击进入复习页面
    public void study(View v) {
        if (studyFragment == null) {
            studyFragment = new StudyFragment();
        }
        setFragment(studyFragment);
    }

    //点击进入设置界面
    public void set(View v) {
        if (setFragment == null) {
            setFragment = new SetFragment();
        }
        setFragment(setFragment);
    }

    /**
     * 解除广播监听
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        screenListener.unregisterListener();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /**
             * 跳转到错题页面
             * */
            case R.id.wrong_btn:
                Intent intent = new Intent(this, WrongActivity.class);
                startActivity(intent);
                break;
        }

    }
}
