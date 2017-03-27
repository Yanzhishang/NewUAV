package com.zs.yzs.newuav;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by ZS on 17-3-23.
 */

public class Gravity_sensor extends Activity implements SensorEventListener {
    private TextView tv_qh1, tv_zy1,gas1;
    private SeekBar skBar1;
    private Socket socket1;
    private OutputStream outputStream1;
    public byte[] data1 = new byte[34];  // 定义通信数组data,长度为34.
    public boolean flag1 = true;         // 定义变量flag，用于while循环。
    private long firstTime = 0;
    public int qh_equ = 1470;
    public int zy_equ = 1500;
    public int reverse;
    public ToggleButton tog_btn1;

    private SensorManager sensorManager;
    private Sensor magneticSensor;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;
    // 将纳秒转化为秒
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float angle[] = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置无标题
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //       WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setContentView(new MySurfaceView(this));
        //隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏系统状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.gravity_sensor);
        //获取控件id
        tog_btn1 = (ToggleButton) findViewById(R.id.touch);
        tv_qh1 = (TextView) findViewById(R.id.tv_qh1);
        tv_zy1 = (TextView) findViewById(R.id.tv_zy1);
        gas1 = (TextView) findViewById(R.id.gas1);
        skBar1 = (SeekBar) findViewById(R.id.seekBar1);

        //调用方法
        initdata();
        initViews();


