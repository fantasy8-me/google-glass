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
        shoppingListProvider = DemoShoppingListProvider.getInstance();
        cardDao = CardDao.getInstance();
        refDataManager = ReferenceDataManager.getInstance();
        VelocityHelper.initVelocity();

    };

    public synchronized static AppController getInstance() {
        if (appController == null) {
            // This log is used to indicate that that our app is reload.
            LOG.info("*****AppController inited:" + System.currentTimeMillis());
            appController = new AppController();
        }
        return appController;

    }

    public String initApp(String userId) throws IOException {

        if (cardDao.getNumberOfCards(userId) > 0) {
            cleanUpAllCards(userId, null);
        }
        // This is a speical handling for demo service, not supported for other service provider
        // int result = ((DemoShoppingListProvider)shoppingListProvider).initData(userId, serviceType);
        // if(result != Constants.INIT_APP_RESULT_FAIL){
        String msg = null;
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        if (createInitialCard(mirrorClient, userId)) {
            msg = "An intial card was created for you in your glass. Pin it and start shopping by fetch shopping list from our exteranl service provider";
        } else {
            msg = "Fail to create IC card for you, please contact our support team for details";
        }
        // }
        return msg;
    }

    public void actionStartShopping(String userId, String shoppingListCardId) throws IOException {
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        bundleIdSuffix = String.valueOf(System.currentTimeMillis());

        List<String> prodcutCards = cardDao.getCardsByType(userId, Constants.CARD_TYPE_PRODUCT, shoppingListCardId);
        if (prodcutCards != null && prodcutCards.size() > 0) {
            for (int i = 0; i < prodcutCards.size(); i++) {
                MirrorUtil.touchCard(mirrorClient, prodcutCards.get(i));
            }
            LOG.info("[Starting shopping] is clicked again, just moved all card to front of your timeline");
        } else {
            String shoppingListId = cardDao.getCardRefById(userId, shoppingListCardId);
            LOG.info("Starting shopping list:" + shoppingListId + " cardId:" + shoppingListCardId);
            updateShoppingListCard(mirrorClient, userId, shoppingListId, shoppingListCardId, Constants.SHOPPING_LIST_STATUS_IN_PROGRESS,
                    Constants.SHOPPING_LIST_STATUS_READY);

            Map<String, List<Map<String, Object>>> shoppingList = shoppingListProvider.getShoppingList(userId, shoppingListId);
            Iterator<String> iter = shoppingList.keySet().iterator();
            while (iter.hasNext()) {
                String category = (String) iter.next();
                List<Map<String, Object>> productList = shoppingList.get(category);

                // String bundleId = getBundleId(shoppingListId + "_" + category);

                // Map<String, Object> bundleConverViewbean = buildBundleConverViewBean(category, 0, productList.size(),
                // shoppingListProvider.getShoppingListName(userId, shoppingListId));
                // createItemConverCard(mirrorClient, bundleConverViewbean, bundleId, userId, shoppingListCardId);

                for (Iterator<Map<String, Object>> iterator = productList.iterator(); iterator.hasNext();) {
                    Map<String, Object> viewbean = new HashMap<String, Object>(iterator.next());
                    viewbean.put(Constants.VELOCITY_PARM_ITEMS_IN_CATEGORY, productList);
                    MirrorUtil.createItemInfoCard(userId, viewbean, shoppingListCardId);
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
        updateShoppingListCard(mirrorClient, userId, shoppingListId, shoppingListCardId, Constants.SHOPPING_LIST_STATUS_DONE,
                Constants.SHOPPING_LIST_STATUS_IN_PROGRESS);
        LOG.info("-----Finish Shopping Done");
    }

    public void bringICToFront(String userId) throws IOException {
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        List<String> cardIds = cardDao.getCardsByType(userId, Constants.CARD_TYPE_IC, null);
        MirrorUtil.touchCard(mirrorClient, cardIds.get(0));
    }

    /**
     * @param userId
     * @param shoppingListCardId
     *            pass null if clean all shopping list
     * @return
     * @throws IOException
     */
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
     * 
     * 
     * @param userId
     * @throws IOException
     */
    public void actionFetchShoppingLists(String userId, String cardIdOfIC) throws IOException {

        if (shoppingListProvider.fetchShoppingLists(userId) > 0) {
            List<String> listCover = cardDao.getCardsByType(userId, Constants.CARD_TYPE_LIST_COVER, null);
            if (listCover != null && listCover.size() == 0) {
                Credential credential = AuthUtil.getCredential(userId);
                Mirror mirrorClient = MirrorClient.getMirror(credential);
                bundleIdSuffix = String.valueOf(System.currentTimeMillis());

                updateICCard(mirrorClient, userId, cardIdOfIC, false);

                String bundleId = getBundleId("listCover");
                createListCoverCard(mirrorClient, userId, bundleId);

                Map<String, Map<String, List<Map<String, Object>>>> shoppingLists = shoppingListProvider.getAllShoppingLists(userId);

                Iterator<String> iter = shoppingLists.keySet().iterator();

                while (iter.hasNext()) {
                    String shoppingListId = (String) iter.next();
                    Map<String, List<Map<String, Object>>> shoppingList = shoppingLists.get(shoppingListId);
                    String shoppingListName = shoppingListProvider.getShoppingListName(userId, shoppingListId);

                    MirrorUtil.createShoppingListCard(userId, shoppingList, shoppingListName, shoppingListId, bundleId);
                }
            } else {
                LOG.warning("-----You have created the list cover card for this project id, we won't create the them for you again");
            }
        } else {
            LOG.severe("Fail to fetch data from exteranl service, even not able to load data from local dummy file");
        }
    }

    public void actionRestartFromIC(String userId, String cardIdOfIC) throws IOException {

        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);
        String[] types = new String[] { Constants.CARD_TYPE_CATEGORY_COVER, Constants.CARD_TYPE_LIST_COVER, Constants.CARD_TYPE_PRODUCT,
                Constants.CARD_TYPE_SHOPPINGLIST };
        List<String> cardsExceptIC = new ArrayList<String>();
        for (int i = 0; i < types.length; i++) {
            cardsExceptIC.addAll(cardDao.getCardsByType(userId, types[i], null));
        }
        cleanUpCards(userId, cardsExceptIC);
        updateICCard(mirrorClient, userId, cardIdOfIC, true);

    }

    public void actionRefresh(String userId) throws IOException {
        shoppingListProvider.refreshData(userId);
    }

    private void createListCoverCard(Mirror mirrorClient, String userId, String bundleId) throws IOException {
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
            LOG.log(Level.SEVERE, "Error when create list conver card", e);
            throw e;// re-throw the exception to break the flow
        }
    }

    @Deprecated
    private Map<String, Object> buildBundleConverViewBean(String category, int numOfCompleted, int subTotoal, String listName) {

        Map<String, String> categoryMap = refDataManager.getCategorySetting(category);
        if (categoryMap == null) {
            categoryMap = new HashMap<String, String>();
            categoryMap.put("imgUrl", "");
            categoryMap.put("title", "Others");
        }
        Map<String, Object> bundleConverViewbean = new HashMap<String, Object>(categoryMap);

        bundleConverViewbean.put(Constants.VELOCITY_PARM_SUBTOTOAL, subTotoal);
        bundleConverViewbean.put(Constants.VELOCITY_PARM_COMPLETED_IN_CATEGORY, numOfCompleted);
        bundleConverViewbean.put(Constants.VELOCICY_PARM_SHOPPING_LIST_NAME, listName);
        return bundleConverViewbean;
    }

    private boolean createInitialCard(Mirror mirrorClient, String userId) {
        String html = VelocityHelper.getFinalStr(null, "initialCard.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_IC_STARTSHOPPING)
                .setDisplayName(Constants.MENU_NAME_IC_STARTSHOPPING));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_IC_STARTSHOPPING).setAction("CUSTOM"));
        menuItemList.add(new MenuItem().setAction("TOGGLE_PINNED"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            TimelineItem item = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_IC, null, null);
            LOG.info("-----Initial Card created:[" + item.getId() + "] [" + userId + "]");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error occur when create initial card", e);
            return false;
        }
        return true;
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
        // String bundleId = timelineItem.getBundleId();

        String productId = cardDao.getCardRefById(userId, productCardId);
        String shoppingListCardId = cardDao.getShoppingListCardByProductCard(userId, productCardId);
        String shoppingListId = cardDao.getCardRefById(userId, shoppingListCardId);

        // Update the model
        if (isMark)
            shoppingListProvider.markProduct(userId, shoppingListId, productId, productCardId);
        else
            shoppingListProvider.unMarkProduct(userId, shoppingListId, productId, productCardId);

        Map<String, Object> viewbean = new HashMap<String, Object>(shoppingListProvider.getProductData(userId, shoppingListId, productId));
        List<Map<String, Object>> subShoppingList = shoppingListProvider.getShoppingList(userId, shoppingListId,
                (String) viewbean.get(Constants.ITEM_COL_CATEGORY));
        // int[] completedStatus = MirrorUtil.calculateCompletedStatus(subShoppingList);

        // Create an empty timeline item for patch
        TimelineItem patchTimelineItem = new TimelineItem();

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();
        patchTimelineItem.setMenuItems(menuItemList);

        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        if (isMark) {
            menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_UNMARK).setDisplayName(Constants.MENU_NAME_UNMARK));
            patchTimelineItem.getMenuItems().add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_UNMARK).setAction("CUSTOM"));
        } else {
            menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK).setDisplayName(Constants.MENU_NAME_MARK));
            patchTimelineItem.getMenuItems().add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));
        }

        viewbean.put(Constants.VELOCITY_PARM_ITEMS_IN_CATEGORY, subShoppingList);
        String html = VelocityHelper.getFinalStr(viewbean, "productInfo.vm");
        patchTimelineItem.setHtml(html);

        try {
            mirrorClient.timeline().patch(productCardId, patchTimelineItem).execute();
            // updateCategoryCoverCard(mirrorClient, userId, shoppingListId, shoppingListCardId, bundleId,
            // (String) viewbean.get(Constants.ITEM_COL_CATEGORY), completedStatus[0], completedStatus[1],
            // shoppingListProvider.getShoppingListName(userId, shoppingListId));
            updateShoppingListCard(mirrorClient, userId, shoppingListId, shoppingListCardId, Constants.SHOPPING_LIST_STATUS_IN_PROGRESS,
                    Constants.SHOPPING_LIST_STATUS_IN_PROGRESS);

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
            if (restart) {
                menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_IC_STARTSHOPPING).setDisplayName(
                        Constants.MENU_NAME_IC_STARTSHOPPING));
                timelineItem.getMenuItems().add(
                        new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_IC_STARTSHOPPING).setAction("CUSTOM"));
            } else {
                menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_IC_REFRESH).setDisplayName(Constants.MENU_NAME_IC_REFRESH));
                timelineItem.getMenuItems().add(
                        new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_IC_REFRESH).setAction("CUSTOM"));
                menuValues = new ArrayList<MenuValue>();
                menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_IC_RESTART).setDisplayName(Constants.MENU_NAME_IC_RESTART));
                timelineItem.getMenuItems().add(
                        new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_IC_RESTART).setAction("CUSTOM"));

            }
            mirrorClient.timeline().patch(cardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while update the IC card:" + cardId + " restart:" + restart);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void updateShoppingListCard(Mirror mirrorClient, String userId, String shoppingListId, String shoppingListCardId,
            String status, String preStatus) {
        try {
            String html = VelocityHelper.getFinalStr(buildShoppingListViewBean(userId, shoppingListId, status), "shoppingList.vm");
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
                            new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_FINISHSHOPPING).setAction("CUSTOM"));
                }
            }
            mirrorClient.timeline().patch(shoppingListCardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while updating the shopping list card for:" + userId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    private void updateCategoryCoverCard(Mirror mirrorClient, String userId, String shoppingListId, String shoppingListCardId,
            String bundleId, String category, int numOfCompleted, int subTotal, String shoppingListName) {
        String cardId = cardDao.getBundleCoverCardId(userId, bundleId, shoppingListCardId);

        Map<String, Object> viewbean = buildBundleConverViewBean(category, numOfCompleted, subTotal, shoppingListName);

        try {

            TimelineItem timelineItem = new TimelineItem();

            String html = VelocityHelper.getFinalStr(viewbean, "bundleConver.vm");
            timelineItem.setHtml(html);

            mirrorClient.timeline().patch(cardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while updating the bundle cover card for:" + userId + " category:" + category + " bundleId:" + bundleId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    private TimelineItem createItemConverCard(Mirror mirrorClient, Map<String, Object> itemData, String bundleId, String userId,
            String shoppingListCardId) {

        TimelineItem returnItem = null;
        String html = VelocityHelper.getFinalStr(itemData, "bundleConver.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setIsBundleCover(true);

        timelineItem.setBundleId(bundleId);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            returnItem = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(returnItem.getId(), userId, Constants.CARD_TYPE_CATEGORY_COVER, bundleId, shoppingListCardId);
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
        Map<String, List<Map<String, Object>>> shoppingList = shoppingListProvider.getShoppingList(userId, shoppingListId);
        String shoppingListName = shoppingListProvider.getShoppingListName(userId, shoppingListId);
        return MirrorUtil.buildShoppingListViewBean(shoppingList, shoppingListName, status);
    }

}
