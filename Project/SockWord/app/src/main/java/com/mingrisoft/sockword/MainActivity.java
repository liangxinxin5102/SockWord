package com.mingrisoft.sockword;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.assetsbasedata.AssetsDatabaseManager;
import com.iflytek.cloud.speech.SpeechConstant;
import com.iflytek.cloud.speech.SpeechError;
import com.iflytek.cloud.speech.SpeechListener;
import com.iflytek.cloud.speech.SpeechSynthesizer;
import com.iflytek.cloud.speech.SpeechUser;
import com.iflytek.cloud.speech.SynthesizerListener;
import com.mingrisoft.greendao.entity.greendao.CET4Entity;
import com.mingrisoft.greendao.entity.greendao.CET4EntityDao;
import com.mingrisoft.greendao.entity.greendao.DaoMaster;
import com.mingrisoft.greendao.entity.greendao.DaoSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SynthesizerListener, RadioGroup.OnCheckedChangeListener {
    //用来显示单词和音标的
    private TextView timeText, dateText, wordText, englishText;
    private ImageView playVioce;                     //播放声音
    private String mMonth, mDay, mWay, mHours, mMinute; // 用来显示时间
    private SpeechSynthesizer speechSynthesizer;         // 合成对象
    //锁屏
    private KeyguardManager km;
    private KeyguardManager.KeyguardLock kl;
    private RadioGroup radioGroup;                      // 加载单词的三个选项
    private RadioButton radioOne, radioTwo, radioThree; // 单词意思的三个选项
    private SharedPreferences sharedPreferences;         // 定义轻量级数据库
    SharedPreferences.Editor editor = null;              // 编辑数据库
    int j = 0;                                     // 用于记录答了几道题
    List<Integer> list;                             // 判断题的数目
    List<CET4Entity> datas;                             // 用于从数据库独处相应的词库
    int k;
    /**
     * 手指按下的点为（x1,y1）
     * 手指离开屏幕的点为（x2,y2）
     */
    float x1 = 0;
    float y1 = 0;
    float x2 = 0;
    float y2 = 0;

    private SQLiteDatabase db;                        // 创建数据库
    private DaoMaster mDaoMaster, dbMaster;            // 管理者
    private DaoSession mDaoSession, dbSession;        // 和数据库进行会话
    // 对应的表,由java代码生成的,对数据库内相应的表操作使用此对象
    private CET4EntityDao questionDao, dbDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 这句话是把此页面显示到手机屏幕的最上层
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_main);
        init();
    }

    /**
     * 初始化控件
     */
    public void init() {
        //初始化轻量级数据库
        sharedPreferences = getSharedPreferences("share", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();          //初始化轻量级数据库编辑器
        //给播放单词语音的设置个appid（这个是要到讯飞平台申请的，详情请参考官网）
        list = new ArrayList<Integer>();            //初始化list
        /**
         * 添加一个10个10以内随机数
         * */
        Random r = new Random();
        int i;
        while (list.size() < 10) {
            i = r.nextInt(20);
            if (!list.contains(i)) {
                list.add(i);
            }
        }
        /**
         * 得到键盘锁管理对象
         * */
        km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("unLock");
        // 初始化，只需要调用一次
        AssetsDatabaseManager.initManager(this);
        // 获取管理对象，因为数据库需要通过管理对象才能够获取
        AssetsDatabaseManager mg = AssetsDatabaseManager.getManager();
        // 通过管理对象获取数据库
        SQLiteDatabase db1 = mg.getDatabase("word.db");
        // 对数据库进行操作
        mDaoMaster = new DaoMaster(db1);
        mDaoSession = mDaoMaster.newSession();
        questionDao = mDaoSession.getCET4EntityDao();
        /**此DevOpenHelper类继承自SQLiteOpenHelper,
         *第一个参数Context,第二个参数数据库名字,第三个参数CursorFactory
         */
        DaoMaster.DevOpenHelper helper = new DaoMaster.
                DevOpenHelper(this, "wrong.db", null);
        /**
         * 初始化数据库
         * */
        db = helper.getWritableDatabase();
        dbMaster = new DaoMaster(db);
        dbSession = dbMaster.newSession();
        dbDao = dbSession.getCET4EntityDao();
        /**
         * 控件初始化
         * */
        //用于显示分钟绑定id
        timeText = (TextView) findViewById(R.id.time_text);
        //用于显示日期绑定id
        dateText = (TextView) findViewById(R.id.date_text);
        //用于显示单词绑定id
        wordText = (TextView) findViewById(R.id.word_text);
        //显示音标绑定id
        englishText = (TextView) findViewById(R.id.english_text);
        //用于播放单词的按钮绑定id
        playVioce = (ImageView) findViewById(R.id.play_vioce);
        //给播放单词按钮进行监听
        playVioce.setOnClickListener(this);
        //给加载单词三个选项绑定id
        radioGroup = (RadioGroup) findViewById(R.id.choose_group);
        //给第一个选项绑定id
        radioOne = (RadioButton) findViewById(R.id.choose_btn_one);
        //给第二个选项绑定id
        radioTwo = (RadioButton) findViewById(R.id.choose_btn_two);
        //给第三个选项绑定id
        radioThree = (RadioButton) findViewById(R.id.choose_btn_three);
        //给加载单词三个选项设置监听事件
        radioGroup.setOnCheckedChangeListener(this);
        setParam();//初始化播放语音
        //appid换成自己申请的，播放语音
        SpeechUser.getUser().login(MainActivity.this, null, null,
                "appid=573a7bf0", listener);

    }

    protected void onStart() {
        super.onStart();
        /**
         * 获取系统日期  并设置显示出来
         * */
        Calendar calendar = Calendar.getInstance();
        mMonth = String.valueOf(calendar.get(Calendar.MONTH) + 1); //获取日期的月
        mDay = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));//获取日期的天
        mWay = String.valueOf(calendar.get(Calendar.DAY_OF_WEEK)); //获取日期的星期
        /**
         * 如果小时是个位数
         *则在前面加一个“0”
         * */
        if (calendar.get(Calendar.HOUR) < 10) {
            mHours = "0" + calendar.get(Calendar.HOUR);
        } else {
            mHours = String.valueOf(calendar.get(Calendar.HOUR));
        }
        /**
         * 如果分钟是个位数
         *
         *则在前面加一个“0”
         * */
        if (calendar.get(Calendar.MINUTE) < 10) {
            mMinute = "0" + calendar.get(Calendar.MINUTE);
        } else {
            mMinute = String.valueOf(calendar.get(Calendar.MINUTE));
        }
        /**
         * 获取星期
         * 并设置出来
         * */
        if ("1".equals(mWay)) {
            mWay = "天";
        } else if ("2".equals(mWay)) {
            mWay = "一";
        } else if ("3".equals(mWay)) {
            mWay = "二";
        } else if ("4".equals(mWay)) {
            mWay = "三";
        } else if ("5".equals(mWay)) {
            mWay = "四";
        } else if ("6".equals(mWay)) {
            mWay = "五";
        } else if ("7".equals(mWay)) {
            mWay = "六";
        }
        timeText.setText(mHours + ":" + mMinute);
        dateText.setText(mMonth + "月" + mDay + "日" + "    " + "星期" + mWay);
        getDBData();			 //获取数据文件方法
        //把mainActivity添加到销毁集合里
        BaseApplication.addDestroyActiivty(this, "mainActivity");

    }

    /**
     * 将错题存到数据库
     */
    private void saveWrongData() {
        String word = datas.get(k).getWord();         //获取答错这道题的单词
        String english = datas.get(k).getEnglish();  //获取答错这道题的音标
        String china = datas.get(k).getChina();       //获取答错这道题的汉语意思
        String sign = datas.get(k).getSign();         //获取答错这道题的标记
        CET4Entity data = new CET4Entity(Long.valueOf(dbDao.count()),
                word, english, china, sign);
        dbDao.insertOrReplace(data);                   //把这些字段存到数据库
    }

    /**
     * 设置选项的不同颜色
     */
    private void btnGetText(String msg, RadioButton btn) {
        /**
         * 答题答对了 设置绿色 答错设置红色
         * */
        if (msg.equals(datas.get(k).getChina())) {
            wordText.setTextColor(Color.GREEN);        //设置单词为绿色
            englishText.setTextColor(Color.GREEN);    //设置音标为绿色
            btn.setTextColor(Color.GREEN);             //设置选项为绿色
        } else {
            wordText.setTextColor(Color.RED);          //设置单词为红色
            englishText.setTextColor(Color.RED);       //设置音标为红色
            btn.setTextColor(Color.RED);                //设置选项为红色
            saveWrongData();                             //执行存入错题的方法
            //保存到数据库
            int wrong = sharedPreferences.getInt("wrong", 0);//从数据库里面取出数据
            editor.putInt("wrong", wrong + 1);         //写入数据库
            editor.putString("wrongId", "," + datas.get(j).getId());  //写入数据库
            editor.commit();                             //保存
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_vioce:                                //播放单词声音
                String text = wordText.getText().toString();    //把单词提取出来
                speechSynthesizer.startSpeaking(text, this);    //讯飞 播放声音
                break;
        }

    }

    @Override
    public void onSpeakBegin() {

    }

    @Override
    public void onBufferProgress(int i, int i1, int i2, String s) {

    }

    @Override
    public void onSpeakPaused() {

    }

    @Override
    public void onSpeakResumed() {

    }

    @Override
    public void onSpeakProgress(int i, int i1, int i2) {

    }

    @Override
    public void onCompleted(SpeechError speechError) {

    }

    /**
     * 通用回调接口
     */
    private SpeechListener listener = new SpeechListener() {
        //消息回调
        @Override
        public void onEvent(int arg0, Bundle arg1) {
            // TODO Auto-generated method stub
        }

        //数据回调
        @Override
        public void onData(byte[] arg0) {
            // TODO Auto-generated method stub
        }

        //结束回调（没有错误）
        @Override
        public void onCompleted(SpeechError arg0) {
            // TODO Auto-generated method stub
        }
    };

    /**
     * 初始化语音播报
     */
    public void setParam() {
        speechSynthesizer = SpeechSynthesizer.createSynthesizer(this);
        speechSynthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        speechSynthesizer.setParameter(SpeechConstant.SPEED, "50");
        speechSynthesizer.setParameter(SpeechConstant.VOLUME, "50");
        speechSynthesizer.setParameter(SpeechConstant.PITCH, "50");
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        /**
         * 选项的点击事件
         */
        radioGroup.setClickable(false);            //默认选项未被选中
        switch (checkedId) {                        //选项的点击事件
            case R.id.choose_btn_one:               //点击“A”选项
                //截取字符串
                String msg = radioOne.getText().toString().substring(3);
                btnGetText(msg, radioOne);          //将参数传入到对应的方法里
                break;
            case R.id.choose_btn_two:               //点击“B”选项
                //截取字符串
                String msg1 = radioTwo.getText().toString().substring(3);
                btnGetText(msg1, radioTwo);        //将参数传入到对应的方法里
                break;
            case R.id.choose_btn_three:             //点击“C”选项
                //截取字符串
                String msg2 = radioThree.getText().toString().substring(3);
                btnGetText(msg2, radioThree);      //将参数传入到对应的方法里
                break;
        }
    }

    /**
     * 还原单词与选项的颜色
     */
    private void setTextColor() {
        //还原单词选项的颜色
        radioOne.setChecked(false);        //默认不被点击
        radioTwo.setChecked(false);        //默认不被点击
        radioThree.setChecked(false);        //默认不被点击
        /**将选项的按钮设置为白色*/
        radioOne.setTextColor(Color.parseColor("#FFFFFF"));
        radioTwo.setTextColor(Color.parseColor("#FFFFFF"));
        radioThree.setTextColor(Color.parseColor("#FFFFFF"));
        wordText.setTextColor(Color.parseColor("#FFFFFF"));  //将单词设置为白色
        englishText.setTextColor(Color.parseColor("#FFFFFF"));//将音标设置为白色
    }

    /**
     * 解锁
     */
    private void unlocked() {
        Intent intent1 = new Intent(Intent.ACTION_MAIN);   //界面跳转
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.addCategory(Intent.CATEGORY_HOME);         //进入到手机桌面
        startActivity(intent1);                               //启动
        kl.disableKeyguard();                               //解锁
        finish();                                           //销毁当前activity
    }

    /**
     * 设置选项
     */
    private void setChina(List<CET4Entity> datas, int j) {
        /**
         * 随机产生几个随机数， 这里面产生几个随机数  是用于解锁单词
         * 因为此demo输入数据库里面20个单词， 所以产生的随机数是20以内的
         * */
        Random r = new Random();
        List<Integer> listInt = new ArrayList<>();
        int i;
        while (listInt.size() < 4) {
            i = r.nextInt(20);
            if (!listInt.contains(i)) {
                listInt.add(i);
            }
        }
        /**
         * 以下的判断是给这个单词设置三个选项，设置单词选项是有规律的
         *三个选项，分别是正确的、正确的前一个、正确的的后一个
         *将这三个解释设置到单词的选项上，以下为实现逻辑
         * */
        if (listInt.get(0) < 7) {
            radioOne.setText("A: " + datas.get(k).getChina());
            if (k - 1 >= 0) {
                radioTwo.setText("B: " + datas.get(k - 1).getChina());
            } else {
                radioTwo.setText("B: " + datas.get(k + 2).getChina());
            }
            if (k + 1 < 20) {
                radioThree.setText("C: " + datas.get(k + 1).getChina());
            } else {
                radioThree.setText("C: " + datas.get(k - 1).getChina());
            }
        } else if (listInt.get(0) < 14) {
            radioTwo.setText("B: " + datas.get(k).getChina());
            if (k - 1 >= 0) {
                radioOne.setText("A: " + datas.get(k - 1).getChina());
            } else {
                radioOne.setText("A: " + datas.get(k + 2).getChina());
            }
            if (k + 1 < 20) {
                radioThree.setText("C: " + datas.get(k + 1).getChina());
            } else {
                radioThree.setText("C: " + datas.get(k - 1).getChina());
            }
        } else {
            radioThree.setText("C: " + datas.get(k).getChina());
            if (k - 1 >= 0) {
                radioTwo.setText("B: " + datas.get(k - 1).getChina());
            } else {
                radioTwo.setText("B: " + datas.get(k + 2).getChina());
            }
            if (k + 1 < 20) {
                radioOne.setText("A: " + datas.get(k + 1).getChina());
            } else {
                radioOne.setText("A: " + datas.get(k - 1).getChina());
            }
        }
    }

    /**
     * 获取数据库数据
     */
    private void getDBData() {
        datas = questionDao.queryBuilder().list();       //把词库里面的单词读出来
        k = list.get(j);
        wordText.setText(datas.get(k).getWord());        //设置单词
        englishText.setText(datas.get(k).getEnglish()); //设置音标
        setChina(datas, k);                                //设置单词的三个选项
    }

    /**
     * 复写activity的onTouch方法
     * 监听滑动事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //当手指按下的时候（x，y）坐标
            x1 = event.getX();
            y1 = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //当手指离开的时候（x，y）坐标
            x2 = event.getX();
            y2 = event.getY();
            if (y1 - y2 > 200) {                        //向上滑

                // 已掌握单词数量加一
                int num = sharedPreferences.getInt("alreadyMastered", 0) + 1;
                editor.putInt("alreadyMastered", num);  //输入到数据库
                editor.commit();            //保存
                Toast.makeText(this, "已掌握", Toast.LENGTH_SHORT).show(); //弹出提示
                getNextData();                            //获取下一条数据
            } else if (y2 - y1 > 200) {                //向下滑
                Toast.makeText(this, "待加功能......", Toast.LENGTH_SHORT).show();
            } else if (x1 - x2 > 200) {                //向左滑
                getNextData();                //获取下一条数据
            } else if (x2 - x1 > 200) {    //向右滑
                unlocked();                //解锁
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 获取下一题
     */
    private void getNextData() {
        j++;                  //当前已做题的数目
        int i = sharedPreferences.getInt("allNum", 2);   //默认解锁题数目为2道
        if (i > j) {         //判断设定的解锁题数目与当前已做题的数目大小关系
            getDBData();     //获取数据
            setTextColor(); //设置颜色
            //已经学习的单词量加一
            int num = sharedPreferences.getInt("alreadyStudy", 0) + 1;
            editor.putInt("alreadyStudy", num);
            editor.commit(); //存到数据库里面
        } else {
            unlocked();       //解锁
        }
    }

}