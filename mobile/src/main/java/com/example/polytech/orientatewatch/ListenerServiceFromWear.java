package com.example.polytech.orientatewatch;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hamdaniy on 31/03/15.
 */
public class ListenerServiceFromWear extends WearableListenerService {

    private static final String HELLO_WORLD_WEAR_PATH = "/hello-world-wear";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {


        /*
         * Receive the message from wear
         */

        List<String> settings = new ArrayList<String>(2);

        if (messageEvent.getPath().equals(HELLO_WORLD_WEAR_PATH)) {

            Intent startIntent = new Intent(this, MainActivity.class);
            // read from byte array
            ByteArrayInputStream bais = new ByteArrayInputStream(messageEvent.getData());
            DataInputStream in = new DataInputStream(bais);
            try{
                while (in.available() > 0) {
                    String element = in.readUTF();
                    settings.add(element);
                }
            }catch(IOException e){
                Log.e("BYTE TO ARRAY", "Erreur lors du passage du byte[] en vecteur string");
            }

            startIntent.putExtra("rayon", settings.get(0));
            startIntent.putExtra("type", settings.get(1));
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(startIntent);
        }

    }

}