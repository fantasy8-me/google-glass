package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;
import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.dao.CardDao;
import com.rightcode.shoppinglist.glass.ref.AuthUtil;
import com.rightcode.shoppinglist.glass.ref.MirrorClient;

public final class MirrorUtil {

    private static final Logger LOG = Logger.getLogger(MirrorUtil.class.getSimpleName());

    /**
     * Touch a card by "PATCH" method to bring the card to front
     * 
     * @param mirrorClient
     * @param cardId
     */
    public static void touchCard(Mirror mirrorClient, String cardId) {
        try {
            mirrorClient.timeline().patch(cardId, null).execute();
        } catch (Exception e) {
            LOG.severe("Error occur when touch card:" + cardId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void createProductCard(String userId, Map<String, Object> productViewBean, String shoppingListCardId) throws IOException {

        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);

        String html = VelocityHelper.getFinalStr(productViewBean, "productInfo.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK).setDisplayName(Constants.MENU_NAME_MARK));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        try {
            TimelineItem item = mirrorClient.timeline().insert(timelineItem).execute();
            CardDao.getInstance().insertCard(item.getId(), userId, Constants.CARD_TYPE_PRODUCT,
                    (String) productViewBean.get(Constants.ITEM_COL_PRD_ID), shoppingListCardId);
            LOG.info("Product card created:[" + item.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create item info card, data:" + productViewBean);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Update the html conent in the product card
     * 
     * @param userId
     * @param metaData
     *            meta data of the product, contains name, price, ...
     * @param cardId
     * @param shoppingList
     *            the whole shopping list, it is required to display the other items in the list
     * @throws IOException
     */
    public static void updateProductCardContent(String userId, Map<String, Object> metaData, String cardId,
            List<Map<String, Object>> shoppingList) {

        TimelineItem patchTimelineItem = new TimelineItem();
        Map<String, Object> viewbean = new HashMap<>(metaData);

        viewbean.put(Constants.VELOCITY_PARM_ITEMS_IN_SAME_LIST, shoppingList);
        String html = VelocityHelper.getFinalStr(viewbean, "productInfo.vm");
        patchTimelineItem.setHtml(html);
        try {
            Credential credential = AuthUtil.getCredential(userId);
            Mirror mirrorClient = MirrorClient.getMirror(credential);
            mirrorClient.timeline().patch(cardId, patchTimelineItem).execute();
            LOG.info("-----Updated content for card:" + cardId);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Fail to update content for card:" + cardId, e);
        }
    }

    public static void updateShoppingListCardContent(String userId, String shoppingListCardId,
            List<Map<String, Object>> shoppingList, String shoppingListName, String status) {
        String html = VelocityHelper.getFinalStr(buildShoppingListViewBean(shoppingList, shoppingListName, status), "shoppingList.vm");
        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);

        try {
            Credential credential = AuthUtil.getCredential(userId);
            Mirror mirrorClient = MirrorClient.getMirror(credential);

            mirrorClient.timeline().patch(shoppingListCardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Fail to update content for shoppinglist card:" + shoppingListCardId, e);
        }
    }

    public static String createShoppingListCard(String userId, List<Map<String, Object>> shoppingList,
            String shoppingListName, String shoppingListId, String bundleId) throws IOException {
        CardDao cardDao = CardDao.getInstance();
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);

        Map<String, Object> items = buildShoppingListViewBean(shoppingList, shoppingListName, Constants.SHOPPING_LIST_STATUS_READY);

        String html = VelocityHelper.getFinalStr(items, "shoppingList.vm");

        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setHtml(html);
        timelineItem.setBundleId(bundleId);

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_STARTSHOPPING).setDisplayName(Constants.MENU_NAME_STARTSHOPPING));
        menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_STARTSHOPPING).setAction("CUSTOM"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        TimelineItem item = null;
        try {
            item = mirrorClient.timeline().insert(timelineItem).execute();
            cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_SHOPPINGLIST, shoppingListId, null);
            LOG.info("Shopping List card created:[" + item.getId() + "] [" + userId + "]");
        } catch (IOException e) {
            LOG.severe("Error when create shopping list card, data:" + items);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        if (item != null) {
            return item.getId();
        } else {
            return "";
        }
    }

    public static Map<String, Object> buildShoppingListViewBean(List<Map<String, Object>> shoppingList,
            String shoppingListName, String status) {
        Map<String, Object> viewBean = new HashMap<String, Object>();
        int[] result = calculateCompletedStatus(shoppingList);
        viewBean.put(Constants.VELOCICY_PARM_AllPRODUCTS, shoppingList);
        viewBean.put(Constants.VELOCITY_PARM_COMPLETED, result[0]);
        viewBean.put(Constants.VELOCITY_PARM_TOTOAL, result[1]);
        viewBean.put(Constants.VELOCICY_PARM_SHOPPING_LIST_NAME, shoppingListName);
        viewBean.put(Constants.VELOCICY_PARM_SHOPPING_LIST_STATUS, status);
        return viewBean;

    }

    /**
     * @param 
     * @return int[] int[0]: completed int[1]: total
     */
    private static int[] calculateCompletedStatus(List<Map<String, Object>> shoppingList) {
        int[] result = new int[] { 0, 0 };

        for (int i = 0; i < shoppingList.size(); i++) {
            Map<String, Object> product = shoppingList.get(i);
            boolean isPurchased = false;
            if (product.get(Constants.ITEM_COL_PURCHASED) != null) {
                isPurchased = (Boolean) product.get(Constants.ITEM_COL_PURCHASED);
            }
            if (isPurchased) {
                result[0]++;
            }
        }
        result[1] = shoppingList.size();
        return result;
    }
}
