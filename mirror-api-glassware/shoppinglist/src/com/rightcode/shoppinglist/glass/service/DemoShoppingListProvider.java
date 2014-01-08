package com.rightcode.shoppinglist.glass.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.dao.CardDao;
import com.rightcode.shoppinglist.glass.dao.ServiceCacheDao;
import com.rightcode.shoppinglist.glass.model.ServiceCache;
import com.rightcode.shoppinglist.glass.util.ExternalServiceUtil;

public class DemoShoppingListProvider implements ShoppingListProvider {

    /**
     * First Level is user, Second Level Key is shopping list, Third Level Key is category
     */
    private Map<String,Map<String, Map<String, List<Map<String, Object>>>>> productData = new HashMap<>();
    
    private Map<String, Map<String,String>> shoppingListNameMap = new HashMap<>();

    private static DemoShoppingListProvider demoShoppingListProvider = null;

    private static final Logger LOG = Logger.getLogger(DemoShoppingListProvider.class.getSimpleName());

    private CardDao cardDao = null;

    

    private DemoShoppingListProvider() {
        cardDao = CardDao.getInstance();
    };

    public synchronized static ShoppingListProvider getInstance() {
        if (demoShoppingListProvider == null) {
            demoShoppingListProvider = new DemoShoppingListProvider();
        }
        return demoShoppingListProvider;
    }

    @Override
    public Map<String, Map<String, List<Map<String, Object>>>> getAllShoppingLists(String userId) {
        refreshData(userId,null);
        Iterator<String> iter = productData.get(userId).keySet().iterator();
        while (iter.hasNext()) {
            String shoppingListId = (String) iter.next();
            mergePurchaseStatus(userId, shoppingListId);
        }
        return productData.get(userId);
    }

    @Override
    public List<Map<String, Object>> getShoppingList(String userId, String shoppingListId, String category) {
        refreshData(userId,null);
        // Use the dummy user id which is defined in productData.json
        mergePurchaseStatus(userId, shoppingListId);
        //We don't support category any more, but will keep the logic for a while
        return productData.get(userId).get(shoppingListId).get(Constants.DEFAULT_CATEGORY); 
    }

    @Override
    public Map<String, List<Map<String, Object>>> getShoppingList(String userId, String shoppingListId) {
        refreshData(userId,null);
        mergePurchaseStatus(userId, shoppingListId);
        Map<String, List<Map<String, Object>>> shoppingList = productData.get(userId).get(shoppingListId);

        return shoppingList;
    }

    @Override
    public void markProduct(String userId, String shoppingListId, String productId, String cardId) {
        refreshData(userId,null);
        cardDao.markPurchaseStatus(userId, cardId, true);
    }

    @Override
    public void unMarkProduct(String userId, String shoppingListId, String productId, String cardId) {
        refreshData(userId,null);
        cardDao.markPurchaseStatus(userId, cardId, false);
    }

    @Override
    public Map<String, Object> getProductData(String userId, String shoppingListId, String productId) {
        refreshData(userId,null);
        mergePurchaseStatus(userId, shoppingListId);
        Map<String, List<Map<String, Object>>> shoppingList = productData.get(userId).get(shoppingListId);

        Iterator<String> iter = shoppingList.keySet().iterator();

        while (iter.hasNext()) {
            List<Map<String, Object>> subShoppingList = shoppingList.get(iter.next());
            Map<String, Object> prodcutData = null;
            for (int i = 0; i < subShoppingList.size(); i++) {
                prodcutData = subShoppingList.get(i);
                if (productId.equals(prodcutData.get(Constants.ITEM_COL_PRD_ID))) {
                    return prodcutData;
                }
            }
        }
        return null;
    }

    @Override
    public String getShoppingListName(String userId, String shoppingListId) {
        return shoppingListNameMap.get(userId).get(shoppingListId);
    }

