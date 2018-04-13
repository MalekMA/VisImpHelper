package com.example.a100541476.visimphelper;

import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class RetrieveDirections extends AsyncTask<double[], Void,ArrayList<Step>> {
    private Exception exception = null;
    private RetrieveDirectionsListener listener;

    public RetrieveDirections(RetrieveDirectionsListener listener) {this.listener = listener;}

    protected ArrayList<Step> doInBackground(double[]... params){

        String mLat = Double.toString(params[0][0]);
        String mLong = Double.toString(params[0][1]);
        String dLat = Double.toString(params[0][2]);
        String dLong = Double.toString(params[0][3]);

        ArrayList<Step> theSteps = new ArrayList<>();

        String setURL = "https://maps.googleapis.com/maps/api/directions/json?origin="+mLat+","+mLong+"&destination="+dLat+","+dLong+"&mode=walking&key=AIzaSyCqc_Vyuz1Dx0sP1EMeaI9MPNAvMOVh8RM";
        Log.d("THE URL", setURL);
        try {
            InputStream inputStream = new URL(setURL).openStream();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                StringBuilder sb = new StringBuilder();
                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                String result = sb.toString();
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray routes = null;
                    JSONArray legs = null;
                    JSONArray steps = null;
                    routes = json.getJSONArray("routes");
                    legs = ((JSONObject) routes.get(0)).getJSONArray("legs");
                    steps = ((JSONObject) legs.get(0)).getJSONArray("steps");
                    for(int i=0; i<steps.length(); i++){
                        double nLat;
                        double nLon;
                        String step;
                        nLat = (Double) ((JSONObject) ((JSONObject) steps.get(i)).get("end_location")).get("lat");
                        nLon = (Double) ((JSONObject) ((JSONObject) steps.get(i)).get("end_location")).get("lng");
                        step = (String) ((JSONObject) steps.get(i)).get("html_instructions");
                        step = Html.fromHtml(step).toString();
                        Step s = new Step(step, nLat, nLon);
                        theSteps.add(s);
                        Log.d("Step", step);
                    }
                }catch(JSONException e){
                    Log.d("JSON E", e.getMessage());
                }
                Log.d("RESULT", result);
            }finally {
                inputStream.close();
            }
        }catch(MalformedURLException e){
            Log.d("MALFORMED URL", e.getMessage());
        }catch(IOException e){
            Log.d("IOEXCEPTION", e.getMessage());
        }

        return theSteps;
    }

    protected void onPostExecute(ArrayList<Step> steps){
        if(this.exception != null){
            Log.d("onPost Exception", this.exception.getMessage());
        }else{
            this.listener.directionsRetrieved(steps);
        }
    }
}
