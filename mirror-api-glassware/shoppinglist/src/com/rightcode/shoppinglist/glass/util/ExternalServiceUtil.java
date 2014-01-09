package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.google.appengine.api.ThreadManager;
import com.rightcode.shoppinglist.glass.Constants;

public class ExternalServiceUtil {

    private static final Logger LOG = Logger.getLogger(ExternalServiceUtil.class.getSimpleName());

    /**
     * To switch between external and local dummy response, use only for testing
     */
    private static boolean enableExternal = false;

    public static Object[] getConvertedData() {

//        ExecutorService exec = Executors.newCachedThreadPool(ThreadManager.currentRequestThreadFactory());
        ExecutorService exec = Executors.newCachedThreadPool();
        FeatchTask task = new FeatchTask();
        Future<Object[]> future = exec.submit(task);
        Object[] taskResult = null;
        try {
            taskResult = future.get(Constants.EXTERNAL_SERVICE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            exec.shutdownNow();
        } catch (Throwable t) {
            // Make sure we can handle all error, so we can fallback to
            // productData.json
            LOG.log(Level.SEVERE, t.getMessage(), t);
        }
        return taskResult;
    }

    private static List<Map<String, Object>> getAllShoppingList() throws ClientProtocolException, IOException {
        String shoppingCollectionUrl = buildUrl("ShoppingListCollection");

        JsonFactory jsonFactory = new JacksonFactory();

        List<Map<String, Object>> result = null;
        if (enableExternal) {
            LOG.info("-----Going to access:" + shoppingCollectionUrl);
            String jsonResponse = fetchFromUrl(shoppingCollectionUrl);
            LOG.info("-----Got all lists:" + jsonResponse);
            result = jsonFactory.fromInputStream(new StringInputStream(jsonResponse), null);
        } else {
            result = jsonFactory.fromInputStream(
                    ExternalServiceUtil.class.getResourceAsStream("/com/rightcode/shoppinglist/glass/testing/external_allLists_demo.json"), null);
        }
        return result;
    }

    private static Map<String, Object> getShoppingList(String shoppingListId) throws ClientProtocolException,
            IOException {
        String shoppingListUrl = buildUrl("ShoppingList/" + shoppingListId);
        JsonFactory jsonFactory = new JacksonFactory();

        Map<String, Object> result = null;
        if (enableExternal) {
            LOG.info("-----Going to access:" + shoppingListUrl);
            String jsonResponse = fetchFromUrl(shoppingListUrl);
            LOG.info("-----Got list:" + jsonResponse);
            result = jsonFactory.fromInputStream(new StringInputStream(jsonResponse), null);
        } else {
            result = (Map<String, Object>) jsonFactory.fromInputStream(
                    ExternalServiceUtil.class.getResourceAsStream("/com/rightcode/shoppinglist/glass/testing/external_allLists_demo_list3.json"), null);
                    
            //result = jsonFactory.fromInputStream(new StringInputStream(shoppingListStr), null);
        }
        return result;
    }

    private static Map<String, List<Map<String, Object>>> convertList(Map<String, Object> dataFromExternal) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productLines = (List<Map<String, Object>>) dataFromExternal
                .get(Constants.EXTERNAL_MSG_TAG_LINES);

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        if (productLines != null) {
            for (int i = 0; i < productLines.size(); i++) {
                Map<String, Object> productLine = productLines.get(i);
                Map<String, Object> product = null;
                if (Constants.EXTERNAL_MSG_ITEM_TYPE_PRODUCT.equals(productLine.get(Constants.EXTERNAL_MSG_TAG_TYPE))) {
                    product = convertProduct(productLine);
                } else if (Constants.EXTERNAL_MSG_ITEM_TYPE_FREETEXT.equals(productLine
                        .get(Constants.EXTERNAL_MSG_TAG_TYPE))) {
                    product = convertFreeText(productLine);
                } else {
                    LOG.info("-----Found a non product item[" + productLine.get(Constants.EXTERNAL_MSG_TAG_ID)
                            + "] Type[" + productLine.get(Constants.EXTERNAL_MSG_TAG_TYPE) + "]");
                }
                if (product != null) {

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

    /**
     * @param productFromExternal
     * @return null if can't convert the product
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertFreeText(Map<String, Object> productFromExternal) {
        Map<String, Object> product = new HashMap<String, Object>();
        // As we don't have the spec of external response, so add this try catch
        // block to avoid the whole parsing fail by one unexpected item
        try {
            product.put(Constants.ITEM_COL_PRD_ID, productFromExternal.get(Constants.EXTERNAL_MSG_TAG_ID));
            product.put(Constants.ITEM_COL_PRDNAME,productFromExternal.get(Constants.EXTERNAL_MSG_TAG_VALUE));

            product.put(Constants.ITEM_COL_IMGURL, Constants.DEFAULT_IMG);

            Object quantityValue = productFromExternal.get(Constants.EXTERNAL_MSG_TAG_QUANTITY);
            if(quantityValue != null)
                product.put(Constants.ITEM_COL_QUANTITY, quantityValue);
            else{
                product.put(Constants.ITEM_COL_QUANTITY, "");
            }
            product.put(Constants.ITEM_COL_PRICE,"");

            product.put(Constants.ITEM_COL_CATEGORY, Constants.DEFAULT_CATEGORY);// Eric.TODO
                                                                                 // Remove
                                                                                 // catetory
                                                                                 // handline
                                                                                 // later
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error occur while covert freetext from external response", e);
            return null;
        }
        return product;
    }

    /**
     * @param productFromExternal
     * @return null if can't convert the product
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertProduct(Map<String, Object> productFromExternal) {
        Map<String, Object> product = new HashMap<String, Object>();
        // As we don't have the spec of external response, so add this try catch
        // block to avoid the whole parsing fail by one unexpected item
        try {
            product.put(Constants.ITEM_COL_PRD_ID, productFromExternal.get(Constants.EXTERNAL_MSG_TAG_ID));
            product.put(Constants.ITEM_COL_PRDNAME,
                    extractDescription((Map<String, Object>) productFromExternal.get(Constants.EXTERNAL_MSG_TAG_ITEM)));

            product.put(Constants.ITEM_COL_IMGURL,
                    extractImg((Map<String, Object>) productFromExternal.get(Constants.EXTERNAL_MSG_TAG_ITEM)));

            Object quantityValue = productFromExternal.get(Constants.EXTERNAL_MSG_TAG_QUANTITY);
            // A special handling for demo
            if (quantityValue != null && quantityValue instanceof BigDecimal) {
                product.put(Constants.ITEM_COL_QUANTITY, ((BigDecimal) quantityValue).intValue());
            } else {
                product.put(Constants.ITEM_COL_QUANTITY, quantityValue);
            }

            product.put(Constants.ITEM_COL_PRICE,
                    extractPrice((Map<String, Object>) productFromExternal.get(Constants.EXTERNAL_MSG_TAG_ITEM)));

            product.put(Constants.ITEM_COL_PROMO,
                    extractPromotion((Map<String, Object>) productFromExternal.get(Constants.EXTERNAL_MSG_TAG_ITEM)));

            product.put(Constants.ITEM_COL_CATEGORY, Constants.DEFAULT_CATEGORY);// Eric.TODO
                                                                                 // Remove
                                                                                 // catetory
                                                                                 // handline
                                                                                 // later
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error occur while covert product from external response", e);
            return null;
        }
        return product;
    }

    @SuppressWarnings("unchecked")
    private static String extractImg(Map<String, Object> itemDetailsMap) {
        Map<String, String> imgObj = (Map<String, String>) itemDetailsMap.get(Constants.EXTERNAL_MSG_TAG_IMG);
        String imgUrl = Constants.DEFAULT_IMG;
        if (imgObj != null) {
            String url = imgObj.get(Constants.EXTERNAL_MSG_TAG_URL);
            if (url != null && !url.isEmpty()) {
                imgUrl = url;
            }
        }
        return imgUrl;
    }

    @SuppressWarnings("unchecked")
    private static String extractPrice(Map<String, Object> itemDetailsMap) {
        String prefix = "$"; // Use $ as deafult
        Map<String, String> priceNode = (Map<String, String>) itemDetailsMap.get(Constants.EXTERNAL_MSG_TAG_PRICE);
        if (priceNode != null) {
            String currency = priceNode.get(Constants.EXTERNAL_MSG_TAG_CURRENCY);
            if (currency != null && currency.equals("GBP")) {
                prefix = "\u20A4";
            }
            return prefix
                    + " "
                    + ((Map<String, String>) itemDetailsMap.get(Constants.EXTERNAL_MSG_TAG_PRICE))
                            .get(Constants.EXTERNAL_MSG_TAG_VALUE);
        } else {
            return "";
        }

    }

    @SuppressWarnings("unchecked")
    private static String extractDescription(Map<String, Object> itemDetailsMap) {
        List<Map<String, String>> descs = (List<Map<String, String>>) itemDetailsMap
                .get(Constants.EXTERNAL_MSG_TAG_DESC);
        String longName = null;
        String shortName = null;
        if (descs != null) {
            for (int i = 0; i < descs.size(); i++) {
                Map<String, String> desc = descs.get(i);
                if ("Long".equals(desc.get(Constants.EXTERNAL_MSG_TAG_TYPECODE))) {
                    longName = desc.get(Constants.EXTERNAL_MSG_TAG_VALUE);
                } else if ("Short".equals(desc.get(Constants.EXTERNAL_MSG_TAG_TYPECODE))) {
                    shortName = desc.get(Constants.EXTERNAL_MSG_TAG_VALUE);
                }
            }
        }
        if (shortName != null) {
            return shortName;
        } else if (longName != null) {
            return longName;
        } else {
            return "UnKnown Product";
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractPromotion(Map<String, Object> itemDetailsMap) {
        List<Map<String, Object>> proms = (List<Map<String, Object>>) itemDetailsMap
                .get(Constants.EXTERNAL_MSG_TAG_PROMO);

        String longName = null;
        String shortName = null;

        if (proms != null && proms.size() > 0) {
            Map<String, Object> prom = (Map<String, Object>) proms.get(0);
            List<Map<String, String>> descs = (List<Map<String, String>>) prom.get(Constants.EXTERNAL_MSG_TAG_DESC);

            for (int i = 0; i < descs.size(); i++) {
                Map<String, String> desc = descs.get(i);
                if ("Long".equals(desc.get(Constants.EXTERNAL_MSG_TAG_TYPECODE))) {
                    longName = desc.get(Constants.EXTERNAL_MSG_TAG_VALUE);
                } else if ("Short".equals(desc.get(Constants.EXTERNAL_MSG_TAG_TYPECODE))) {
                    shortName = desc.get(Constants.EXTERNAL_MSG_TAG_VALUE);
                }
            }
        }
        if (shortName != null) {
            return shortName;
        } else if (longName != null) {
            return longName;
        } else {
            return null;
        }
    }

    final static class FeatchTask implements Callable<Object[]> {

        @Override
        public Object[] call() throws Exception {
            Map<String, Map<String, List<Map<String, Object>>>> shoppingListData = new HashMap<String, Map<String, List<Map<String, Object>>>>();

            List<Map<String, Object>> allLists = getAllShoppingList();
            Map<String, String> listNames = new HashMap<String, String>();

            for (int i = 0; i < allLists.size(); i++) {
                String id = (String) allLists.get(i).get(Constants.EXTERNAL_MSG_TAG_ID);
                listNames.put(id, (String) allLists.get(i).get(Constants.EXTERNAL_MSG_TAG_NAME));

                shoppingListData.put(id, convertList(getShoppingList(id)));
            }
            Object[] result = new Object[2];
            result[0] = shoppingListData;
            result[1] = listNames;
            return result;
        }
    }

    public static void main(String[] args) throws IOException {
        JsonFactory jsonFactory = new JacksonFactory();
        System.out.println(jsonFactory.toPrettyString(ExternalServiceUtil.getConvertedData()));
        
    }
}
