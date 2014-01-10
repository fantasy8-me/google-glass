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
import com.rightcode.shoppinglist.glass.util.MirrorUtil;

public class DemoShoppingListProvider implements ShoppingListProvider {

    /**
     * First Level is user, Second Level Key is shopping list, Third Level Key is category
     */
    private Map<String, Map<String, Map<String, List<Map<String, Object>>>>> productData = new HashMap<>();

    private Map<String, Map<String, String>> shoppingListNameMap = new HashMap<>();

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
        initData(userId);
        Iterator<String> iter = productData.get(userId).keySet().iterator();
        while (iter.hasNext()) {
            String shoppingListId = (String) iter.next();
            mergePurchaseStatus(userId, shoppingListId);
        }
        return productData.get(userId);
    }

    @Override
    public List<Map<String, Object>> getShoppingList(String userId, String shoppingListId, String category) {
        // In GAE, any data cached in memory will be destroyed after certain period, we need to load the data from db if this happen
        initData(userId);
        // Use the dummy user id which is defined in productData.json
        mergePurchaseStatus(userId, shoppingListId);
        // We don't support category any more, but will keep the logic for a while
        return productData.get(userId).get(shoppingListId).get(Constants.DEFAULT_CATEGORY);
    }

    @Override
    public Map<String, List<Map<String, Object>>> getShoppingList(String userId, String shoppingListId) {
        initData(userId);
        mergePurchaseStatus(userId, shoppingListId);
        Map<String, List<Map<String, Object>>> shoppingList = productData.get(userId).get(shoppingListId);

        return shoppingList;
    }

    @Override
    public void markProduct(String userId, String shoppingListId, String productId, String cardId) {
        initData(userId);
        cardDao.markPurchaseStatus(userId, cardId, true);
    }

    @Override
    public void unMarkProduct(String userId, String shoppingListId, String productId, String cardId) {
        initData(userId);
        cardDao.markPurchaseStatus(userId, cardId, false);
    }

    @Override
    public Map<String, Object> getProductData(String userId, String shoppingListId, String productId) {
        initData(userId);
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
        initData(userId);
        return shoppingListNameMap.get(userId).get(shoppingListId);
    }

    @SuppressWarnings("unchecked")
    public int fetchShoppingLists(String userId) {
        JsonFactory jsonFactory = new JacksonFactory();
        ServiceCacheDao scDao = ServiceCacheDao.getInstance();
        try {
            Object[] result = ExternalServiceUtil.getConvertedData();
            if (result != null) {
                productData.put(userId, (Map<String, Map<String, List<Map<String, Object>>>>) result[0]);
                shoppingListNameMap.put(userId, (Map<String, String>) result[1]);
                LOG.info("-----Going to update service type:" + Constants.SERVICE_TYPE_EXTERNAL);
                scDao.storeServiceCache(userId, Constants.SERVICE_TYPE_EXTERNAL, jsonFactory.toString(productData.get(userId)),
                        jsonFactory.toString(shoppingListNameMap.get(userId)));
                LOG.info("*****Init app with external data successfully");
                return Constants.INIT_APP_RESULT_SUCCESS_WITH_EXTERNAL;
            } else {
                initFromLocalDummyData(userId, true);
                LOG.info("**********Switch to productData.json");
                return Constants.INIT_APP_RESULT_SUCCESS_WITH_DUMMY;
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Constants.INIT_APP_RESULT_FAIL;
        }

    }

    @Override
    public boolean refreshData(String userId) {
        initData(userId);

        JsonFactory jsonFactory = new JacksonFactory();
        ServiceCacheDao scDao = ServiceCacheDao.getInstance();
        try {
            Object[] result = ExternalServiceUtil.getConvertedData();
//             Object[] result = new Object[2]; //Local testing
//             Map<String, Map<String, List<Map<String, Object>>>> productDataOfUser = jsonFactory.fromInputStream(
//             DemoShoppingListProvider.class.getResourceAsStream("/com/rightcode/shoppinglist/glass/testing/productData_refresh.json"),
//             null);
//            
//             Map<String, String> nameMap = createDummyListNameMapForRefresh();
//             result[0] = productDataOfUser;
//             result[1] = nameMap;

            if (result != null) {
                Map<String, Map<String, List<Map<String, Object>>>> newData = (Map<String, Map<String, List<Map<String, Object>>>>) result[0];
                Map<String, Map<String, List<Map<String, Object>>>> oldData = productData.get(userId);
                if (oldData == null) {
                    LOG.warning("We can't refresh data for you as you didn't initial the app for user:" + userId);
                    return false;
                }
                Map<String, String> newShoppingListNameMap = (Map<String, String>) result[1];
                List<String> shoppingListCover = cardDao.getCardsByType(userId, Constants.CARD_TYPE_LIST_COVER, null);
                if (shoppingListCover.size() != 0) {
                    if (updateTimeline(userId, oldData, newData, newShoppingListNameMap)) {
                        productData.put(userId, newData);
                        shoppingListNameMap.put(userId, newShoppingListNameMap);

                        scDao.storeServiceCache(userId, Constants.SERVICE_TYPE_EXTERNAL, jsonFactory.toString(newData),
                                jsonFactory.toString(newShoppingListNameMap));
                        LOG.info("*****Refresh data for user[" + userId + "] successfully");
                        return true;
                    }
                    LOG.warning("You click the restart but no shopping list cover, which is invalid");
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Encounter error when refresh data", e);
        }
        return false;
    }

    private void initData(String userId) {
        JsonFactory jsonFactory = new JacksonFactory();
        ServiceCacheDao scDao = ServiceCacheDao.getInstance();
        Map<String, Map<String, List<Map<String, Object>>>> productDataForCurrentUser = this.productData.get(userId);
        if (productDataForCurrentUser == null) {
            ServiceCache sc = scDao.getRecord(userId);
            if (sc != null) {
                if (Constants.SERVICE_TYPE_DUMMY.equals(sc.getCurrentService())) {
                    initFromLocalDummyData(userId, false);
                    LOG.info("*****Restore data for local file successfully");
                } else {
                    Map<String, Map<String, List<Map<String, Object>>>> productDataOfUser = null;
                    try {
                        productDataOfUser = jsonFactory.fromString(sc.getCachedListData(), null);
                        productData.put(userId, productDataOfUser);
                        shoppingListNameMap.put(userId, (Map<String, String>) jsonFactory.fromString(sc.getCachedListNames(), null));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Can not restore data from db", e);
                        throw new RuntimeException(e); // Re-throw the excepton to break the flow
                    }
                    LOG.info("*****Restore data for external service successfully");
                }
            } else {
                LOG.severe("Can not restore data from db, the shopping list is not fetched before");
            }
        }
    }

    private void initFromLocalDummyData(String userId, boolean updateDb) {
        JsonFactory jsonFactory = new JacksonFactory();
        try {

            Map<String, Map<String, List<Map<String, Object>>>> productDataOfUser = jsonFactory.fromInputStream(
                    DemoShoppingListProvider.class.getResourceAsStream("/productData.json"), null);
            productData.put(userId, productDataOfUser);

            Map<String, String> nameMap = createDummyListNameMap();

            shoppingListNameMap.put(userId, nameMap);

            if (updateDb) {
                LOG.info("-----Going to update service type:" + Constants.SERVICE_TYPE_DUMMY);
                ServiceCacheDao.getInstance().storeServiceCache(userId, Constants.SERVICE_TYPE_DUMMY, "", "");
            }
        } catch (IOException e) {
            LOG.severe("Can not init product data from productData.json:" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean updateTimeline(String userId, Map<String, Map<String, List<Map<String, Object>>>> oldData,
            Map<String, Map<String, List<Map<String, Object>>>> newData, Map<String, String> newShoppingListNameMap) throws IOException {
        Iterator<String> listIds = newData.keySet().iterator();
        while (listIds.hasNext()) {
            String shoppingListId = (String) listIds.next();

            if (oldData.containsKey(shoppingListId)) {
                List<Map<String, Object>> newProductList = newData.get(shoppingListId).get(Constants.DEFAULT_CATEGORY);
                List<Map<String, Object>> oldProductlist = oldData.get(shoppingListId).get(Constants.DEFAULT_CATEGORY);
                String shoppingListCardId = cardDao.getCardIdByRef(userId, shoppingListId, Constants.CARD_TYPE_SHOPPINGLIST);
                if (!shoppingListCardId.equals("")) {
                    List<String> productCards = cardDao.getCardsByType(userId, Constants.CARD_TYPE_PRODUCT, shoppingListCardId);
                    // Currently, we don't have an perfect way to determine whether a list is 'READY',"IN PROGRESS" or "DONE", So a turn
                    // around is to use number of product cards to determine.
                    if (productCards.size() > 0) {
                        LOG.info("-----Going to check list:" + shoppingListId + " for any update");
//                        MirrorUtil.updateShoppingListCardContent(userId, shoppingListCardId, newData.get(shoppingListId),
//                                newShoppingListNameMap.get(shoppingListId), Constants.SHOPPING_LIST_STATUS_IN_PROGRESS);                        
                        updateTimeLineForSingleList(userId, oldProductlist, newProductList, shoppingListCardId);
                    }
                }
            } else {
                String bundleId = cardDao.getBundleIdFromListCoverCard(userId);
                MirrorUtil.createShoppingListCard(userId, newData.get(shoppingListId), newShoppingListNameMap.get(shoppingListId),
                        shoppingListId, bundleId);
                LOG.info("-----Just sync a new list to glass");
            }
        }
        return true;
    }

    private void updateTimeLineForSingleList(String userId, List<Map<String, Object>> oldProductlist,
            List<Map<String, Object>> newProductList, String shoppingListCardId) throws IOException {
        for (int i = 0; i < newProductList.size(); i++) {
            Map<String, Object> newProduct = newProductList.get(i);
            if (!containProcut(newProduct, oldProductlist)) {
                Map<String, Object> viewBean = new HashMap<>(newProductList.get(i));
                viewBean.put(Constants.VELOCITY_PARM_ITEMS_IN_CATEGORY, newProductList);
                MirrorUtil.createItemInfoCard(userId, viewBean, shoppingListCardId);
                LOG.info("-----Just add a new card:" + viewBean.get(Constants.ITEM_COL_PRDNAME)
                        + " to your timeline for list which card id is:" + shoppingListCardId);
            } 
            
//            else {
//                String cardId = cardDao.getCardIdByRef(userId, (String) newProduct.get(Constants.ITEM_COL_PRD_ID),
//                        Constants.CARD_TYPE_PRODUCT);
//                MirrorUtil.updateProductCardContent(userId, newProduct, cardId, newProductList);
//                LOG.info("newProduct:"+newProduct);
//            }
        }
    }

    private boolean containProcut(Map<String, Object> newProduct, List<Map<String, Object>> oldProductlist) {
        for (int i = 0; i < oldProductlist.size(); i++) {
            String newProductId = (String) newProduct.get(Constants.ITEM_COL_PRD_ID);
            String oldProductId = (String) oldProductlist.get(i).get(Constants.ITEM_COL_PRD_ID);
            if (oldProductId.equals(newProductId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The product purchase status is store in our Card table, we need to merge the status to the cached shopping list
     * 
     * @param userId
     */
    private void mergePurchaseStatus(String userId, String shoppingListId) {
        LOG.info("userId:" + userId + " shoppingListId:" + shoppingListId + " productData is null:" + (productData == null));
        Map<String, List<Map<String, Object>>> shoppingList = productData.get(userId).get(shoppingListId);

        Iterator<String> iter = shoppingList.keySet().iterator();
        // As we can tell which shopping list a product card belongs to, so we have to get purchase status for all cards in all shopping
        // list Two long term soltion
        // 1. Enhance the app db to maintain the relationship between shopping list and product card
        // 2. Enhance the criteria, use something like in (cardId1, cardId2,cardId3 ...) to build up a efficient sql
        // 3. Instead of getting all the status in one sql, execute a sql get the the status for one card only(but we have to make many sql
        // call by using this approach)
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

    /**
     * Method used for local testing with dummy data
     * 
     * @return
     */
    private Map<String, String> createDummyListNameMap() {
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("b6ce7152-f24d-4bab-82ed-a2ad01605654", "Sunday's List");
        nameMap.put("48adac20-19e6-445c-a157-a2ad01615d6a", "Eric's Request");
        nameMap.put("d892571e-3647-45bc-bb64-a2ad0160fc11", "Crucial For Mom");
        return nameMap;
    }

    /**For local testing only
     * @return
     */
    private Map<String, String> createDummyListNameMapForRefresh() {
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("b6ce7152-f24d-4bab-82ed-a2ad01605654", "Sunday's List_Refresh");
        nameMap.put("48adac20-19e6-445c-a157-a2ad01615d6a", "Eric's Request");
        nameMap.put("d892571e-3647-45bc-bb64-a2ad0160fc11", "Crucial For Mom");
        nameMap.put("local-shoppinglist-2", "local-shoppinglist-2");
        return nameMap;
    }
    
    public static void main(String[] args) {
        JsonFactory jsonFactory = new JacksonFactory();
        Object data = null;
        try {
            // Shopping list will be cached when initialize
            // DemoShoppingListProvider
            data = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData_real.json"), null);
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
