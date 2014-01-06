package com.rightcode.shoppinglist.glass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;
import com.rightcode.shoppinglist.glass.dao.CardDao;
import com.rightcode.shoppinglist.glass.ref.AuthUtil;
import com.rightcode.shoppinglist.glass.ref.MirrorClient;
import com.rightcode.shoppinglist.glass.service.DemoShoppingListProvider;
import com.rightcode.shoppinglist.glass.service.ExternalShoppingListProvider;
import com.rightcode.shoppinglist.glass.service.ShoppingListProvider;
import com.rightcode.shoppinglist.glass.util.MirrorUtil;
import com.rightcode.shoppinglist.glass.util.ReferenceDataManager;
import com.rightcode.shoppinglist.glass.util.VelocityHelper;

public class AppController {

    private static AppController appController = null;
    private ShoppingListProvider shoppingListProvider = null;
    ReferenceDataManager refDataManager = null;

    private CardDao cardDao = null;
    private String bundleIdSuffix = ""; // Reset this value whenever you are
                                        // going to create bundle card

    private static final Logger LOG = Logger.getLogger(AppController.class.getSimpleName());

    /**
     * Call back used by clean up batch call. Eric.TODO, can be further enhanced
     * 
     * @author me
     * 
     */
    private final static class CleanUpBatchCallback extends JsonBatchCallback<Void> {

        private String cardId = null;
        private CardDao cardDao = null;

        /**
         * Used to record down the result.
         * 
         */
        private static boolean allSuccess = true;

        @Override
        public void onSuccess(Void t, HttpHeaders responseHeaders) throws IOException {
            cardDao.deleteCard(cardId);
        }

        @Override
        public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
            allSuccess = false;
            LOG.severe("Fail to delete card:" + cardId + " Reason:" + e.getMessage());
        }

        public CleanUpBatchCallback setId(String cardId) {
            this.cardId = cardId;
            return this;
        }

        public CleanUpBatchCallback setDao(CardDao cardDao) {
            this.cardDao = cardDao;
            return this;
        }

        public static boolean isAllSuccess() {
            return allSuccess;
        }

