

package com.example.test;


import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.RequiresApi;
import android.view.View.OnClickListener;
//import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.MyLocationStyle;


public class BTClient extends Activity implements View.OnClickListener {

    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号
    private InputStream is;    //输入流，用来接收蓝牙数据
    Button lianjie;
    BluetoothDevice _device = null;     //蓝牙设备
    BluetoothSocket _socket = null;      //蓝牙通信socket
    boolean _discoveryFinished = false;
    boolean bRun = true;
    boolean bThread = false;
    Bundle save;
    TextView xinlvtext, xueyangtext;

    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备
    private Context context = null;
    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private Button startLocationButton = null;
    private Button stopLocationButton = null;

    double longitude = 0.0;
    double latitude = 0.0;

    MapView mMapView = null;
    AMap aMap;
    MyLocationStyle myLocationStyle;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);   //设置画面为主画面 main.xml
        context = this;
        lianjie = (Button) findViewById(R.id.lianjie);
        lianjie.setOnClickListener(new lianjie());

        xinlvtext = (TextView) findViewById(R.id.xinlvtext);
        xueyangtext = (TextView) findViewById(R.id.xueyangtext);
        startLocationButton = (Button) findViewById(R.id.id_start_location);
        stopLocationButton = (Button) findViewById(R.id.id_stop_location);
        startLocationButton.setOnClickListener(this);
        stopLocationButton.setOnClickListener(this);

        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        if(aMap == null){
            aMap = mMapView.getMap();
        }

        //如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (_bluetooth == null) {
            Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //定位style
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000);
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW) ;
        myLocationStyle.showMyLocation(true);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);

        //设置定位
        locationClient = new AMapLocationClient(this.getApplicationContext());
        locationOption = getDefaultOption();
        locationClient.setLocationOption(locationOption);
        locationClient.setLocationListener(locationListener);
        //开始定位
        startLocation();
        requestPermission();

        // 设备可以被搜索
        new Thread() {
            @Override
            public void run() {
                if (_bluetooth.isEnabled() == false) {
                    //requestBLUETOOTHPermission();
                    _bluetooth.enable();
                }
            }
        }.start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.id_start_location:
                Toast.makeText(context, "已开启定位，请耐心等待。。。", Toast.LENGTH_SHORT).show();
                startLocation();
                break;
            case R.id.id_stop_location:
                Toast.makeText(context, "已关闭定位监听", Toast.LENGTH_SHORT).show();
                longitude = 0.0;
                latitude = 0.0;
                stopLocation();
                break;
        }
    }


    class lianjie implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (_bluetooth.isEnabled() == false) {  //如果蓝牙服务不可用则提示
                Toast.makeText(BTClient.this, " 打开蓝牙中...", Toast.LENGTH_LONG).show();
                return;
            }

            //如未连接设备则打开DeviceListActivity进行设备搜索
            //	Button btn = (Button) findViewById(R.id.Button03);
            if (_socket == null) {
                Intent serverIntent = new Intent(BTClient.this, DeviceListActivity.class); //跳转程序设置
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
            } else {
                //关闭连接socket
                try {
                    is.close();
                    _socket.close();
                    _socket = null;
                    bRun = false;
                } catch (IOException e) {
                }
            }
            return;
        }
    }


    //接收活动结果，响应startActivityForResult()
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 得到蓝牙设备句柄
                    _device = _bluetooth.getRemoteDevice(address);

                    // 用服务号得到socket
                    try {
                        _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                    } catch (IOException e) {
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                    //连接socket
                    //Button btn = (Button) findViewById(R.id.Button03);
                    try {
                        _socket.connect();
                        Toast.makeText(this, "连接" + _device.getName() + "成功！", Toast.LENGTH_SHORT).show();
                        //	btn.setText("断开");
                    } catch (IOException e) {
                        try {
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                            _socket.close();
                            _socket = null;
                        } catch (IOException ee) {
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    //打开接收线程
                    try {
                        is = _socket.getInputStream();   //得到蓝牙数据输入流
                    } catch (IOException e) {
                        Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (bThread == false) {
                        ReadThread.start();
                        bThread = true;
                    } else {
                        bRun = true;
                    }
                }
                break;
            default:
                break;
        }
    }

    //接收数据线程
    Thread ReadThread = new Thread() {
        @Override
        public void run() {
            int num = 0;
            byte[] buffer = new byte[1024];

            bRun = true;
            //接收线程
            while (true) {
                try {
                    while (is.available() == 0) {
                        while (bRun == false) {
                        }
                    }
                    while (true) {//在采集单个数据的时候把while(true给去掉)
                        num = is.read(buffer);         //读入数据
                        Message message = new Message(); // 通知界面
                        message.what = 2;
                        message.obj = buffer;
                        mHandler.sendMessage(message);
                    }
                } catch (IOException e) {
                }
            }
        }
    };

    //接收温湿度显示的函数
    Handler mHandler = new Handler()//温湿度数据显示
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 2:
                    byte buffer[] = (byte[]) msg.obj;
                    refreshView(buffer); // 接收到数据后显示
                    break;
            }
        }

        private void refreshView(byte[] buffer) {
            int xinlv = (buffer[0] & 0xff);
            int xueyang = (buffer[1] & 0xff);
            int wendu1 = (buffer[2] & 0xff);
            int wendu2 = (buffer[3] & 0xff);

            final StringBuffer stringBuffer = new StringBuffer(16);
            if (xinlv != 0) {
                stringBuffer.append(xinlv);
            }
            stringBuffer.append(xueyang).append(".").append(wendu1).append(wendu2);
            xinlvtext.setText("糖度值：" + stringBuffer.toString());
            long time = System.currentTimeMillis();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date date = new Date(time);
            final String t1 = format.format(date);
            boolean isResult = JavaForExcel.writeToExcel(t1, stringBuffer.toString(), longitude, latitude);
            if (!isResult) {
                JavaForExcel.createExcel(context);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        JavaForExcel.writeToExcel(t1, stringBuffer.toString(), longitude, latitude);
                    }
                }, 500);
            }
        }
    };


    //关闭程序掉用处理部分
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_socket != null) {  //关闭连接socket
            try {
                _socket.close();
            } catch (IOException e) {
            }
            //	_bluetooth.disable();  //关闭蓝牙服务
        }
        destroyLocation();
        mMapView.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.DEFAULT);//可选，设置逆地理信息的语言，默认值为默认语言（根据所在地区选择语言）
        return mOption;
    }

    private void startLocation() {
        // 设置定位参数
        locationClient.setLocationOption(locationOption);
        // 启动定位
        locationClient.startLocation();
        startLocationButton.setEnabled(false);
        stopLocationButton.setEnabled(true);
    }

    private void stopLocation() {
        // 停止定位
        locationClient.stopLocation();
        startLocationButton.setEnabled(true);
        stopLocationButton.setEnabled(false);
    }

    private void destroyLocation() {
        if (null != locationClient) {
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
        }
    }

    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            if (null != location) {
                double longitudeTem = location.getLongitude();
                double latitudeTem = location.getLatitude();
                double[] locationData = JavaForExcel.gcj02_To_Bd09(latitudeTem, longitudeTem);
                if (locationData != null && locationData.length == 2) {
                    latitude = locationData[0];
                    longitude = locationData[1];
                    //xueyangtext.setText("经    度:" + longitude + " 纬    度: " + latitude);
                }
            } else {
                Toast.makeText(context, "定位失败", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH_CONNECT};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }
    }

    private void requestBLUETOOTHPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permission ={Manifest.permission.BLUETOOTH_CONNECT};
            //验证是否许可权限
            if (this.checkSelfPermission(permission[0]) != PackageManager.PERMISSION_GRANTED) {
                //申请权限
                this.requestPermissions(permission, REQUEST_CODE_CONTACT);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (String str : permissions) {
            if (this.checkSelfPermission(str) == PackageManager.PERMISSION_GRANTED) {
                JavaForExcel.createExcel(context);
            }
        }
    }
}
    

    
  
    
   
	