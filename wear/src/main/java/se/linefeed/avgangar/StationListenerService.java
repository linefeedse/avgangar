package se.linefeed.avgangar;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.lang.reflect.Array;

/**
 * Created by torkel on 15-05-20.
 */
public class StationListenerService extends WearableListenerService {
    private static final String TAG = "StationListenerSrv";
    protected static final String PATH_STATION_INFO = "/GetStationService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);

        DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
        String path = messageEvent.getPath();
        Log.d(TAG, "Message for path " + path);
        if (path.equals(PATH_STATION_INFO)) {
            String[] stations = new String[dataMap.keySet().size()];
            for (String key : dataMap.keySet()) {
                int order = Integer.parseInt(dataMap.getString(key));
                if (order <= dataMap.keySet().size()) {
                    stations[order] = key;
                }
            }
            for (String station : stations) {
                Log.d(TAG,station);

            }
        }

    }
}
