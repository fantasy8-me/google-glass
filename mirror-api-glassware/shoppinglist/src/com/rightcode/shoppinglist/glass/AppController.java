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
import com.google.api.client.util.ArrayMap;
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
    private String bundleIdSuffix = ""; // used for testing

    private static final Logger LOG = Logger.getLogger(AppController.class.getSimpleName());

    private AppController() {
        shoppingListProvider = DemoShoppingListProvider.getInstance();
        cardDao = CardDao.getInstance();
        refDataManager = ReferenceDataManager.getInstance();
        VelocityHelper.initVelocity();

    };

    public synchronized static AppController getInstance() {
        if (appController == null) {
            //This log is used to indicate that that our app is reload.
            LOG.info("*--------------------------AppController inited:" + System.currentTimeMillis());
            appController = new AppController();
        }
        return appController;

    }

    public void initApp(String userId) throws IOException {
        Credential credential;
        credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);

        bundleIdSuffix = String.valueOf(System.currentTimeMillis());

        initGlass(userId, mirrorClient);
    }

    public void startShopping(String userId) throws IOException {
        Credential credential;
        credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);

        MirrorUtil.touchCard(mirrorClient, cardDao.getShoppingListCardId(userId));

        List<String> bundleCovers = cardDao.getAllBundleConvers(userId);
        for (int i = 0; i < bundleCovers.size(); i++) {
            MirrorUtil.touchCard(mirrorClient, bundleCovers.get(i));
        }
        LOG.info("Starting shopping done, all cards are move to front of your timeline");
    }

    private void initGlass(String userId, Mirror mirrorClient) {
        Map<String, List<Map<String, Object>>> shoppingList = shoppingListProvider.getShoppingList(userId);

        Iterator<String> iter = shoppingList.keySet().iterator();

        while (iter.hasNext()) {
            String category = (String) iter.next();
            List<Map<String, Object>> productList = shoppingList.get(category);

            String bundleId = getBundleId(category);

            Map<String, Object> bundleConverViewbean = buildBundleConverViewBean(category, productList.size(), 0);
            createItemConverCard(mirrorClient, bundleConverViewbean, bundleId, userId);

            for (Iterator<Map<String, Object>> iterator = productList.iterator(); iterator.hasNext();) {
                Map<String, Object> viewbean = iterator.next();
                createItemInfoCard(mirrorClient, viewbean, bundleId, userId);
            }
        }

        createShoppingListCard(mirrorClient, buildShoppingListViewBean(shoppingList), userId);
    }

    private Map<String, Object> buildBundleConverViewBean(String category, int subTotoal, int numOfCompleted) {

        Map<String, Object> bundleConverViewbean = new HashMap<String, Object>(refDataManager
                .getCategorySetting(category));

        bundleConverViewbean.put(Constants.VELOCITY_PARM_SUBTOTOAL, subTotoal);
        bundleConverViewbean.put(Constants.VELOCITY_PARM_COMPLETED_IN_CATEGORY, numOfCompleted);
        return bundleConverViewbean;
    }

    private void createShoppingListCard(Mirror mirrorClient, Map<String, Object> items, String userId) {
        String html = VelocityHelper.getFinalStr(items, "shoppingList.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_STARTSHOPPING).setDisplayName("Start Shopping"));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_STARTSHOPPING)
                .setAction("CUSTOM"));
        menuItemList.add(new MenuItem().setAction("TOGGLE_PINNED"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            TimelineItem item = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_MAIN, null);
            LOG.info("Shopping List card created:[" + item.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create item info card, data:" + items);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void createItemInfoCard(Mirror mirrorClient, Map<String, Object> itemData, String bundleId, String userId) {

        String html = VelocityHelper.getFinalStr(itemData, "productInfo.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setBundleId(bundleId);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK).setDisplayName("Mark"));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            TimelineItem item = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_PRODUCT,
                    String.valueOf(itemData.get(Constants.ITEM_COL_PRDNUM)));
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

        // Create an empty timeline item for patch
        timelineItem = new TimelineItem(); 

        int itemNub = Integer.parseInt(cardDao.getProdutNumByCardId(userId, productCardId));

        // Update the model
        if (isMark)
            shoppingListProvider.markProduct(userId, itemNub, productCardId);
        else
            shoppingListProvider.unMarkProduct(userId, itemNub, productCardId);

        // timelineItem.getMenuItems().clear();

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();
        timelineItem.setMenuItems(menuItemList);

        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        if (isMark) {
            menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_UNMARK).setDisplayName("UnMark"));
            timelineItem.getMenuItems().add(
                    new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_UNMARK).setAction("CUSTOM"));
        } else {
            menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK).setDisplayName("Mark"));
            timelineItem.getMenuItems().add(
                    new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));
        }

        Map<String, Object> viewBean = shoppingListProvider.getProductData(userId, itemNub);
        String html = VelocityHelper.getFinalStr(viewBean, "productInfo.vm");
        timelineItem.setHtml(html);

        try {
            mirrorClient.timeline().patch(productCardId, timelineItem).execute();
            updateCategoryCoverCard(mirrorClient, userId, bundleId, (String) viewBean.get(Constants.ITEM_COL_CATEGORY));
            updateShoppingListCard(mirrorClient, userId);

            LOG.info("--------Purchase status of item[" + productCardId + "] is updated to:" + isMark);
        } catch (IOException e) {
            LOG.severe("Error when update prodcut purchase status:" + productCardId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void updateShoppingListCard(Mirror mirrorClient, String userId) {
        try {
            String cardId = cardDao.getShoppingListCardId(userId);
            String html = VelocityHelper.getFinalStr(buildShoppingListViewBean(userId), "shoppingList.vm");
            TimelineItem timelineItem = new TimelineItem();
            timelineItem.setHtml(html);

            mirrorClient.timeline().patch(cardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while updating the shopping list card for:" + userId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void updateCategoryCoverCard(Mirror mirrorClient, String userId, String bundleId, String category) {
        String cardId = cardDao.getBundleCoverCardId(userId, bundleId);

        List<Map<String, Object>> subShoppingList = shoppingListProvider.getShoppingList(userId, category);

        int[] completedStatus = calculateCompletedStatus(subShoppingList);

        Map<String, Object> viewBean = buildBundleConverViewBean(category, completedStatus[0], completedStatus[1]);

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
            String userId) {

        TimelineItem returnItem = null;
        String html = VelocityHelper.getFinalStr(itemData, "bundleConver.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setIsBundleCover(true);

        timelineItem.setBundleId(bundleId);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            returnItem = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(returnItem.getId(), userId, Constants.CARD_TYPE_BUNDLE, bundleId);
            LOG.info("Bundle cover card created:[" + returnItem.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create item conver card, data:" + itemData);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        return returnItem;

    }

    private String getBundleId(String category) {
        // Add current time stamp to separate the cards created in different
        // time
        return "bundle_" + category + "_" + bundleIdSuffix;
    }

    private Map<String, Object> buildShoppingListViewBean(String userId) {
        Map<String, List<Map<String, Object>>> shoppingList = shoppingListProvider.getShoppingList(userId);
        return buildShoppingListViewBean(shoppingList);
    }

    private Map<String, Object> buildShoppingListViewBean(Map<String, List<Map<String, Object>>> shoppingList) {
        Map<String, Object> viewBean = new HashMap<String, Object>();
        int[] result = calculateCompletedStatus(shoppingList);
        viewBean.put(Constants.VELOCICY_PARM_AllPRODUCTS, shoppingList);
        viewBean.put(Constants.VELOCICY_PARM_CATEGORY_TITLES, refDataManager.getCategoryTitleMap());
        viewBean.put(Constants.VELOCITY_PARM_COMPLETED, result[1]);
        viewBean.put(Constants.VELOCITY_PARM_TOTOAL, result[0]);
        return viewBean;

    }

    /**
     * @param subShoppingList
     * @return int[] int[0]: completed int[1]: total
     */
    private int[] calculateCompletedStatus(List<Map<String, Object>> subShoppingList) {
        int[] result = new int[] { 0, 0 };

        for (int i = 0; i < subShoppingList.size(); i++) {
            Map<String, Object> product = subShoppingList.get(i);
            boolean isPurchased = (Boolean) product.get(Constants.ITEM_COL_PURCHASED);
            if (isPurchased) {
                result[1]++;
            }
        }
        result[0] = subShoppingList.size();
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
