package com.scenemaxeng.projector;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONObjectExt extends JSONObject {

    public JSONObjectExt(JSONObject obj) throws JSONException {
        super(obj.toString());
    }

    public float getFloat(String field) {
        try {
            Double v = this.getDouble(field);
            return v.floatValue();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
