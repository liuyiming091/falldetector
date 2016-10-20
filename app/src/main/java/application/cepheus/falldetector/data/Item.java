package application.cepheus.falldetector.data;

import org.json.JSONObject;

/**
 * Created by liuyiming091 on 2016/10/16 0016.
 */
public class Item implements JSONPopulator {
    private Condition condition;

    public Condition getCondition() {
        return condition;
    }

    @Override
    public void populate(JSONObject data) {
        condition=new Condition();
        condition.populate(data.optJSONObject("condition"));

    }
}
