package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.util.ArrayList;
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

/**
 * This is a testing class for connectivity test of external service 
 *
 */
public class ConnectivityTester {

    private static final Logger LOG = Logger.getLogger(ConnectivityTester.class.getSimpleName());

    public static boolean run() {
        try {

            String baseurl = "http://ec2-23-21-131-69.compute-1.amazonaws.com/retalix.sgw/";
            String shoppingListCollectionUrl = baseurl + "ShoppingListCollection/";
            LOG.info("-----Start fetcch:" + shoppingListCollectionUrl);
            String slcollectionJson = fetchFromUrl(shoppingListCollectionUrl);
            ArrayList<String> ShoppingLists = new ArrayList<String>();
            LOG.info("-----slcollectionJson:" + slcollectionJson);
            for (String shoppingListId : GetSlIds(slcollectionJson)) {
                String slUrl = baseurl + "ShoppingList/" + shoppingListId + "/";
                LOG.info("-----Going to fetch: " + slUrl);
                String ShoppingList = fetchFromUrl(slUrl);
                ShoppingLists.add(ShoppingList);
                LOG.info("ShoppingList with id: " + shoppingListId);
                LOG.info(ShoppingList);
            }
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }

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

    private static ArrayList<String> GetSlIds(String slcollectionJson) {
        List<Map<String, Object>> list = null;
        ArrayList<String> aList = new ArrayList<String>();
        
        JsonFactory jsonFactory = new JacksonFactory();
        try {
            list = jsonFactory.fromInputStream(new StringInputStream(slcollectionJson),null);
        } catch (IOException e) {
            LOG.severe("Can not init product data");
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }

        for (Map<String, Object> map : list) {
            aList.add(map.get("Id").toString());
        }
        System.out.println(aList.size());
        return aList;
    }
    
    public static void main(String[] args) {
        String slcollectionJson ="[{\"Id\":\"61c368e0-4951-446c-9857-a291015149c2\",\"Name\":\"Sunday's List\",\"ViewOrder\":0,\"LastModifiedDate\":\"2014-01-03T10:57:37\",\"Lines\":[{\"ShopListId\":\"61c368e0-4951-446c-9857-a291015149c2\",\"Id\":\"673a2ff1-8b17-40d8-9129-ad5acd7691e1\",\"Value\":\"1020\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"61c368e0-4951-446c-9857-a291015149c2\",\"Id\":\"cff2ad37-bd65-4fd7-b2d0-b9508df66b96\",\"Value\":\"1090000114\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"61c368e0-4951-446c-9857-a291015149c2\",\"Id\":\"9f6c948d-be7d-4d23-b356-458db92d0dc3\",\"Value\":\"1002\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"61c368e0-4951-446c-9857-a291015149c2\",\"Id\":\"9b336276-42af-47de-8fbc-55963f2062ed\",\"Value\":\"1017\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"61c368e0-4951-446c-9857-a291015149c2\",\"Id\":\"20f4e4b0-97a5-4d50-995c-fd61831716dd\",\"Value\":\"1016\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1}],\"ItemsCount\":5},{\"Id\":\"7c37d977-ad5d-40a2-a5ec-a2a8008d2591\",\"Name\":\"Mom's Birthday List\",\"ViewOrder\":0,\"LastModifiedDate\":\"2014-01-03T11:02:53\",\"Lines\":[{\"ShopListId\":\"7c37d977-ad5d-40a2-a5ec-a2a8008d2591\",\"Id\":\"fe8e6a5c-6750-4814-8113-ec0d34df1269\",\"Value\":\"4014400400007\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"7c37d977-ad5d-40a2-a5ec-a2a8008d2591\",\"Id\":\"6e12efba-6120-4711-808d-264c5c6c92b1\",\"Value\":\"214\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"7c37d977-ad5d-40a2-a5ec-a2a8008d2591\",\"Id\":\"f172621e-3c64-4103-b755-5df651c3aee6\",\"Value\":\"348\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1}],\"ItemsCount\":3},{\"Id\":\"d17bb2da-ce13-4eea-99d4-a2a800b68217\",\"Name\":\"Household List\",\"ViewOrder\":0,\"LastModifiedDate\":\"2014-01-03T11:05:33\",\"Lines\":[{\"ShopListId\":\"d17bb2da-ce13-4eea-99d4-a2a800b68217\",\"Id\":\"58c1a440-8119-4a55-87a7-bb4007ad48ba\",\"Value\":\"1087801663\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"d17bb2da-ce13-4eea-99d4-a2a800b68217\",\"Id\":\"dc194f12-9dee-4ed2-b756-cc8b27d969a0\",\"Value\":\"3223619166\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"d17bb2da-ce13-4eea-99d4-a2a800b68217\",\"Id\":\"d12067b5-3df4-4c1c-8760-9003c22e61a2\",\"Value\":\"6670\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1},{\"ShopListId\":\"d17bb2da-ce13-4eea-99d4-a2a800b68217\",\"Id\":\"3b5954cc-3169-437d-85d3-a893054ae9e2\",\"Value\":\"1000\",\"Type\":\"PRODUCT\",\"Quantity\":1.000,\"ViewOrder\":1}],\"ItemsCount\":4}]";
        ConnectivityTester.GetSlIds(slcollectionJson);
    }
}
