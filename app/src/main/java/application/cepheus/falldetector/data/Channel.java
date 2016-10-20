package application.cepheus.falldetector.data;

import org.json.JSONObject;

/**
 * Created by liuyiming091 on 2016/10/16 0016.
 */
public class Channel implements JSONPopulator{
    private Units units;
    private Item item;

    public Item getItem() {
        return item;
    }

    public Units getUnits() {
        return units;
    }

    @Override
    public void populate(JSONObject data) {
        units = new Units();
        units.populate(data.optJSONObject("units"));

        item =new Item();
        item.populate(data.optJSONObject("item"));


    }
}
