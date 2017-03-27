package com.zs.yzs.newuav;

import android.app.Activity;
import android.content.Intent;
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

public class MainActivity extends Activity {
    private TextView tv_qh, tv_zy;
    private PanelView panelView;
    private SeekBar skBar;
    private Socket socket;
    private OutputStream outputStream;
    public byte[] data = new byte[34];  // 定义通信数组data,长度为34.
    public boolean flag = true;         // 定义变量flag，用于while循环。
    private long firstTime = 0;
    public int qh_equ = 1470;
    public int zy_equ = 1500;
    public int p = 1405;    //横滚
    public int q = 1308;    //俯仰
    public int m = 1500;    //航向
    public ToggleButton tog_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏系统状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //设置无标题
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //       WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setContentView(new MySurfaceView(this));
        setContentView(R.layout.main);
        //获取控件id
        tog_btn = (ToggleButton) findViewById(R.id.btn_link);
        tv_qh = (TextView) findViewById(R.id.tv_qh);
        tv_zy = (TextView) findViewById(R.id.tv_zy);
        panelView = (PanelView) findViewById(R.id.panView);
        skBar = (SeekBar) findViewById(R.id.seekBar);
        //调用方法
        initdata();
        initViews();
    }

    /**
     * 本方法作用： 添加初始数据给通信数组
     */
    public void initdata() {
        data[0] = (byte) 0xAA; // 通信协议固定数据
        data[1] = (byte) 0xC0; // 通信协议固定数据
        data[2] = (byte) 0x1C; // 通信协议固定数据
        // ------油门值（控制飞行速度与高度）------
        data[3] = (byte) 0x00; // 油门值的高八位
        data[4] = (byte) 0x00; // 油门值的低八位
        // ------航向值（控制旋转角度）------
        data[5] = (byte) (1500 >> 8); // 航向值的高八位
        data[6] = (byte) (1500 & 0xff); // 航向值的低八位
        // ------横滚值（控制左右方向）------
        data[7] = (byte) (1405 >> 8); // 横滚值的高八位
        data[8] = (byte) (1405 & 0xff); // 横滚值的低八位
        // ------仰俯值（控制前后方向）------
        data[9] = (byte) (1305 >> 8); // 仰俯值的高八位
        data[10] = (byte) (1305 & 0xff); // 仰俯值的低八位
        // 剩余其他位置的数据由于默认就是0，并且加油门时不用，所以可以省略掉。
        data[31] = (byte) 0x1C; // 通信协议固定数据
        data[32] = (byte) 0x0D; // 通信协议固定数据
        data[33] = (byte) 0x0A; // 通信协议固定数据
    }


    //点击连接无人机
    public void link(View view) {
        new Thread(new ConnThread()).start();
        flag = true;
        if (tog_btn.isChecked()) {
        } else {

            // Kill app
            //android.os.Process.killProcess(android.os.Process.myPid());
            onDestroy();
            //System.exit(0);//正常退出App

        }
    }

    //跳转到重力控制
    public void gravity(View view) {
        Intent intent = new Intent(MainActivity.this, Gravity_sensor.class);
        startActivity(intent);

    }

    //点击停止无人机
    public void stop(View view) {
        data[3] = (byte) 0x00; // 油门值的高八位
        data[4] = (byte) 0x00;
        skBar.setProgress(0);
        //设置航行值为中间值 1500
        data[5] = (byte) (1500 >> 8); // 横滚值的高八位
        data[6] = (byte) (1500 & 0xff); // 横滚值的低八位

        //设置横滚值为中间值 1500（控制左右方向）
        data[7] = (byte) (1500 >> 8); // 横滚值的高八位
        data[8] = (byte) (1500 & 0xff); // 横滚值的低八位
        p = 1500;
        tv_zy.setText("平衡值：" + p);
        // ------仰俯值（控制前后方向）------（前后）
        data[9] = (byte) (1470 >> 8); // 仰俯值的高八位
        data[10] = (byte) (1470 & 0xff); // 仰俯值的低八位
        q = 1470;
        tv_qh.setText("平衡值：" + q);
    }


    //seekbar触摸事件
    private void initViews() {
        skBar.setMax(700);          //设置活动条最大值
        skBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int k = 7;          //仪表盘的值
                panelView.setPercent(progress / k);
                double pg = progress % 5;
                if (pg == 0) {
                    data[3] = (byte) (progress >> 8); // 油门值的高八位
                    data[4] = (byte) (progress & 0xff);
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
     * 控制方向(一键平衡)
     * @param view
     */
/**
 //向前
 public void up(View view) {
 if(q < qh_equ){
 q=qh_equ;
 tv_qh.setText("平衡值："+ q);
 }else if(q < 3000 && q >= qh_equ){
 q +=10;
 data[9] = (byte) (q >> 8); // 横滚值的高八位
 data[10] = (byte) (q & 0xff); // 横滚值的低八位
 tv_qh.setText("前："+ q);
 }
 }



 //向后
 public void below(View view) {
 if(q > qh_equ){
 q=qh_equ;
 tv_qh.setText("平衡值："+ q);
 }else if(q <= qh_equ && q > 0){
 q -=10;
 data[9] = (byte) (q >> 8); // 横滚值的高八位
 data[10] = (byte) (q & 0xff); // 横滚值的低八位

 tv_qh.setText("后："+ q);
 }
 }
 //向左
 public void lift(View view) {
 if(p < 1500){
 p=1500;
 tv_zy.setText("平衡值："+ p);
 }else if(p < 3000 && p >= 1500){
 p +=10;
 data[7] = (byte) (p >> 8); // 横滚值的高八位
 data[8] = (byte) (p & 0xff); // 横滚值的低八位

 tv_zy.setText("左："+ p);
 }
 }
 //向右
 public void right(View view) {
 if(p > 1500){
 p=1500;
 tv_zy.setText("平衡值："+ p);
 }else if(p <= 1500 && p > 0){
 p -=10;
 data[7] = (byte) (p >> 8); // 横滚值的高八位
 data[8] = (byte) (p & 0xff); // 横滚值的低八位

 tv_zy.setText("右："+ p);
 }
 }
 */


    /**
     * 控制方向(逐渐平衡)
     *
     * @param view
     */

    //向前
    public void up(View view) {
        if (q < 1505 && q >= 1105) {
            q += 20;
            data[9] = (byte) (q >> 8); // 俯仰值的高八位
            data[10] = (byte) (q & 0xff); // 俯仰值的低八位
            tv_qh.setText("前：" + q);
        }
    }


    //向后
    public void below(View view) {
        if (q <= 1505 && q > 1105) {
            q -= 20;
            data[9] = (byte) (q >> 8); // 俯仰值的高八位
            data[10] = (byte) (q & 0xff); // 俯仰值的低八位

            tv_qh.setText("后：" + q);
        }
    }

    //向左
    public void lift(View view) {
        if (p < 1605 && p >= 1205) {
            p += 10;
            data[7] = (byte) (p >> 8); // 横滚值的高八位
            data[8] = (byte) (p & 0xff); // 横滚值的低八位

            tv_zy.setText("左：" + p);
        }
    }

    //向右
    public void right(View view) {
        if (p <= 1605 && p > 1205) {
            p -= 10;
            data[7] = (byte) (p >> 8); // 横滚值的高八位
            data[8] = (byte) (p & 0xff); // 横滚值的低八位

            tv_zy.setText("右：" + p);
        }
    }


    /**
     * 设置偏转    航向
     *
     * @param view
     */


    //顺时针
    public void obey(View view) {


     /*
        if(m > 1500){
            m=1500;
        }else if(m <= 1500 && m > 0){
            p -=20;
            data[5] = (byte) (m >> 8); // 横滚值的高八位
            data[6] = (byte) (m & 0xff); // 横滚值的低八位
        }
        */
    }

    //逆时针
    public void athwart(View view) {

     /*
      if(m < 1500){

            m=1500;
        }else if(m > 3000 && m >= 1500){
            m +=20;
            data[5] = (byte) (m >> 8); // 横滚值的高八位
            data[6] = (byte) (m & 0xff); // 横滚值的低八位
        }
        */
    }


    /**
     * 连接无人机   线程
     */
    class ConnThread implements Runnable {
        @Override
        public void run() {
            // 在线程里实现网络通信
            try {
                socket = new Socket("192.168.4.1", 333); // 调用socket类连接要访问的ip地址和端口。s是任意命名的，现在就相当于socket了。new
                // socket()就是调用socket
                socket.getOutputStream().write("GEC\r\n".getBytes()); // 从socket里调用输出流（也就是getOutputStream方法），
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
                outputStream = socket.getOutputStream(); // 从socket里调用输出流
                while (flag) // while循环，当flag值为true时执行循环，值为false时不执行循环。
                {
                    outputStream.write(data); // 从输出流里调用write方法发通信数组给无人机
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
        flag = false;
//        try {
//            socket.close();
//        }catch (IOException e){
//        }
        super.onDestroy();
    }
}