        /**
         * 陀螺仪
         */

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //注册陀螺仪传感器，并设定传感器向应用中输出的时间间隔类型是SensorManager.SENSOR_DELAY_GAME(20000微秒)
        //SensorManager.SENSOR_DELAY_FASTEST(0微秒)：最快。最低延迟，一般不是特别敏感的处理不推荐使用，该模式可能在成手机电力大量消耗，由于传递的为原始数据，诉法不处理好会影响游戏逻辑和UI的性能
        //SensorManager.SENSOR_DELAY_GAME(20000微秒)：游戏。游戏延迟，一般绝大多数的实时性较高的游戏都是用该级别
        //SensorManager.SENSOR_DELAY_NORMAL(200000微秒):普通。标准延时，对于一般的益智类或EASY级别的游戏可以使用，但过低的采样率可能对一些赛车类游戏有跳帧现象
        //SensorManager.SENSOR_DELAY_UI(60000微秒):用户界面。一般对于屏幕方向自动旋转使用，相对节省电能和逻辑处理，一般游戏开发中不使用
        sensorManager.registerListener(this, gyroscopeSensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magneticSensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME);


    }


    /**
     * 本方法作用： 添加初始数据给通信数组
     */
    public void initdata() {
        data1[0] = (byte) 0xAA; // 通信协议固定数据
        data1[1] = (byte) 0xC0; // 通信协议固定数据
        data1[2] = (byte) 0x1C; // 通信协议固定数据
        // ------油门值（控制飞行速度与高度）------
        data1[3] = (byte) 0x00; // 油门值的高八位
        data1[4] = (byte) 0x00; // 油门值的低八位
        // ------航向值（控制旋转角度）------
        data1[5] = (byte) (1500 >> 8); // 航向值的高八位
        data1[6] = (byte) (1500 & 0xff); // 航向值的低八位
        // ------横滚值（控制左右方向）------
        data1[7] = (byte) (1405 >> 8); // 横滚值的高八位
        data1[8] = (byte) (1405 & 0xff); // 横滚值的低八位
        // ------仰俯值（控制前后方向）------
        data1[9] = (byte) (1305 >> 8); // 仰俯值的高八位
        data1[10] = (byte) (1305 & 0xff); // 仰俯值的低八位
        // 剩余其他位置的数据由于默认就是0，并且加油门时不用，所以可以省略掉。
        data1[31] = (byte) 0x1C; // 通信协议固定数据
        data1[32] = (byte) 0x0D; // 通信协议固定数据
        data1[33] = (byte) 0x0A; // 通信协议固定数据
    }


    //点击连接无人机
    public void touch(View view) {
        new Thread(new ConnThread()).start();
        flag1 = true;
        if (tog_btn1.isChecked()) {
        } else {
            data1[3] = (byte) 0x00; // 油门值的高八位
            data1[4] = (byte) 0x00;
            skBar1.setProgress(0);
            gas1.setText("油门：0.0 ");

            data1[5] = (byte) (1500 >> 8); // 横滚值的高八位
            data1[6] = (byte) (1500 & 0xff); // 横滚值的低八位

            //设置横滚值为中间值 1500（控制左右方向）
            data1[7] = (byte) (1500 >> 8); // 横滚值的高八位
            data1[8] = (byte) (1500 & 0xff); // 横滚值的低八位

            // ------仰俯值（控制前后方向）------（前后）
            data1[9] = (byte) (1470 >> 8); // 仰俯值的高八位
            data1[10] = (byte) (1470 & 0xff); // 仰俯值的低八位
            Intent intent = new Intent(Gravity_sensor.this,MainActivity.class);
            startActivity(intent);
            onDestroy();


        }
    }




    //seekbar触摸事件
    private void initViews() {
        skBar1.setMax(800);          //设置活动条最大值
        skBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                double pg = progress % 5;
                if (pg == 0) {
                    data1[3] = (byte) (progress >> 8); // 油门值的高八位
                    data1[4] = (byte) (progress & 0xff);
                    gas1.setText("油门：" + progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                new Thread(new Send_rock()).start();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    /**
     * 连接无人机   线程
     */
    class ConnThread implements Runnable {
        @Override
        public void run() {
            // 在线程里实现网络通信
            try {
                socket1 = new Socket("192.168.4.1", 333); // 调用socket类连接要访问的ip地址和端口。s是任意命名的，现在就相当于socket了。new
                // socket()就是调用socket
                socket1.getOutputStream().write("GEC\r\n".getBytes()); // 从socket里调用输出流（也就是getOutputStream方法），
                // 再调用输出流里面的write方法给无人机发数据。GEC\r\n就是要发送的数据，getBytes方法的作用是把字符串转换成字节形式。
                new Thread(new Send_rock()).start();
//                tvx = (TextView) findViewById(R.id.tvx);
//                tvx.setText("已连接");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发通信数组给无人机
     */
    class Send_rock implements Runnable {
        @Override
        public void run() {
            try {
                outputStream1 = socket1.getOutputStream(); // 从socket里调用输出流
                while (flag1) // while循环，当flag值为true时执行循环，值为false时不执行循环。
                {
                    outputStream1.write(data1); // 从输出流里调用write方法发通信数组给无人机
                    try {
                        Thread.sleep(4); // 每隔4毫秒执行一次循环
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        flag1 = false;
        super.onDestroy();
    }


    /**
     * 陀螺仪传感器
     */
    //坐标轴都是手机从左侧到右侧的水平方向为x轴正向，从手机下部到上部为y轴正向，垂直于手机屏幕向上为z轴正向
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // x,y,z分别存储坐标轴x,y,z上的加速度
            float x = (((event.values[0]) - ((event.values[0]) % 1)) * 30) + 1500;//左右
            float y = (((event.values[1]) - ((event.values[1]) % 1)) * 28) + 1530;//前后
            float z = ((event.values[2]) - ((event.values[2]) % 1));

            //前后
            data1[9] = (byte) ((int) (3000 - y) >> 8); // 俯仰值的高八位
            data1[10] = (byte) ((int) (3000 - y) & 0xff); // 俯仰值的低八位
            if ((3000 - y) > 1470) {
                tv_qh1.setText("前：" + (3000 - y));
            } else if ((3000 - y) == 1470) {
                tv_qh1.setText("平衡：" + (3000 - y));
            }else {
                tv_qh1.setText("后：" + (3000 - y));
            }

            //左右
            data1[7] = (byte) ((int) x >> 8); // 横滚值的高八位
            data1[8] = (byte) ((int) x & 0xff); // 横滚值的低八位
            if ((x) > 1500) {
                tv_zy1.setText("左：" + x);
            } else if (x == 1500) {
                tv_qh1.setText("平衡：" + x);
            }else {
                tv_zy1.setText("右：" + x);
            }
        }
        //将当前时间赋值给timestamp
        timestamp = event.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}


