package com.example.polytech.orientatewatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    Node mNode; // the connected device to send the message to
    GoogleApiClient mGoogleApiClient;
    private static final String HELLO_WORLD_WEAR_PATH = "/hello-world-wear";
    private boolean mResolvingError=false;
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private boolean mIsInResolution;

    public static final String ACTION_CLOSE = "com.example.polytech.orientatewatch.ACTION_CLOSE";
    private FirstReceiver firstReceiver;

    ViewPager viewPager;
    MyPagerAdapter myPagerAdapter;

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private List<String> settings = new ArrayList<String>(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings.add("250");
        settings.add("restaurant");
        IntentFilter filter = new IntentFilter(ACTION_CLOSE);
        firstReceiver = new FirstReceiver();
        registerReceiver(firstReceiver, filter);

        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();


        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                viewPager = (ViewPager) stub.findViewById(R.id.myviewpager);
                myPagerAdapter = new MyPagerAdapter();
                viewPager.setAdapter(myPagerAdapter);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        resolveNode();
        Log.i("TAG", "GoogleApiClient connected");
    }

    /*
     * Resolve the node = the connected device to send the message to
     */
    private void resolveNode() {

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                for (Node node : nodes.getNodes()) {
                    mNode = node;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("TAG", "GoogleApiClient connection suspended");
        retryConnecting();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("TAG", "GoogleApiClient connection failed: " + connectionResult.toString());
        if (!connectionResult.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    connectionResult.getErrorCode(), this, 0, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e("TAG", "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    class FirstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("FirstReceiver", "FirstReceiver");
            if (intent.getAction().equals(ACTION_CLOSE)) {
                MainActivity.this.finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(firstReceiver);
    }

    private class MyPagerAdapter extends PagerAdapter {

        int NumberOfPages = 2;

        int[] backgroundcolor = {
                0xFF101010,
                0xFF101010};

        @Override
        public int getCount() {
            return NumberOfPages;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        /**
         * Send message to mobile handheld
         */
        private void sendMessage() {

            // write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            for (String element : settings) {
                try{
                    out.writeUTF(element);
                }catch(IOException e){
                    Log.e("ARRAY TO BYTE","Erreur lors du passage du vecteur string en byte[]");
                }
            }
            byte[] bytes = baos.toByteArray();

            if (mNode != null && mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
                Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, mNode.getId(), HELLO_WORLD_WEAR_PATH, bytes).setResultCallback(

                        new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    Log.e("TAG", "Failed to send message with status code: "
                                            + sendMessageResult.getStatus().getStatusCode());
                                }
                            }
                        }
                );
            }else{
                //Improve your code
            }

        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {

            LinearLayout layout = new LinearLayout(MainActivity.this);;
            if (position == 0){
                TextView textView = new TextView(MainActivity.this);
                textView.setBackgroundColor(Color.WHITE);
                textView.setTextColor(Color.BLACK);
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                textView.setText("Orientate Watch");
                textView.setTextSize(18);

                Button button = new Button(MainActivity.this);
                button.setText("Rechercher");
                button.setTextColor(Color.BLACK);
                button.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        sendMessage();
                    }
                });
                button.setBackgroundColor(Color.rgb(30,144,255));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0,25,0,0);
                button.setLayoutParams(params);

                Drawable d = getResources().getDrawable(R.drawable.settings);
                ImageView imageview = new ImageView(MainActivity.this);
                imageview.setImageDrawable(d);
                imageview.setBackgroundColor(Color.WHITE);
                params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0,25,0,0);
                imageview.setLayoutParams(params);
                //imageview.getLayoutParams().height = 50;
                //imageview.getLayoutParams().width = 50;

                layout.setOrientation(LinearLayout.VERTICAL);
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
                layout.setBackgroundColor(backgroundcolor[position]);
                layout.setLayoutParams(layoutParams);

                layout.addView(textView);
                layout.addView(button);
                layout.addView(imageview);

            }else if (position == 1){

                TextView textView = new TextView(MainActivity.this);
                textView.setTextColor(Color.BLACK);
                textView.setBackgroundColor(Color.WHITE);
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                textView.setText("Configurations");
                textView.setTextSize(18);

                Button button1 = new Button(MainActivity.this);
                button1.setText("Changer Rayon");
                button1.setTextSize(15);
                button1.setTextColor(Color.BLACK);
                button1.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                button1.setBackgroundColor(Color.rgb(30,144,255));
                button1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH);
                        intent.putExtra("settings", "rayon");
                        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez pour indiquer le rayon (en mètres): ");
                        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
                    }
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0,20,0,0);
                button1.setLayoutParams(params);

                Button button2 = new Button(MainActivity.this);
                button2.setText("Type de Points d'intêrets");
                button2.setTextSize(15);
                button2.setTextColor(Color.BLACK);
                button2.setDrawingCacheBackgroundColor(Color.WHITE);
                button2.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                button2.setBackgroundColor(Color.rgb(30,144,255));
                button2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH);
                        intent.putExtra("settings", "type");
                        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez pour indiquer le type de point d'intérêt: ");
                        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
                    }
                });
                params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0,30,0,20);
                button2.setLayoutParams(params);

                TextView textView2 = new TextView(MainActivity.this);
                textView2.setTextColor(Color.WHITE);
                textView2.setBackgroundColor(Color.WHITE);
                textView2.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                textView2.setTextSize(18);
                textView2.setText("design");

                layout.setOrientation(LinearLayout.VERTICAL);
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
                layout.setBackgroundColor(backgroundcolor[position]);
                layout.setLayoutParams(layoutParams);

                layout.addView(textView);
                layout.addView(button1);
                layout.addView(button2);
                layout.addView(textView2);
            }

            final int page = position;
            layout.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                }});

            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout)object);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (data.getExtras().get("settings").toString().equals("rayon")){
                settings.set(0,matches.get(0));
            }else{
                settings.set(1,matches.get(0));
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}