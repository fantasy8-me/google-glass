package com.google.glassware.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mortbay.log.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.glassware.AuthUtil;
import com.google.glassware.MirrorClient;
import com.google.glassware.custom.service.DemoShoppingListProvider;
import com.google.glassware.custom.service.ShoppingListProvider;

public class AppController {

	private static AppController appController = null;
	private ShoppingListProvider shoppingListProvider = null;
	private String bundleIdSuffix = ""; // used for testing

	private static final Logger LOG = Logger.getLogger(AppController.class.getSimpleName());

	private AppController() {
		shoppingListProvider = DemoShoppingListProvider.getInstance();
		VelocityHelper.initVelocity();

	};

	public synchronized static AppController getInstance() {
		if (appController == null) {
			appController = new AppController();
		}
		return appController;

	}

	public void initApp(String userId) throws IOException {
		/*
		 * Eric.TODO we need to implement logic to check whether our app is
		 * inited for specifie user, always init for the time being
		 */
		Credential credential;
		credential = AuthUtil.getCredential(userId);
		Mirror mirrorClient = MirrorClient.getMirror(credential);

		bundleIdSuffix = String.valueOf(System.currentTimeMillis());

		List<Map> shoppingList = shoppingListProvider.getShoppingList(userId);

		for (int i = 0; i < shoppingList.size(); i++) {
			Map tmp = shoppingList.get(i);
			// Eric.TODO do not user ArrayMap
			Map<String, Object> data = ((ArrayMap) shoppingList.get(i)).clone();

			data.put(Constants.ITEM_PARM_MAXITEM, shoppingList.size());
			createItemConverCard(mirrorClient, data);
			createItemInfoCard(mirrorClient, data);
		}
		
		Map items = new HashMap<String, List>();
		items.put(Constants.LIST_PARM_ITEMS, shoppingList);
		items.put(Constants.ITEM_PARM_MAXITEM, shoppingList.size());

		createShoppingListCard(mirrorClient, items);
	}

	private void createShoppingListCard(Mirror mirrorClient, Map items) {
		String html = VelocityHelper.getFinalStr(items, "shoppingList.vm");

		TimelineItem timelineItem = new TimelineItem();
		timelineItem.setHtml(html);
		timelineItem.setBundleId(getBundleId(items));

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
			mirrorClient.timeline().insert(timelineItem).execute();
		} catch (IOException e) {
			LOG.severe("Error when create item info card, data:" + items);
			e.printStackTrace();
		}
	}

	private void createItemInfoCard(Mirror mirrorClient, Map itemData) {

		String html = VelocityHelper.getFinalStr(itemData, "itemInfo.vm");

		TimelineItem timelineItem = new TimelineItem();
		timelineItem.setHtml(html);
		timelineItem.setBundleId(getBundleId(itemData));

		List<MenuItem> menuItemList = new ArrayList<MenuItem>();

		// And custom actions
		List<MenuValue> menuValues = new ArrayList<MenuValue>();
		menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK).setDisplayName("Mark"));
		menuItemList.add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));

		timelineItem.setMenuItems(menuItemList);
		timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

		try {
			mirrorClient.timeline().insert(timelineItem).execute();
		} catch (IOException e) {
			LOG.severe("Error when create item info card, data:" + itemData);
			e.printStackTrace();
		}
	}
	
	public void markItem(Mirror mirrorClient, String userId,TimelineItem timelineItem) {
		Map itemData = null;
		String bundleId = timelineItem.getBundleId();
		int itemNub = Integer.parseInt(bundleId.substring(4,bundleId.indexOf("-")));
		Log.info("----------bundld id:" + bundleId + " item number:" + itemNub);
		
		List shoppingList = shoppingListProvider.getShoppingList(userId);
		for (int i = 0; i < shoppingList.size(); i++) {
			if(itemNub-1 == i){
				Map item = (Map)shoppingList.get(i);
				item.put(Constants.ITEM_COL_PURCHASED, true);
			}
		}
//		Map items = new HashMap<String, List>();
//		items.put(Constants.LIST_PARM_ITEMS, shoppingList);
//		items.put(Constants.ITEM_PARM_MAXITEM, shoppingList.size());
		
		String html = VelocityHelper.getFinalStr((Map)shoppingList.get(itemNub-1), "itemInfo.vm");
		timelineItem.setHtml(html);

		try {
			mirrorClient.timeline().update(timelineItem.getId(), timelineItem).execute();
			  LOG.info("Item is marked------------");
		} catch (IOException e) {
			LOG.severe("Error when create item info card, data:" + itemData);
			e.printStackTrace();
		}
	}
	private void createItemConverCard(Mirror mirrorClient, Map itemData) {

		String html = VelocityHelper.getFinalStr(itemData, "itemConver.vm");

		TimelineItem timelineItem = new TimelineItem();
		timelineItem.setHtml(html);
		timelineItem.setIsBundleCover(true);

		timelineItem.setBundleId(getBundleId(itemData));
		timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

		try {
			mirrorClient.timeline().insert(timelineItem).execute();
		} catch (IOException e) {
			LOG.severe("Error when create item conver card, data:" + itemData);
			e.printStackTrace();
		}

	}

	private String getBundleId(Map itemData) {
		// Add current time stamp to separate the cards created in different
		// time
		// Eric.TODO check if required later
		return "Item" + itemData.get(Constants.ITEM_COL_PRDNUM) + "_" + bundleIdSuffix;
	}
}
