package com.tiket.common.email;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class Convertor {
    private Convertor() {
    }

    public static String fromJson(String json) {
        if (json == null || json.isEmpty()) throw new RuntimeException("Json can't be null or empty!");
        if (json.trim().startsWith("{")) {
            JSONObject jsonObject = new JSONObject(json);
            return convertToHtmlTable(fromObject(jsonObject));
        } else if (json.trim().startsWith("[")) {
            JSONArray jsonArray = new JSONArray(json);
            return convertToHtmlTable(jsonArray);
        }
        throw new RuntimeException("Provided value doesn't seem to be a json formatted string!");
    }

    private static JSONArray fromObject(JSONObject object) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(object);
        return jsonArray;
    }

    private static String convertToHtmlTable(JSONArray jsonArray) {
        if (jsonArray.length() == 0) return "";
        Object item = jsonArray.get(0);
        if (!(item instanceof JSONObject) && !(item instanceof JSONArray))
            return item.toString();
        if (item instanceof JSONArray)
            return convertToHtmlTable((JSONArray) item);

        List<String> keys = jsonArray.getJSONObject(0).keySet().stream().sorted().toList();
        StringBuilder html = new StringBuilder();
        html.append("<table style=\"border: 1px solid #ddd; padding: 8px;\">");

        // HEAD
        html.append("<tr style=\"background-color: #4CAF50; color: white;padding-top: 5px; padding-bottom: 5px;\">");
        keys.forEach(key -> {
            String title = key.substring(2);    // Removing the numbers used for sorting in pojo name
            html.append("<th style=\"text-align: left;\">").append(title).append("</th>");
        });
        html.append("</tr>");

        // BODY
        html.append("<tbody>");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            html.append("<tr>");
            keys.forEach(key -> {
                Object value = obj.get(key);
                String toAppend;
                if (value instanceof JSONArray) {
                    toAppend = convertToHtmlTable((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    toAppend = convertToHtmlTable(fromObject((JSONObject) value));
                } else
                    toAppend = value.toString();
                html.append("<td>").append(toAppend).append("</td>");
            });
            html.append("</tr>");
        }
        html.append("</tbody>");

        html.append("</table>");
        return html.toString();
    }
}