    @SuppressWarnings("unchecked")
    public int refreshData(String userId,String serviceType){
        JsonFactory jsonFactory = new JacksonFactory();
        ServiceCacheDao scDao = ServiceCacheDao.getInstance();
        Map<String, Map<String, List<Map<String, Object>>>> productDataForCurrentUser = this.productData.get(userId);
        try {
            if (serviceType != null) {
                if (Constants.SERVICE_TYPE_DUMMY.equals(serviceType)) {
                    initFromLocalDummyData(userId, true);
                    LOG.info("*****Init app with local data successfully");
                    return Constants.INIT_APP_RESULT_SUCCESS_WITH_DUMMY;
                } else {
                    Object[] result = ExternalServiceUtil.getConvertedData();
                    if (result != null) {
                        productData.put(userId, (Map<String, Map<String, List<Map<String, Object>>>>) result[0]);
                        shoppingListNameMap.put(userId, (Map<String, String>) result[1]);
                        LOG.info("-----Going to update service type:" + Constants.SERVICE_TYPE_EXTERNAL);
                        scDao.storeServiceCache(userId,Constants.SERVICE_TYPE_EXTERNAL, jsonFactory.toString(productData.get(userId)),
                                jsonFactory.toString(shoppingListNameMap.get(userId)));
                        LOG.info("*****Init app with external data successfully");
                        return Constants.INIT_APP_RESULT_SUCCESS_WITH_EXTERNAL;
                    } else {
                        initFromLocalDummyData(userId,true);
                        LOG.info("**********Switch to productData.json");
                        return Constants.INIT_APP_RESULT_SUCCESS_WITH_DUMMY;
                    }
                }
            } else {
                if (productDataForCurrentUser == null) {
                    ServiceCache sc = scDao.getRecord(userId);
                    if (Constants.SERVICE_TYPE_DUMMY.equals(sc.getCurrentService())) {
                        initFromLocalDummyData(userId, false);
                        LOG.info("*****Restore data for local file successfully");
                        return Constants.INIT_APP_RESULT_SUCCESS_WITH_DUMMY;
                    } else {
                        Map<String, Map<String, List<Map<String, Object>>>> productDataOfUser = jsonFactory.fromString(sc.getCachedListData(), null);
                        productData.put(userId, productDataOfUser);
                        shoppingListNameMap.put(userId, (Map<String,String>)jsonFactory.fromString(sc.getCachedListNames(), null));
                        LOG.info("*****Restore data for external service successfully");
                        return Constants.INIT_APP_RESULT_SUCCESS_WITH_DUMMY;
                    }
                }else{
                    return Constants.INIT_APP_RESULT_SUCCESS;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(),e);
            return Constants.INIT_APP_RESULT_FAIL;
        }

    }

    private void initFromLocalDummyData(String userId, boolean updateDb) {
        JsonFactory jsonFactory = new JacksonFactory();
        try {
            
            Map<String, Map<String, List<Map<String, Object>>>> productDataOfUser = jsonFactory.fromInputStream(
                    DemoShoppingListProvider.class.getResourceAsStream("/productData.json"), null);
            productData.put(userId, productDataOfUser);
            
            Map<String,String> nameMap = new HashMap<String,String>();
            nameMap.put("local-shoppinglist-1", "Food for Jason");
            nameMap.put("local-shoppinglist-2", "Food for Mom");
            shoppingListNameMap.put(userId, nameMap);
            
            if (updateDb){
                LOG.info("-----Going to update service type:" + Constants.SERVICE_TYPE_DUMMY);
                ServiceCacheDao.getInstance().storeServiceCache(userId,Constants.SERVICE_TYPE_DUMMY, "", "");
            }
        } catch (IOException e) {
            LOG.severe("Can not init product data from productData.json:" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * The product purchase status is store in our Card table, we need to merge
     * the status to the cached shopping list
     * 
     * @param userId
     */
    private void mergePurchaseStatus(String userId, String shoppingListId) {
        LOG.info("userId:" + userId + " shoppingListId:" + shoppingListId + " productData is null:"+ (productData == null));
        Map<String, List<Map<String, Object>>> shoppingList = productData.get(userId).get(shoppingListId);

        Iterator<String> iter = shoppingList.keySet().iterator();
        // As we can tell which shopping list a product card belongs to, so we
        // have to get purchase status for all cards in all shopping list
        // Two long term soltion
        // 1. Enhance the app db to maintain the relationship between shopping
        // list and product card
        // 2. Enhance the criteria, use something like in (cardId1, cardId2,
        // cardId3 ...) to build up a efficient sql
        // 3. Instead of getting all the status in one sql, execute a sql get
        // the the status for one card only(but we have to make many sql call by
        // using this approach)
        Map<String, Boolean> purchaseStatus = cardDao.getPurchaseStatus(userId);

        while (iter.hasNext()) {
            String categoryId = (String) iter.next();
            List<Map<String, Object>> subShoppingList = shoppingList.get(categoryId);
            enrichPurchaseStatus(purchaseStatus, subShoppingList);
        }
    }

    private void enrichPurchaseStatus(Map<String, Boolean> purchaseStatus, List<Map<String, Object>> shoppingList) {
        for (int i = 0; i < shoppingList.size(); i++) {
            Map<String, Object> product = shoppingList.get(i);
            Boolean purchased = purchaseStatus.get(product.get(Constants.ITEM_COL_PRD_ID));
            // set initial value as false;
            product.put(Constants.ITEM_COL_PURCHASED, purchased == null ? false : purchased);
        }
    }

    public static void main(String[] args) {
        JsonFactory jsonFactory = new JacksonFactory();
        Object data = null;
        try {
            // Shopping list will be cached when initialize
            // DemoShoppingListProvider
            data = jsonFactory.fromInputStream(
                    DemoShoppingListProvider.class.getResourceAsStream("/productData_real.json"), null);
            System.out.println("data:" + data);
        } catch (IOException e) {
            LOG.severe("Can not init product data");
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }

        List<Map<String, Object>> productList = new ArrayList<Map<String, Object>>();
        Map<String, Object> productMap1 = new HashMap<String, Object>();
        productMap1.put("prdNum", 1);
        productMap1.put("prdDes", "Fish");
        productMap1.put("Price", 1.234);
        productMap1.put("Purchased", false);
        productList.add(productMap1);

        Map<String, Object> productMap2 = new HashMap<String, Object>();
        productMap2.put("prdNum", 2);
        productMap2.put("prdDes", "Meat");
        productMap2.put("Price", 4.234);
        productMap2.put("Purchased", false);
        productList.add(productMap2);

        try {
            System.out.println(jsonFactory.toString(productList));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
