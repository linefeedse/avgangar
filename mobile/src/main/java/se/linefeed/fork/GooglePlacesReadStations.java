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

/**
 * Created by torkel on 15-05-19.
 */
public class GooglePlacesReadStations extends AsyncTask<Object, Integer, List<Place>> {

    protected static final String TAG = "GooglePlacesReadSt";
    protected static final String PATH_STATION_INFO = "/GetStationService";

    @Override
    protected List<Place> doInBackground(Object... obj) {
        String mPeerId = (String) obj[2];
        final GoogleApiClient mGoogleApiClient = (GoogleApiClient) obj[1];
        Location location = (Location) obj[0];
        Places.Response<List<Place>> resp = null;
        Log.d(TAG,"asking for bus_station with radius 500 around "
                + location.getLatitude() + "," + location.getLongitude());
        try {
            resp = Places.nearbySearch(new Places.Params().location(
                            location.getLatitude(),
                            location.getLongitude())
                            .radius(500)
                            .types("bus_station")
                            .maxResults(5)
                            .rankBy(Places.Params.RankBy.DISTANCE),
                    Places.Field.NAME, Places.Field.VICINITY);
        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
        Places.Response.Status status = null;
        List<Place> places = null;
        try {
            status = resp.getStatus();
            places = resp.getResult();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        if (status == Places.Response.Status.OK && places != null) {
            Log.d(TAG,"have some places");
            DataMap placemap = new DataMap();
            int order = 0;
            for (Place place : places) {
                placemap.putString(place.getName(),Integer.toString(order++));
                Log.i(TAG, place.getName());
            }
            Log.d(TAG, "Sending update message to " + mPeerId + " for path " + PATH_STATION_INFO);
            Wearable.MessageApi.sendMessage(mGoogleApiClient,mPeerId,PATH_STATION_INFO, placemap.toByteArray()).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG,"SendUpdateMessage: " + sendMessageResult.getStatus());
                            //mGoogleApiClient.disconnect();
                        }
                    }
            );
        } else if (status == Places.Response.Status.ZERO_RESULTS) {
            Log.i(TAG,"no results");
        } else {
            Log.e(TAG,"error: " + status);
        }
        return places;
    }
    @Override
    protected void onPostExecute(List<Place> places) {
        for (Place place : places) {
            Log.d(TAG, place.getName() + " - " + place.getVicinity());
        }
    }
}