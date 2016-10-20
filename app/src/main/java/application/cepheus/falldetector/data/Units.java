package application.cepheus.falldetector.data;

import org.json.JSONObject;

/**
 * Created by liuyiming091 on 2016/10/16 0016.
 */
public class Units implements JSONPopulator {
    private String temperature;

    public String getTemperature() {
        return temperature;
    }

    @Override
    public void populate(JSONObject data) {
        temperature=data.optString("temperature");

    }
}
