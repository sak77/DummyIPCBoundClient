package com.sshriwas.dummyipcboundclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


/**
 * For this app we have 2 goals -
 * 1. bind to a remote service and communicate with it via messages
 * 2. Allow the remote service to send replies back to this client via replyTo attribute of message
 *
 * The source for the service app can be found at - https://github.com/sak77/SampleServiceApp
 *
 */
public class MainActivity extends AppCompatActivity {

    //local handler for client app. This will process messages on the client's main messagequque
    Handler clientHandler;
    //the clientMessenger will be assigned to the replyTo attribute of the message being sent to
    //the remote service.
    //The remote service will use the messenger to dispatch messages to the local handler .i.e. clientHandler
    private Messenger clientsMessenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnStart)
                .setOnClickListener(view -> {
                    bindService();
                });

        //Define a local handler to send/process messages on the messagequeue of the mainlooper
        clientHandler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //Here we process messages on the messagequeue. Probably coming from the
                //remote service....
                if (msg.obj != null){
                    Bundle bundle = (Bundle) msg.obj;
                    String reply = bundle.getString("RESPONSE");
                    Log.v("CLIENT", "SERVICE REPLY: " + reply);
                }
            }
        };
        //messenger which will be used by service to send messages back to clientHandler
        clientsMessenger = new Messenger(clientHandler);
    }

    //binds to remote service
    private void bindService() {
        //Here we are trying to bind to a remote service via intent filters
        //Service intent must be explicit. Implicit intents are considered a security risk
        //Hence, it is important to specify the package name for the intent.
        Intent inBind = new Intent("com.example.sshriwas.sampleserviceapp");
        inBind.setPackage("com.example.sshriwas.sampleserviceapp");
        bindService(inBind, new MyServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    //callback method that listens to service connected state
    private class MyServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Messenger messenger = new Messenger(iBinder);
            //While the constructor of Message is public,
            // the best way to get one of these is to call Message.obtain() or one of the Handler.obtainMessage() methods,
            // which will pull them from a pool of recycled objects.
            Message message = Message.obtain();
            message.what = 1;
            //Passing string directly as message.obj give error-
            //Can't marshal non-Parcelable objects across processes.
            //so for IPC you can use bundles instead.
            Bundle bundle = new Bundle();
            bundle.putString("id", "Remote Client");
            message.obj = bundle;
            //You can specify a messenger to the messages's replyTo attribute. This allows the
            //service to send response to this client via this messenger
            message.replyTo = clientsMessenger;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    }
}
