package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.tools.ant.filters.StringInputStream;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.service.DemoShoppingListProvider;

public class ExternalServiceUtil {

    private static final Logger LOG = Logger.getLogger(ExternalServiceUtil.class.getSimpleName());

    private final static String DFT_IMG = "http://i.imgur.com/ZWChwLu.png";
    private final static String DFT_CATEGORY = "other";

    public static Map<String, Map<String, List<Map<String, Object>>>> getConvertedData() throws IOException {

        Map<String, Map<String, List<Map<String, Object>>>> result = new HashMap<String, Map<String, List<Map<String, Object>>>>();

        List<Map<String, Object>> allLists = getAllShoppingList();

        for (int i = 0; i < allLists.size(); i++) {
            String id = (String) allLists.get(i).get("Id");
            result.put(id, convertList(getShoppingList(id)));
        }
        return result;

    }

    private static List<Map<String, Object>> getAllShoppingList() throws ClientProtocolException, IOException {
        String shoppingCollectionUrl = buildUrl("ShoppingListCollection");

        JsonFactory jsonFactory = new JacksonFactory();

        List<Map<String, Object>> result = null;
        String jsonResponse = fetchFromUrl(shoppingCollectionUrl);
        LOG.info("-----Got all lists:" + jsonResponse);
        result = jsonFactory.fromInputStream(new StringInputStream(jsonResponse), null);
        // result = jsonFactory.fromInputStream(
        // DemoShoppingListProvider.class.getResourceAsStream("/productData_external_allLists.json"),null);
        return result;
    }

    private static Map<String, Object> getShoppingList(String shoppingListId) throws ClientProtocolException,
            IOException {
        String shoppingListUrl = buildUrl("ShoppingList/" + shoppingListId);
        JsonFactory jsonFactory = new JacksonFactory();

        Map<String, Object> result = null;
        String jsonResponse = fetchFromUrl(shoppingListUrl);
        LOG.info("-----Got list:" + jsonResponse);
        result = jsonFactory.fromInputStream(new StringInputStream(jsonResponse), null);
        // String shoppingListStr= ((Map<String,String>)jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData_external_DetailsLists.json"),null)).get(shoppingListId);
        // result = jsonFactory.fromInputStream(new StringInputStream(shoppingListStr), null);
        return result;
    }

    private static Map<String, List<Map<String, Object>>> convertList(Map<String, Object> dataFromExternal) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productLines = (List<Map<String, Object>>) dataFromExternal.get("Lines");

        Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();
        for (int i = 0; i < productLines.size(); i++) {
            Map<String, Object> productLine = productLines.get(i);
            Map<String, Object> product = convertProduct(productLine);
            String category = (String) product.get(Constants.ITEM_COL_CATEGORY);
            if (result.containsKey(category)) {
                List<Map<String, Object>> list = result.get(category);
                list.add(product);
            } else {
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                list.add(product);
                result.put(category, list);
            }

        }
        return result;

    }

    private static String buildUrl(String endPoint) {
        String baseurl = "http://ec2-23-21-131-69.compute-1.amazonaws.com/retalix.sgw/";
        return baseurl + endPoint + "/";
    }

    private static String fetchFromUrl(String shoppingListCollectionUrl) throws IOException, ClientProtocolException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        ResponseHandler<String> resonseHandler = new BasicResponseHandler();
        HttpGet getMethod = new HttpGet(shoppingListCollectionUrl);
        // Headers:
        setupRequestHeaders(getMethod);
        String response = httpClient.execute(getMethod, resonseHandler);
        return response;
    }

    private static void setupRequestHeaders(HttpGet getMethod) {
        getMethod.setHeader("Retailer", "synergy");
        getMethod.setHeader("Authorization", "Basic YUBiLmNvbTpxd2VydHk=");
        getMethod.setHeader("TouchPoint", "MobileShopper");
        getMethod.setHeader("Accept-Language", "en-US");
        getMethod.setHeader("Content-Type", "application/json; charset=utf-8");
        getMethod.setHeader("User-Agent", "ShopperGatewayCore/4.1.2 (Simulator; Windows 7; en-US)");
        getMethod.setHeader("Accept", "application/json, text/javascript, */*");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertProduct(Map<String, Object> productFromExternal) {
        Map<String, Object> product = new HashMap<String, Object>();

        product.put(Constants.ITEM_COL_PRD_ID, productFromExternal.get("Id"));
        product.put(Constants.ITEM_COL_PRDNAME,
                extractDescription((Map<String, Object>) productFromExternal.get("Item")));

        String img = (String) productFromExternal.get("Image");
        if (img == null || img.isEmpty()) {
            img = DFT_IMG;
        }
        product.put(Constants.ITEM_COL_IMGURL, img);

        product.put(Constants.ITEM_COL_QUANTITY, productFromExternal.get("Quantity"));

        product.put(Constants.ITEM_COL_PRICE, extractPrice((Map<String, Object>) productFromExternal.get("Item")));

        product.put(Constants.ITEM_COL_PROMO, extractPromotion((Map<String, Object>) productFromExternal.get("Item")));

        product.put(Constants.ITEM_COL_CATEGORY, DFT_CATEGORY);// Eric.TODO

        return product;

    }

    @SuppressWarnings("unchecked")
    private static String extractPrice(Map<String, Object> itemDetailsMap) {
        String prefix = "$";
        String currency = ((Map<String, String>) itemDetailsMap.get("Price")).get("Currency");
        if (currency != null && currency.equals("GBP")) {
            prefix = "\u20A4";
        }
        return prefix + " " + ((Map<String, String>) itemDetailsMap.get("Price")).get("Value");
    }

    @SuppressWarnings("unchecked")
    private static String extractDescription(Map<String, Object> itemDetailsMap) {
        List<Map<String, String>> descs = (List<Map<String, String>>) itemDetailsMap.get("Description");
        String longName = null;
        String shortName = null;
        for (int i = 0; i < descs.size(); i++) {
            Map<String, String> desc = descs.get(i);
            if ("Long".equals(desc.get("TypeCode"))) {
                longName = desc.get("Value");
            } else if ("Short".equals(desc.get("TypeCode"))) {
                shortName = desc.get("Value");
            }
        }
        if (longName != null) {
            return longName;
        } else if (shortName != null) {
            return shortName;
        } else {
            return "UnKnow Product";
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractPromotion(Map<String, Object> itemDetailsMap) {
        List<Map<String, Object>> proms = (List<Map<String, Object>>) itemDetailsMap.get("Promotions");

        String longName = null;
        String shortName = null;

        if (proms != null && proms.size() > 0) {
            Map<String, Object> prom = (Map<String, Object>) proms.get(0);
            List<Map<String, String>> descs = (List<Map<String, String>>) prom.get("Description");

            for (int i = 0; i < descs.size(); i++) {
                Map<String, String> desc = descs.get(i);
                if ("Long".equals(desc.get("TypeCode"))) {
                    longName = desc.get("Value");
                } else if ("Short".equals(desc.get("TypeCode"))) {
                    shortName = desc.get("Value");
                }
            }
        }
        if (longName != null) {
            return longName;
        } else if (shortName != null) {
            return shortName;
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        JsonFactory jsonFactory = new JacksonFactory();
        LOG.info(jsonFactory.toPrettyString(ExternalServiceUtil.getConvertedData()));
    }
}
