package com.rightcode.shoppinglist.glass.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.dao.CardDao;

public class DemoShoppingListProvider implements ShoppingListProvider {

	private Map<String, Map<String,List<Map<String, Object>>>> productData = null;

	private static DemoShoppingListProvider demoShoppingListProvider = null;

	private static final Logger LOG = Logger.getLogger(DemoShoppingListProvider.class.getSimpleName());
	
	private CardDao cardDao = null;

	private DemoShoppingListProvider() {
		JsonFactory jsonFactory = new JacksonFactory();
		cardDao = CardDao.getInstance();
		try {
		    //Shopping list will be cached when initialize DemoShoppingListProvider
			productData = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData.json"),null);
		} catch (IOException e) {
			LOG.severe("Can not init product data");
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	};

	public synchronized static ShoppingListProvider getInstance() {
		if (demoShoppingListProvider == null) {
			demoShoppingListProvider = new DemoShoppingListProvider();
		}
		return demoShoppingListProvider;

	}
	
	public List<Map<String,Object>> getShoppingList(String userId, String category) {
		// Use the dummy user id which is defined in productData.json
	    mergePurchaseStatus(userId);
		return productData.get("dummyusrId1").get(category);
		// return productData.get(userId);
	}

	@Override
	public Map<String, List<Map<String,Object>>> getShoppingList(String userId) {
	    mergePurchaseStatus(userId);
	    Map<String, List<Map<String,Object>>> shoppingList = productData.get("dummyusrId1");
	    
	    return shoppingList;
	}
	
	
    @Override
    public void markProduct(String userId, int prdNum, String cardId) {
        cardDao.markPurchaseStatus(userId, cardId, true);
    }

    @Override
    public void unMarkProduct(String userId, int productNum, String cardId) {
        cardDao.markPurchaseStatus(userId, cardId, false);
    }
    
    @Override
    public Map<String, Object> getProductData(String userId, int productNum) {
      mergePurchaseStatus(userId);
      Map<String, List<Map<String,Object>>> shoppingList = productData.get("dummyusrId1");
        
        Iterator<String> iter = shoppingList.keySet().iterator();
        
        while (iter.hasNext()) {
            List<Map<String,Object>> subShoppingList = shoppingList.get(iter.next());
            Map<String,Object> prodcutData = null;
            for (int i = 0; i < subShoppingList.size(); i++) {
                prodcutData = subShoppingList.get(i);
                //BigDecimal is used by google JSON library to represent a number
                if(((BigDecimal)prodcutData.get(Constants.ITEM_COL_PRDNUM)).intValue() == productNum){
                    return prodcutData;
                }
            }
        }
        return null;
    }

    /**
     * The product purchase status is store in our Card table, we need to merge the status
     * to the cached shopping list
     * 
     * @param userId
     */
    private void mergePurchaseStatus(String userId) {
        Map<String, List<Map<String,Object>>> shoppingList = productData.get("dummyusrId1");
        
        Iterator<String> iter = shoppingList.keySet().iterator();
        Map<String,Boolean> purchaseStatus = cardDao.getPurchaseStatus(userId);
        
        while (iter.hasNext()) {
            String categoryId = (String) iter.next();
            List<Map<String,Object>> subShoppingList = shoppingList.get(categoryId);
            enrichPurchaseStatus(purchaseStatus, subShoppingList);
        }
    }   

    private void enrichPurchaseStatus(Map<String, Boolean> purchaseStatus, List<Map<String, Object>> shoppingList) {
        for (int i = 0; i < shoppingList.size(); i++) {
           Map<String,Object> product = shoppingList.get(i);
           Boolean purchased = purchaseStatus.get(String.valueOf(product.get(Constants.ITEM_COL_PRDNUM)));
           product.put(Constants.ITEM_COL_PURCHASED, purchased == null ? false : purchased); //set initial value as false;
        }
    }
    
    public static void main(String[] args) {
        System.out.println(DemoShoppingListProvider.getInstance().getShoppingList("", "category1"));
    }

}
