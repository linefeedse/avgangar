package se.linefeed.avgangar;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import net.sf.sprockets.google.Place;
import net.sf.sprockets.google.Places;

import java.util.List;

public class GooglePlacesReadAvgangar extends AsyncTask<Object, Integer, Void> {

    protected static final String TAG = "GooglePlacesReadSt";
    protected static final String PATH_STATION_INFO = "/GetStationService";

    @Override
    protected Void doInBackground(Object... obj) {
        String mPeerId = (String) obj[2];
        final GoogleApiClient mGoogleApiClient = (GoogleApiClient) obj[1];
        String station = (String) obj[0];
        Log.d(TAG,"asking for departures at " + station);
        DataMap departures = new DataMap();
        departures.putString("Avgångar", "0");
        
        Wearable.MessageApi.sendMessage(mGoogleApiClient,mPeerId,PATH_STATION_INFO + "/Departures", departures.toByteArray()).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "SendUpdateMessage: " + sendMessageResult.getStatus());
                    }
                }
        );
        return null;
    }
    @Override
    protected void onPostExecute(Void v) {
    }
}