        /**
         * Must be call after each clean up
         * 
         * @param resetValue
         */
        public static void resetAllSuccess() {
            allSuccess = true;
        }

    }

    private AppController() {
        // Eric.TODO, move following logic to a serviceProiderFactory later
//        shoppingListProvider = DemoShoppingListProvider.getInstance();
        cardDao = CardDao.getInstance();
        refDataManager = ReferenceDataManager.getInstance();
        VelocityHelper.initVelocity();

    };

    public synchronized static AppController getInstance() {
        if (appController == null) {
            // This log is used to indicate that that our app is reload.
            LOG.info("*--------------------------AppController inited:" + System.currentTimeMillis());
            appController = new AppController();
        }
        return appController;

    }

    public void initApp(String userId) throws IOException {
        Credential credential;
        credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        shoppingListProvider = DemoShoppingListProvider.getInstance();
        
        createInitialCard(mirrorClient, userId);
    }

    public void actionStartShopping(String userId, String shoppingListCardId) throws IOException {
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        bundleIdSuffix = String.valueOf(System.currentTimeMillis());

        List<String> bundleCovers = cardDao.getCardsByType(userId, Constants.CARD_TYPE_CATEGORY_COVER,
                shoppingListCardId);
        if (bundleCovers != null && bundleCovers.size() > 0) {
            for (int i = 0; i < bundleCovers.size(); i++) {
                MirrorUtil.touchCard(mirrorClient, bundleCovers.get(i));
            }
            LOG.info("Starting shopping done, all cards are move to front of your timeline");
        } else {
            String shoppingListId = cardDao.getCardRefById(userId, shoppingListCardId);
            updateShoppingListCard(mirrorClient, userId, shoppingListId, shoppingListCardId,
                    Constants.SHOPPING_LIST_STATUS_IN_PROGRESS, Constants.SHOPPING_LIST_STATUS_READY);

            Map<String, List<Map<String, Object>>> shoppingList = shoppingListProvider.getShoppingList(userId,
                    shoppingListId);
            Iterator<String> iter = shoppingList.keySet().iterator();
            while (iter.hasNext()) {
                String category = (String) iter.next();
                List<Map<String, Object>> productList = shoppingList.get(category);

                String bundleId = getBundleId(shoppingListId + "_" + category);

                Map<String, Object> bundleConverViewbean = buildBundleConverViewBean(category, 0, productList.size(),
                        shoppingListProvider.getShoppingListName(userId, shoppingListId));
                createItemConverCard(mirrorClient, bundleConverViewbean, bundleId, userId, shoppingListCardId);

                for (Iterator<Map<String, Object>> iterator = productList.iterator(); iterator.hasNext();) {
                    Map<String, Object> viewbean = new HashMap<String, Object>(iterator.next());
                    viewbean.put(Constants.VELOCITY_PARM_COMPLETED_IN_CATEGORY, 0);
                    viewbean.put(Constants.VELOCITY_PARM_SUBTOTOAL, productList.size());
                    viewbean.put(Constants.VELOCITY_PARM_ITEMS_IN_CATEGORY, productList);
                    createItemInfoCard(mirrorClient, viewbean, bundleId, userId, shoppingListCardId);
                }
            }
            LOG.info("Starting shopping done, all shopping list cards have ben created");
        }
    }

    public void actionFinishShopping(String userId, String shoppingListCardId) throws IOException {
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        cleanUpAllCards(userId, shoppingListCardId);
        String shoppingListId = cardDao.getCardRefById(userId, shoppingListCardId);
        updateShoppingListCard(mirrorClient, userId, shoppingListId, shoppingListCardId,
                Constants.SHOPPING_LIST_STATUS_DONE, Constants.SHOPPING_LIST_STATUS_IN_PROGRESS);
        LOG.info("-----Finish Shopping Done");
    }

    public void bringICToFront(String userId) throws IOException {
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        List<String> cardIds = cardDao.getCardsByType(userId, Constants.CARD_TYPE_IC, null);
        MirrorUtil.touchCard(mirrorClient, cardIds.get(0));
    }

    public boolean cleanUpAllCards(String userId, String shoppingListCardId) throws IOException {
        List<String> allCards = cardDao.getCardsByType(userId, null, shoppingListCardId);
        return cleanUpCards(userId, allCards);
    }

    public boolean cleanUpCards(String userId, List<String> cards) throws IOException {
        Credential credential = AuthUtil.getCredential(userId);

        BatchRequest batch = MirrorClient.getMirror(null).batch();
        for (int i = 0; i < cards.size(); i++) {
            MirrorClient.getMirror(credential).timeline().delete(cards.get(i))
                    .queue(batch, new CleanUpBatchCallback().setId(cards.get(i)).setDao(cardDao));
        }
        if (cards.size() != 0)
            batch.execute();

        if (CleanUpBatchCallback.isAllSuccess()) {
            LOG.info("Card clean up successfully");
            CleanUpBatchCallback.resetAllSuccess();
            return true;
        } else {
            LOG.severe("Fail to clean up cards");
            CleanUpBatchCallback.resetAllSuccess();
            return false;
        }
    }

    public void adminCleanUpToken() throws IOException {
        List<String> users = AuthUtil.getAllUserIds();
        for (int i = 0; i < users.size(); i++) {
            AuthUtil.clearUserId(users.get(i));
        }
    }

    public void insertCoupon(String userId, String couponContent) throws IOException {
        Credential credential = AuthUtil.getCredential(userId);
        TimelineItem couponItem = new TimelineItem();
        couponItem.setHtml(couponContent);

        Mirror mirrorClient = MirrorClient.getMirror(credential);
        mirrorClient.timeline().insert(couponItem).execute();
        LOG.info("Coupon is created");
    }

    /**
     * Called by Notification Servlet
     * 
     * @param userId
     * @throws IOException
     */
    public void actionStartShoppingListFromIC(String userId, String cardIdOfIC) throws IOException {

        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        bundleIdSuffix = String.valueOf(System.currentTimeMillis());
        
        updateICCard(mirrorClient, userId, cardIdOfIC, false);

        String bundleId = getBundleId("listCover");
        createListCoverCard(mirrorClient, userId, bundleId);
        
        Map<String, Map<String, List<Map<String, Object>>>> shoppingLists = shoppingListProvider
                .getAllShoppingLists(userId);

        Iterator<String> iter = shoppingLists.keySet().iterator();

        while (iter.hasNext()) {
            String shoppingListId = (String) iter.next();
            Map<String, List<Map<String, Object>>> shoppingList = shoppingLists.get(shoppingListId);
            String shoppingListName = shoppingListProvider.getShoppingListName(userId, shoppingListId);

            createShoppingListCard(mirrorClient,
                    buildShoppingListViewBean(shoppingList, shoppingListName, Constants.SHOPPING_LIST_STATUS_READY),
                    userId, shoppingListId, bundleId);
        }
    }

    public void actionRestartFromIC(String userId, String cardIdOfIC) throws IOException {

        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        String[] types = new String[]{Constants.CARD_TYPE_CATEGORY_COVER, Constants.CARD_TYPE_LIST_COVER, Constants.CARD_TYPE_PRODUCT, Constants.CARD_TYPE_SHOPPINGLIST};
        List<String> cardsExceptIC = new ArrayList<String>();
        for (int i = 0; i < types.length; i++) {
            cardsExceptIC.addAll(cardDao.getCardsByType(userId, types[i], null));
        }
        cleanUpCards(userId, cardsExceptIC);
        updateICCard(mirrorClient, userId, cardIdOfIC, true);

    }

    private void createListCoverCard(Mirror mirrorClient, String userId, String bundleId) {
        TimelineItem returnItem = null;
        String html = VelocityHelper.getFinalStr(new HashMap<String, Object>(), "listCover.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setIsBundleCover(true);

        timelineItem.setBundleId(bundleId);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            returnItem = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(returnItem.getId(), userId, Constants.CARD_TYPE_LIST_COVER, bundleId, null);
            LOG.info("Bundle cover card for list has been created:[" + returnItem.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create list conver card");
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private Map<String, Object> buildBundleConverViewBean(String category, int numOfCompleted, int subTotoal,
            String listName) {
        
        Map<String, String> categoryMap = refDataManager.getCategorySetting(category);
        if(categoryMap == null){
            categoryMap = new HashMap<String,String>();
            categoryMap.put("imgUrl", "");
            categoryMap.put("title", "Others");
        }
        Map<String, Object> bundleConverViewbean = new HashMap<String, Object>(categoryMap);
        
        bundleConverViewbean.put(Constants.VELOCITY_PARM_SUBTOTOAL, subTotoal);
        bundleConverViewbean.put(Constants.VELOCITY_PARM_COMPLETED_IN_CATEGORY, numOfCompleted);
        bundleConverViewbean.put(Constants.VELOCICY_PARM_SHOPPING_LIST_NAME, listName);
        return bundleConverViewbean;
    }

    private void createInitialCard(Mirror mirrorClient, String userId) {
        String html = VelocityHelper.getFinalStr(null, "initialCard.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_IC_STARTSHOPPING).setDisplayName(
                Constants.MENU_NAME_IC_STARTSHOPPING));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_IC_STARTSHOPPING)
                .setAction("CUSTOM"));
        menuItemList.add(new MenuItem().setAction("TOGGLE_PINNED"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            TimelineItem item = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_IC, null, null);
            LOG.info("-----Initial Card created:[" + item.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create initial card");
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void createShoppingListCard(Mirror mirrorClient, Map<String, Object> items, String userId,
            String shoppingListId, String bundleId) {
        String html = VelocityHelper.getFinalStr(items, "shoppingList.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setBundleId(bundleId);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_STARTSHOPPING).setDisplayName(
                Constants.MENU_NAME_STARTSHOPPING));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_STARTSHOPPING)
                .setAction("CUSTOM"));
        menuItemList.add(new MenuItem().setAction("TOGGLE_PINNED"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            TimelineItem item = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_SHOPPINGLIST, shoppingListId, null);
            LOG.info("Shopping List card created:[" + item.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create shopping list card, data:" + items);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void createItemInfoCard(Mirror mirrorClient, Map<String, Object> itemData, String bundleId, String userId,
            String shoppingListCardId) {

        String html = VelocityHelper.getFinalStr(itemData, "productInfo.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setBundleId(bundleId);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK).setDisplayName(Constants.MENU_NAME_MARK));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            TimelineItem item = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_PRODUCT,
                    (String) itemData.get(Constants.ITEM_COL_PRD_ID), shoppingListCardId);
            LOG.info("Product card created:[" + item.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create item info card, data:" + itemData);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * For local testing only, used to trigger the mark processing
     * 
     * @param mirrorClient
     * @param userId
     * @param cardId
     */
    public void markItem(Mirror mirrorClient, String userId, String cardId) {
        TimelineItem timelineItem = null;
        try {
            timelineItem = mirrorClient.timeline().get(cardId).execute();
            markOrUnMarkProduct(mirrorClient, userId, timelineItem, true);
        } catch (IOException e) {
            LOG.severe("Error when mark item:" + cardId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * For local testing only, used to trigger the unMark processing
     * 
     * @param mirrorClient
     * @param userId
     * @param cardId
     */
    public void unMarkItem(Mirror mirrorClient, String userId, String cardId) {
        TimelineItem timelineItem = null;
        try {
            timelineItem = mirrorClient.timeline().get(cardId).execute();
            markOrUnMarkProduct(mirrorClient, userId, timelineItem, false);
        } catch (IOException e) {
            LOG.severe("Error when mark item:" + cardId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void markOrUnMarkProduct(Mirror mirrorClient, String userId, TimelineItem timelineItem, boolean isMark) {
        String productCardId = timelineItem.getId();
        String bundleId = timelineItem.getBundleId();

        String productId = cardDao.getCardRefById(userId, productCardId);
        String shoppingListCardId = cardDao.getShoppingListCardByProductCard(userId, productCardId);
        String shoppingListId = cardDao.getCardRefById(userId, shoppingListCardId);

        // Update the model
        if (isMark)
            shoppingListProvider.markProduct(userId, shoppingListId, productId, productCardId);
        else
            shoppingListProvider.unMarkProduct(userId, shoppingListId, productId, productCardId);

        Map<String, Object> viewBean = new HashMap<String, Object>(shoppingListProvider.getProductData(userId,
                shoppingListId, productId));
        List<Map<String, Object>> subShoppingList = shoppingListProvider.getShoppingList(userId, shoppingListId,
                (String) viewBean.get(Constants.ITEM_COL_CATEGORY));
        int[] completedStatus = calculateCompletedStatus(subShoppingList);

        // Create an empty timeline item for patch
        TimelineItem patchTimelineItem = new TimelineItem();

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();
        patchTimelineItem.setMenuItems(menuItemList);

        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        if (isMark) {
            menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_UNMARK).setDisplayName(
                    Constants.MENU_NAME_UNMARK));
            patchTimelineItem.getMenuItems().add(
                    new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_UNMARK).setAction("CUSTOM"));
        } else {
            menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK)
                    .setDisplayName(Constants.MENU_NAME_MARK));
            patchTimelineItem.getMenuItems().add(
                    new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));
        }

        viewBean.put(Constants.VELOCITY_PARM_COMPLETED_IN_CATEGORY, completedStatus[0]);
        viewBean.put(Constants.VELOCITY_PARM_SUBTOTOAL, completedStatus[1]);
        viewBean.put(Constants.VELOCITY_PARM_ITEMS_IN_CATEGORY, subShoppingList);
        String html = VelocityHelper.getFinalStr(viewBean, "productInfo.vm");
        patchTimelineItem.setHtml(html);

        try {
            mirrorClient.timeline().patch(productCardId, patchTimelineItem).execute();
            updateCategoryCoverCard(mirrorClient, userId, shoppingListId, shoppingListCardId, bundleId,
                    (String) viewBean.get(Constants.ITEM_COL_CATEGORY), completedStatus[0], completedStatus[1],
                    shoppingListProvider.getShoppingListName(userId, shoppingListId));
            updateShoppingListCard(mirrorClient, userId, shoppingListId, shoppingListCardId,
                    Constants.SHOPPING_LIST_STATUS_IN_PROGRESS, Constants.SHOPPING_LIST_STATUS_IN_PROGRESS);

            LOG.info("--------Purchase status of item[" + productCardId + "] is updated to:" + isMark);
        } catch (IOException e) {
            LOG.severe("Error when update prodcut purchase status:" + productCardId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * 
     * @param mirrorClient
     * @param userId
     * @param cardId
     */
    private void updateICCard(Mirror mirrorClient, String userId, String cardId, boolean restart) {
        try {
            TimelineItem timelineItem = new TimelineItem();
            List<MenuItem> menuItemList = new ArrayList<MenuItem>();
            timelineItem.setMenuItems(menuItemList);
            List<MenuValue> menuValues = new ArrayList<MenuValue>();
            if(restart){
                menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_IC_STARTSHOPPING).setDisplayName(
                        Constants.MENU_NAME_IC_STARTSHOPPING));
                timelineItem.getMenuItems().add(
                        new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_IC_STARTSHOPPING).setAction("CUSTOM"));
            }else{
                menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_IC_RESTART).setDisplayName(
                        Constants.MENU_NAME_IC_RESTART));
                timelineItem.getMenuItems().add(
                        new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_IC_RESTART).setAction("CUSTOM"));
            }
            mirrorClient.timeline().patch(cardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while update the IC card:" + cardId + " restart:" + restart);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void updateShoppingListCard(Mirror mirrorClient, String userId, String shoppingListId,
            String shoppingListCardId, String status, String preStatus) {
        try {
            String html = VelocityHelper.getFinalStr(buildShoppingListViewBean(userId, shoppingListId, status),
                    "shoppingList.vm");
            TimelineItem timelineItem = new TimelineItem();
            timelineItem.setHtml(html);
            if (!status.equals(preStatus)) {
                List<MenuItem> menuItemList = new ArrayList<MenuItem>();
                timelineItem.setMenuItems(menuItemList);
                if (status.equals(Constants.SHOPPING_LIST_STATUS_IN_PROGRESS)) {
                    List<MenuValue> menuValues = new ArrayList<MenuValue>();
                    menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_FINISHSHOPPING).setDisplayName(
                            Constants.MENU_NAME_FINISHSHOPPING));
                    timelineItem.getMenuItems().add(
                            new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_FINISHSHOPPING)
                                    .setAction("CUSTOM"));
                }
            }
            mirrorClient.timeline().patch(shoppingListCardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while updating the shopping list card for:" + userId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void updateCategoryCoverCard(Mirror mirrorClient, String userId, String shoppingListId,
            String shoppingListCardId, String bundleId, String category, int numOfCompleted, int subTotal,
            String shoppingListName) {
        String cardId = cardDao.getBundleCoverCardId(userId, bundleId, shoppingListCardId);

        Map<String, Object> viewBean = buildBundleConverViewBean(category, numOfCompleted, subTotal, shoppingListName);

        try {

            TimelineItem timelineItem = new TimelineItem();

            String html = VelocityHelper.getFinalStr(viewBean, "bundleConver.vm");
            timelineItem.setHtml(html);

            mirrorClient.timeline().patch(cardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while updating the bundle cover card for:" + userId + " category:" + category
                    + " bundleId:" + bundleId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private TimelineItem createItemConverCard(Mirror mirrorClient, Map<String, Object> itemData, String bundleId,
            String userId, String shoppingListCardId) {

        TimelineItem returnItem = null;
        String html = VelocityHelper.getFinalStr(itemData, "bundleConver.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setIsBundleCover(true);

        timelineItem.setBundleId(bundleId);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            returnItem = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(returnItem.getId(), userId, Constants.CARD_TYPE_CATEGORY_COVER, bundleId,
                    shoppingListCardId);
            LOG.info("Bundle cover card created:[" + returnItem.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create item conver card, data:" + itemData);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        return returnItem;

    }

    private String getBundleId(String coverName) {
        // Add current time stamp to separate the cards created in different
        // time
        return "bundle_" + coverName + "_" + bundleIdSuffix;
    }

    private Map<String, Object> buildShoppingListViewBean(String userId, String shoppingListId, String status) {
        Map<String, List<Map<String, Object>>> shoppingList = shoppingListProvider.getShoppingList(userId,
                shoppingListId);
        String shoppingListName = shoppingListProvider.getShoppingListName(userId, shoppingListId);
        return buildShoppingListViewBean(shoppingList, shoppingListName, status);
    }

    private Map<String, Object> buildShoppingListViewBean(Map<String, List<Map<String, Object>>> shoppingList,
            String shoppingListName, String status) {
        Map<String, Object> viewBean = new HashMap<String, Object>();
        int[] result = calculateCompletedStatus(shoppingList);
        viewBean.put(Constants.VELOCICY_PARM_AllPRODUCTS, shoppingList);
        viewBean.put(Constants.VELOCICY_PARM_CATEGORY_TITLES, refDataManager.getCategoryTitleMap());
        viewBean.put(Constants.VELOCITY_PARM_COMPLETED, result[0]);
        viewBean.put(Constants.VELOCITY_PARM_TOTOAL, result[1]);
        viewBean.put(Constants.VELOCICY_PARM_SHOPPING_LIST_NAME, shoppingListName);
        viewBean.put(Constants.VELOCICY_PARM_SHOPPING_LIST_STATUS, status);
        return viewBean;

    }

    /**
     * @param subShoppingList
     * @return int[] int[0]: completed int[1]: subtotal
     */
    private int[] calculateCompletedStatus(List<Map<String, Object>> subShoppingList) {
        int[] result = new int[] { 0, 0 };

        for (int i = 0; i < subShoppingList.size(); i++) {
            Map<String, Object> product = subShoppingList.get(i);
            boolean isPurchased = (Boolean) product.get(Constants.ITEM_COL_PURCHASED);
            if (isPurchased) {
                result[0]++;
            }
        }
        result[1] = subShoppingList.size();
        return result;
    }

    private int[] calculateCompletedStatus(Map<String, List<Map<String, Object>>> shoppingList) {
        int[] result = new int[] { 0, 0 };

        Iterator<String> iter = shoppingList.keySet().iterator();
        while (iter.hasNext()) {
            String category = (String) iter.next();
            int[] subResult = calculateCompletedStatus(shoppingList.get(category));
            result[0] += subResult[0];
            result[1] += subResult[1];
        }
        return result;
    }

}
