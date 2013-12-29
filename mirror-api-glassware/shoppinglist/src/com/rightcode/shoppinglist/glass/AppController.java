package com.rightcode.shoppinglist.glass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
//		((DemoShoppingListProvider)shoppingListProvider).refreshData(); //For testing only

		bundleIdSuffix = String.valueOf(System.currentTimeMillis());

		initGlass(userId, mirrorClient);
	}


	private void initGlass(String userId, Mirror mirrorClient) {
		Map<String, List<Map<String,Object>>> shoppingList = shoppingListProvider.getShoppingList(userId);
		
		Iterator<String> iter = shoppingList.keySet().iterator();
		
		while (iter.hasNext()) {
			String category = (String) iter.next();
			List<Map<String,Object>> productList = shoppingList.get(category);
			
			String bundleId = getBundleId(category);
			
			Map<String, Object> bundleConverViewbean = buildBundleConverViewBean(category, productList.size(),0);
			createItemConverCard(mirrorClient, bundleConverViewbean, bundleId, userId);
			
			for (Iterator<Map<String,Object>> iterator = productList.iterator(); iterator.hasNext();) {
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
		
		createShoppingListCard(mirrorClient, buildShoppingListViewBean(shoppingList), userId);
	}


    private Map<String, Object> buildBundleConverViewBean(String category, int subTotoal, int numOfCompleted) {
        Map<String,Object> bundleConverViewbean = ((ArrayMap)refDataManager.getCategorySetting(category)).clone();
        
        bundleConverViewbean.put(Constants.VELOCITY_PARM_SUBTOTOAL, subTotoal);
        bundleConverViewbean.put(Constants.VELOCITY_PARM_COMPLETED_IN_CATEGORY, numOfCompleted);
        return bundleConverViewbean;
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
			LOG.info("Shopping List card created:[" + item.getId() + "] ["+ userId + "]");
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
			LOG.info("Product card created:[" + item.getId() + "] ["+ userId + "]");
		} catch (IOException e) {
			LOG.severe("Error when create item info card, data:" + itemData);
			e.printStackTrace();
		}
	}
	
    public void markItem(Mirror mirrorClient, String userId,String cardId) {
        TimelineItem timelineItem = null;
        try {
            timelineItem = mirrorClient.timeline().get(cardId).execute();
            markOrUnMarkProduct(mirrorClient, userId, timelineItem,true);
        } catch (IOException e) {
            LOG.severe("Error when mark item:" + cardId);
            e.printStackTrace();
        }
    }
    
    public void unMarkItem(Mirror mirrorClient, String userId,String cardId) {
        TimelineItem timelineItem = null;
        try {
            timelineItem = mirrorClient.timeline().get(cardId).execute();
            markOrUnMarkProduct(mirrorClient, userId, timelineItem, false);
        } catch (IOException e) {
            LOG.severe("Error when mark item:" + cardId);
            e.printStackTrace();
        }
    }
    
	public void markOrUnMarkProduct(Mirror mirrorClient, String userId,TimelineItem timelineItem,boolean isMark) {
		int itemNub = Integer.parseInt(cardDao.getProdutNumByCardId(userId, timelineItem.getId()));
		
		//Update the model
		if(isMark)
		    shoppingListProvider.markProduct(userId, itemNub, timelineItem.getId()); 
		else
		    shoppingListProvider.unMarkProduct(userId, itemNub, timelineItem.getId()); 
		
		timelineItem.getMenuItems().clear();
	    List<MenuValue> menuValues = new ArrayList<MenuValue>();
	    if(isMark){
	        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_UNMARK).setDisplayName("UnMark"));
	        timelineItem.getMenuItems().add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_UNMARK).setAction("CUSTOM"));
	    }else{
	        menuValues.add(new MenuValue().setIconUrl(Constants.MENU_ICON_MARK).setDisplayName("Mark"));
	        timelineItem.getMenuItems().add(new MenuItem().setValues(menuValues).setId(Constants.MENU_ID_MARK).setAction("CUSTOM"));
	    }
	    
		Map<String,Object> viewBean = shoppingListProvider.getProductData(userId, itemNub);
		String html = VelocityHelper.getFinalStr(viewBean, "productInfo.vm");
		timelineItem.setHtml(html);

		try {
			mirrorClient.timeline().update(timelineItem.getId(), timelineItem).execute();
			updateCategoryCoverCard(mirrorClient, userId, timelineItem.getBundleId(), (String)viewBean.get(Constants.ITEM_COL_CATEGORY));
			updateShoppingListCard(mirrorClient,userId);
			
			LOG.info("Item[" +timelineItem.getId() +  "] is updated to------------purchased:" + isMark);
		} catch (IOException e) {
			LOG.severe("Error when update prodcut purchase status:" + timelineItem.getId());
			e.printStackTrace();
		}
	}
	
	private void updateShoppingListCard(Mirror mirrorClient, String userId){
	    try {
	        String cardId = cardDao.getShoppingListCardId(userId);
	        String html = VelocityHelper.getFinalStr(buildShoppingListViewBean(userId), "shoppingList.vm");
            TimelineItem timelineItem = mirrorClient.timeline().get(cardId).execute();
            timelineItem.setHtml(html);
            
            mirrorClient.timeline().update(cardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while updating the shopping list card for:" + userId);
            e.printStackTrace();
        }
	}
	
    private void updateCategoryCoverCard(Mirror mirrorClient, String userId,String bundleId, String category){
        String cardId = cardDao.getBundleCoverCardId(userId, bundleId);
        
        List subShoppingList = shoppingListProvider.getShoppingList(userId, category);
        
        int[] completedStatus = calculateCompletedStatus(subShoppingList);
        
        Map viewBean = buildBundleConverViewBean(category, completedStatus[0], completedStatus[1]);
                
        try {

            TimelineItem timelineItem = mirrorClient.timeline().get(cardId).execute();
            
            LOG.info("Have got the bundle card:" + timelineItem.getId());
            String html = VelocityHelper.getFinalStr(viewBean, "bundleConver.vm");
            timelineItem.setHtml(html);
            
            mirrorClient.timeline().update(cardId, timelineItem).execute();
        } catch (IOException e) {
            LOG.severe("Error occur while updating the bundle cover card for:" + userId + " category:" + category + " bundleId:"+bundleId);
            throw new RuntimeException(e);
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
			cardDao.insertCard(returnItem.getId(), userId, Constants.CARD_TYPE_BUNDLE, bundleId);
			LOG.info("Bundle cover card created:[" + returnItem.getId() + "] ["+ userId + "]");
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
	
	private Map<String,Object> buildShoppingListViewBean(String userId){
	    Map<String,List<Map<String,Object>>> shoppingList = shoppingListProvider.getShoppingList(userId);
	    return buildShoppingListViewBean(shoppingList);
	}
    private Map<String,Object> buildShoppingListViewBean(Map<String,List<Map<String,Object>>> shoppingList){
        Map<String,Object> viewBean = new HashMap<String, Object>();
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
    private int[] calculateCompletedStatus(List<Map<String,Object>> subShoppingList){
        int[] result = new int[]{0,0};
        
        for (int i = 0; i < subShoppingList.size(); i++) {
            Map product = subShoppingList.get(i);
            boolean isPurchased = (Boolean)product.get(Constants.ITEM_COL_PURCHASED);
            if(isPurchased){
                result[1] ++;
            }
        }
        result[0] = subShoppingList.size();
        return result;
    }
    
    private int[] calculateCompletedStatus(Map<String,List<Map<String,Object>>> shoppingList){
        int[] result = new int[]{0,0};
        
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
