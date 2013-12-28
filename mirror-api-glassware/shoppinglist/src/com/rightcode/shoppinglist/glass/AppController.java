package com.rightcode.shoppinglist.glass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import com.rightcode.shoppinglist.glass.dao.CardDao;
import com.rightcode.shoppinglist.glass.ref.AuthUtil;
import com.rightcode.shoppinglist.glass.ref.MirrorClient;
import com.rightcode.shoppinglist.glass.service.DemoShoppingListProvider;
import com.rightcode.shoppinglist.glass.service.ShoppingListProvider;
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
		((DemoShoppingListProvider)shoppingListProvider).refreshData(); //For testing only

		bundleIdSuffix = String.valueOf(System.currentTimeMillis());

		initGlass(userId, mirrorClient);
	}


	private void initGlass(String userId, Mirror mirrorClient) {
		Map<String, List<Map>> shoppingList = shoppingListProvider.getShoppingList(userId);
		
		Iterator<String> iter = shoppingList.keySet().iterator();
		
		while (iter.hasNext()) {
			String category = (String) iter.next();
			List<Map> productList = shoppingList.get(category);
			
			String bundleId = getBundleId(category);
			
			Map<String,Object> bundleConverViewbean = ((ArrayMap)refDataManager.getCategorySetting(category)).clone();
			
			bundleConverViewbean.put(Constants.VELOCITY_PARM_SUBTOTOAL, productList.size());
			bundleConverViewbean.put(Constants.VELOCITY_COMPLETE_IN_CATEGORY, 0);
			createItemConverCard(mirrorClient, bundleConverViewbean, bundleId, userId);
			
			for (Iterator<Map> iterator = productList.iterator(); iterator.hasNext();) {
				Map<String,Object> viewbean = ((ArrayMap) iterator.next()).clone();
				//Eric.TODO, depends on the card design, might not need to put this
				viewbean.put(Constants.VELOCITY_PARM_SUBTOTOAL, productList.size());
				createItemInfoCard(mirrorClient, viewbean,bundleId, userId);
			}
		}

/*		Map<String, String> bundlesMap = new HashMap<String, String>();
		Map<String, Integer> bundleItemsCount = new HashMap<String, Integer>();
		for (int i = 0; i < shoppingList.size(); i++) {
			Map<String, Object> viewbean = null;
			
			String category = (String)viewbean.get(Constants.ITEM_COL_CATEGORY);
			if(category == null || category.isEmpty()){
				category = Constants.CATEGORY_DFT;
			}
			
			if(bundlesMap.containsKey(category)){
				viewbean = ((ArrayMap) shoppingList.get(i)).clone();
				String bundleId = bundlesMap.get(category);
				viewbean.put(Constants.ITEM_PARM_MAXITEM, shoppingList.size());
				createItemInfoCard(mirrorClient, viewbean,bundleId, userId);
				bundleItemsCount.put(category, bundleItemsCount.get(category) + 1);
			}else{
				String bundleId = getBundleId(viewbean);
				
				ReferenceDataManager refDataManager = ReferenceDataManager.getInstance();
				
				viewbean = ((ArrayMap)refDataManager.getCategorySetting(category)).clone();
				createItemConverCard(mirrorClient, viewbean, bundleId, userId);
				bundlesMap.put(category, bundleId);
				bundleItemsCount.put(category, 0);
			}
		}*/
		
		Map<String,Object> items = new HashMap<String, Object>();
		items.put(Constants.VELOCICY_PARM_AllPRODUCTS, shoppingList);
		items.put(Constants.VELOCICY_PARM_CATEGORY_TITLES, refDataManager.getCategoryTitleMap());
		createShoppingListCard(mirrorClient, items, userId);
	}

	private void createShoppingListCard(Mirror mirrorClient, Map items, String userId) {
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
		} catch (IOException e) {
			LOG.severe("Error when create item info card, data:" + items);
			e.printStackTrace();
		}
	}

	private void createItemInfoCard(Mirror mirrorClient, Map itemData, String bundleId,String userId) {

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
			TimelineItem item  = mirrorClient.timeline().insert(timelineItem).execute();
			cardDao.insertCard(item.getId(), userId, Constants.CARD_TYPE_PRODUCT, String.valueOf(itemData.get(Constants.ITEM_COL_PRDNUM)));
		} catch (IOException e) {
			LOG.severe("Error when create item info card, data:" + itemData);
			e.printStackTrace();
		}
	}
	
	public void markItem(Mirror mirrorClient, String userId,TimelineItem timelineItem) {
		Map itemData = null;
		String bundleId = timelineItem.getBundleId();
		Log.info("----------bundld id:" + bundleId + " itemid:" + timelineItem.getId());
		int itemNub = Integer.parseInt(bundleId.substring(4,bundleId.indexOf("_")));
		Log.info("----------bundld id:" + bundleId + " item number:" + itemNub);
		
		List shoppingList = shoppingListProvider.getShoppingList(userId,"");//Eric. wrong logic, fix later
		for (int i = 0; i < shoppingList.size(); i++) {
			if(itemNub-1 == i){
				Map item = (Map)shoppingList.get(i);
				item.put(Constants.ITEM_COL_PURCHASED, true);
			}
		}
//		Map items = new HashMap<String, List>();
//		items.put(Constants.VELOCICY_PARM_AllPRODUCTS, shoppingList);
//		items.put(Constants.ITEM_PARM_MAXITEM, shoppingList.size());
		
		String html = VelocityHelper.getFinalStr((Map)shoppingList.get(itemNub-1), "productInfo.vm");
		timelineItem.setHtml(html);

		try {
			mirrorClient.timeline().update(timelineItem.getId(), timelineItem).execute();
			  LOG.info("Item is marked------------");
		} catch (IOException e) {
			LOG.severe("Error when create item info card, data:" + itemData);
			e.printStackTrace();
		}
	}
	private TimelineItem createItemConverCard(Mirror mirrorClient, Map itemData,String bundleId, String userId) {

		TimelineItem returnItem = null;
		String html = VelocityHelper.getFinalStr(itemData, "bundleConver.vm");

		TimelineItem timelineItem = new TimelineItem();
		timelineItem.setHtml(html);
		timelineItem.setIsBundleCover(true);

		timelineItem.setBundleId(bundleId);
		timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

		try {
			returnItem  = mirrorClient.timeline().insert(timelineItem).execute();
			cardDao.insertCard(returnItem.getId(), userId, Constants.CARD_TYPE_BUNDLE, null);			
		} catch (IOException e) {
			LOG.severe("Error when create item conver card, data:" + itemData);
			e.printStackTrace();
		}
		return returnItem;

	}

	private String getBundleId(String category) {
		// Add current time stamp to separate the cards created in different
		// time
		// Eric.TODO check if required later
		return "bundle_" + category + "_" + bundleIdSuffix;
	}
}
