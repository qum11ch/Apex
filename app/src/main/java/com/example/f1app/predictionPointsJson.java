package com.example.f1app;
import org.json.JSONObject;
public final class predictionPointsJson {
    private predictionPointsJson(){}
    public static JSONObject object(String url)throws Exception {
        try {
            JSONObject json=new JSONObject(predictionPointsHttp.get(url));
            json.getJSONObject("MRData");
            return json;
        }catch(Exception e){predictionPointsHttp.invalidate(url);throw e;}
    }
}
