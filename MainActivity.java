package com.example.suraj.indicator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    private ServerSocket serverSocket;
    private Socket tempSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 3175;
    private Handler handler;

    TextView tv_accX, tv_accY;
    ToggleButton toggleButton;
    SensorManager accManager;
    Sensor accSensor;
    int x2;
    String i1,i2,i3,i4,i5,i6,message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this.getApplicationContext())) {

            } else {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
        CreateNewWifiApNetwork();

        accManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = accManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        handler = new Handler();
        tv_accX = (TextView) findViewById(R.id.accX);
        tv_accY = (TextView)findViewById(R.id.accY);
        final SeekBar seekbar= (SeekBar) findViewById(R.id.seekBar);
        final ArcSeekBar arcSeekBar = (ArcSeekBar) findViewById(R.id.defaultSeekBar);
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
        final Button button=(Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(button.getText().equals("Manual OFF")) {
                    button.setText("Manual ON");
                    isButtonPressed=true;
                }
                else
                {
                    isButtonPressed=false;
                    button.setText("Manual OFF");
                }
            }
        });

        arcSeekBar.setMaxProgress(256);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);  //hotspot
        arcSeekBar.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int i) {
                i5=String.valueOf(i);
            }
        });
        startTimer();
        seekbar.setMin(-100);
        seekbar.setMax(100);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar,int progress, boolean fromUser) {
                i4=String.valueOf(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        startTimer();
        /*arcSeekBar.setMaxProgress(200);
       // ProgressListener progressListener;
        progressListener = progress -> Log.i("SeekBar", "Value is " + progress);
        progressListener.invoke(0);
        arcSeekBar.setOnProgressChangedListener(progressListener);
        i5=String.valueOf(progress);
        //int[] intArray = getResources().getIntArray(R.array.progressGradientColors);
        // arcSeekBar.setProgressGradient(intArray);*/
    }


    boolean isButtonPressed=false;

    private Timer timer;
    private TimerTask timerTask;
    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 10 minutus
        timer.schedule(timerTask, 1000, 100); //
    }

    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                if(isButtonPressed)
                {
                    i6=i4+","+i5;
                    sendMessageLEd(i6);
                }
                else
                {
                    sendMessageLEd(i3);
                }

            }
        };
    }

  /*  public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
*/
    private void sendMessageLEd(final String message) {
        try {
            if (null != tempSocket) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrintWriter out = null;
                        try {
                            out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(tempSocket.getOutputStream())),
                                    true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        out.println(message);
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                //findViewById(R.id.start_server).setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
                //showMessage("Error Starting Server : " + e.getMessage(), Color.RED);
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED);
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                //showMessage("Error Connecting to Client!!", Color.RED);
            }
            //showMessage("Connected to Client!!", greenColor);
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        read = "Client Disconnected";
                        //showMessage("Client : " + read, greenColor);
                        break;
                    }
                    //showMessage("Client : " + read, greenColor);
                    if(read.equals("LED"))
                    {
                        // showMessage("Connected to Client!!", greenColor);
                        tempSocket = clientSocket;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            //sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
        }
    }


    public void onResume() {
        super.onResume();
        accManager.registerListener(accListener, accSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onStop() {
        super.onStop();
        accManager.unregisterListener(accListener);
    }

    public void CreateNewWifiApNetwork() {               //hotspot

        ApManager ap = new ApManager(this.getApplicationContext());
        ap.createNewNetwork("madhu12","gulabjam");
    }
    public SensorEventListener accListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            float x = event.values[0]*10;
            float y = event.values[1]*10;
            int x1= (int)((x-100)*(-1.28));
            i1=String.valueOf(x1);
            int x2=(int)y;
            i2=String.valueOf(x2);
            i3=i2+","+i1;
            tv_accX.setText("X Acc : " + x1 );
            tv_accY.setText("Y Acc: " + x2);

        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
}
