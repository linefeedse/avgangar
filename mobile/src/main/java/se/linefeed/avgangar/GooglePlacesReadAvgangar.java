package se.linefeed.avgangar;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import net.sf.sprockets.google.Place;
import net.sf.sprockets.google.Places;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

public class GooglePlacesReadAvgangar extends AsyncTask<Object, Integer, Void> {
    protected static final String TAG = "GooglePlacesReadSt";
    protected static final String PATH_STATION_INFO = "/GetStationService";
    protected static final String SL_API_TYPEAHEAD = "http://api.sl.se/api2/typeahead.json?maxresults=1&key=";
    protected static final String SL_API_REALTIME = "http://api.sl.se/api2/realtimedepartures.json?timewindow=60&key=";

    static class Site {
        String Name;
        String SiteId;
        String Type;
        String X;
        String Y;
    }

    static class TypeaheadResponse {
        Integer StatusCode;
        String Message;
        Integer ExecutionTime;
        List<Site> ResponseData;
    }

    static class Vehicle {
        String Destination; // "\u00c5kersberga station",
        String DisplayTime; // "4 min",
        String ExpectedDateTime; // "2015-05-21T14:35:56",
        Integer JourneyDirection; // 2,
        String LineNumber; // "623V",
        Integer SiteId; // 2601,
        String StopAreaName; // "Oxenstiernas v\u00e4g",
        Integer StopAreaNumber; // 62311,
        Integer StopPointNumber; // 62312,
        String TimeTabledDateTime; // "2015-05-21T14:31:00",
        String TransportMode; // "BUS"
    }

    static class Bus extends Vehicle {
        String GroupOfLine; // null,
        String StopPointDesignation; // letter,
    }

    static class Metro extends Vehicle {

    }

    static class Train extends Vehicle {
        String SecondaryDestinationName;
        String StopPointDesignation; // track

    }
    static class Tram extends Vehicle {
        String GroupOfLine; // "Tvärbanan"
        String StopPointDesignation; // track

    }

    static class VehicleTypes {
        List<Bus> Buses;
        List<Metro> Metros;
        List<Train> Trains;
        List<Tram> Trams;
    }

    static class RealTimeResponse {
        Integer StatusCode;
        String Message;
        Integer ExecutionTime;
        VehicleTypes ResponseData;
    }

    private static String readUrl(String urlStr) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars,0,read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    @Override
    protected Void doInBackground(Object... obj) {
        Bundle bundle = (Bundle) obj[3];
        String mPeerId = (String) obj[2];
        final GoogleApiClient mGoogleApiClient = (GoogleApiClient) obj[1];
        String station = (String) obj[0];
        DataMap departures = new DataMap();
        Integer numDepartures = 0;
        departures.putString("Avgångar", "0");
        String sites = null;
        String json = null;
        try {
            String key = bundle.getString("SL_API_TYPEAHEAD_KEY");
            sites = readUrl(SL_API_TYPEAHEAD + key + "&searchstring="
                    + URLEncoder.encode(station, "UTF-8"));
        }
        catch (Exception e) {
            Log.e(TAG,"Exception calling readUrl " + e.getClass().toString());
        }
        if (sites == null)
            return null;

        Gson gson = new Gson();
        TypeaheadResponse respSite = gson.fromJson(sites, TypeaheadResponse.class);

        if (respSite.ResponseData.isEmpty()) {
            Log.d(TAG,"Can not find station");
            return null;
        }
        String siteId = respSite.ResponseData.get(0).SiteId;

        try {
            String key = bundle.getString("SL_API_REALTIME_KEY");
            json = readUrl(SL_API_REALTIME + key + "&siteid=" + siteId);
        }
        catch (Exception e) {
            Log.e(TAG,"Exception calling readUrl " + e.getClass().toString());
        }
        if (json == null)
            return null;

        RealTimeResponse respDep = gson.fromJson(json, RealTimeResponse.class);

        if (!respDep.ResponseData.Buses.isEmpty()) {
            for (Bus bus : respDep.ResponseData.Buses) {
                numDepartures++;
                String destination = (bus.Destination.length() > 17 ? bus.Destination.substring(0,14) + "..." : bus.Destination);
                departures.putString(bus.LineNumber + " " + destination + " " + bus.DisplayTime, numDepartures.toString());
            }
        }
        if (!respDep.ResponseData.Metros.isEmpty()) {
            for (Vehicle vehicle : respDep.ResponseData.Metros) {
                numDepartures++;
                String destination = (vehicle.Destination.length() > 18 ? vehicle.Destination.substring(0,15) + "..." : vehicle.Destination);
                departures.putString(vehicle.LineNumber + " " + destination + " " + vehicle.DisplayTime, numDepartures.toString());
            }
        }
        if (!respDep.ResponseData.Trains.isEmpty()) {
            for (Vehicle vehicle : respDep.ResponseData.Trains) {
                numDepartures++;
                String destination = (vehicle.Destination.length() > 18 ? vehicle.Destination.substring(0,15) + "..." : vehicle.Destination);
                departures.putString(vehicle.LineNumber + " " + destination + " " + vehicle.DisplayTime, numDepartures.toString());
            }
        }
        if (!respDep.ResponseData.Trams.isEmpty()) {
            for (Vehicle vehicle : respDep.ResponseData.Trams) {
                numDepartures++;
                String destination = (vehicle.Destination.length() > 18 ? vehicle.Destination.substring(0,15) + "..." : vehicle.Destination);
                departures.putString(vehicle.LineNumber + " " + destination + " " + vehicle.DisplayTime, numDepartures.toString());
            }
        }


        Wearable.MessageApi.sendMessage(mGoogleApiClient,mPeerId,PATH_STATION_INFO + "/Departures", departures.toByteArray()).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    }
                }
        );
        return null;
    }
    @Override
    protected void onPostExecute(Void v) {
    }
}