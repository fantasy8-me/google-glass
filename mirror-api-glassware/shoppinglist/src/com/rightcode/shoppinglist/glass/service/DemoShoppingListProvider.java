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

public class DemoShoppingListProvider implements ShoppingListProvider {

	/**
	 * First Level Key is shopping list, Second Level Key is category 
	 */
	private Map<String, Map<String,List<Map<String, Object>>>> productData = null;

	private static DemoShoppingListProvider demoShoppingListProvider = null;

	private static final Logger LOG = Logger.getLogger(DemoShoppingListProvider.class.getSimpleName());
	
	private CardDao cardDao = null;
	
	private Map<String,String> shoppingListNameMap = new HashMap<String, String>();

	private DemoShoppingListProvider() {
		JsonFactory jsonFactory = new JacksonFactory();
		cardDao = CardDao.getInstance();
		shoppingListNameMap.put("d17bb2da-ce13-4eea-99d4-a2a800b68217", "Household List");
		shoppingListNameMap.put("61c368e0-4951-446c-9857-a291015149c2", "Sunday's List");
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
	
    @Override
    public Map<String,Map<String, List<Map<String, Object>>>> getAllShoppingLists(String userId) {
        Iterator<String> iter = productData.keySet().iterator();
        while (iter.hasNext()) {
            String shoppingListId = (String) iter.next();
            mergePurchaseStatus(userId, shoppingListId);
        }
        return productData;
    }
    
	public List<Map<String,Object>> getShoppingList(String userId, String shoppingListId, String category) {
		// Use the dummy user id which is defined in productData.json
	    mergePurchaseStatus(userId, shoppingListId);
		return productData.get(shoppingListId).get(category);
		// return productData.get(userId);
	}

	@Override
	public Map<String, List<Map<String,Object>>> getShoppingList(String userId, String shoppingListId) {
	    mergePurchaseStatus(userId,shoppingListId);
	    Map<String, List<Map<String,Object>>> shoppingList = productData.get(shoppingListId);
	    
	    return shoppingList;
	}
	
	
    @Override
    public void markProduct(String userId, String shoppingListId, String productId, String cardId) {
        cardDao.markPurchaseStatus(userId, cardId, true);
    }

    @Override
    public void unMarkProduct(String userId, String shoppingListId, String productId, String cardId) {
        cardDao.markPurchaseStatus(userId, cardId, false);
    }
    
    @Override
    public Map<String, Object> getProductData(String userId, String shoppingListId, String productId) {
      mergePurchaseStatus(userId,shoppingListId);
      Map<String, List<Map<String,Object>>> shoppingList = productData.get(shoppingListId);
        
        Iterator<String> iter = shoppingList.keySet().iterator();
        
        while (iter.hasNext()) {
            List<Map<String,Object>> subShoppingList = shoppingList.get(iter.next());
            Map<String,Object> prodcutData = null;
            for (int i = 0; i < subShoppingList.size(); i++) {
                prodcutData = subShoppingList.get(i);
                //BigDecimal is used by google JSON library to represent a number
                if(productId.equals(prodcutData.get(Constants.ITEM_COL_PRD_ID))){
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
    private void mergePurchaseStatus(String userId, String shoppingListId) {
        Map<String, List<Map<String,Object>>> shoppingList = productData.get(shoppingListId);
        
        Iterator<String> iter = shoppingList.keySet().iterator();
        //As we can tell which shopping list a product card belongs to, so we have to get purchase status for all cards in all shopping list
        //Two long term soltion
        //1. Enhance the app db to maintain the relationship between shopping list and product card
        //2. Enhance the criteria, use something like in (cardId1, cardId2, cardId3 ...) to build up a efficient sql
        //3. Instead of getting all the status in one sql, execute a sql get the the status for one card only(but we have to make many sql call by using this approach)
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
           Boolean purchased = purchaseStatus.get(product.get(Constants.ITEM_COL_PRD_ID));
           product.put(Constants.ITEM_COL_PURCHASED, purchased == null ? false : purchased); //set initial value as false;
        }
    }
    
    @Override
    public String getShoppingListName(String userId, String shoppingListId) {
        return shoppingListNameMap.get(shoppingListId);
    }
    
    public static void main(String[] args) {
        JsonFactory jsonFactory = new JacksonFactory();
        Object data = null;
        try {
            //Shopping list will be cached when initialize DemoShoppingListProvider
            data = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData_real.json"),null);
            System.out.println("data:" + data);
        } catch (IOException e) {
            LOG.severe("Can not init product data");
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        
        List<Map<String,Object>> productList = new ArrayList<Map<String,Object>>();
        Map<String,Object> productMap1 = new HashMap<String, Object>();
        productMap1.put("prdNum", 1);
        productMap1.put("prdDes", "Fish");
        productMap1.put("Price", 1.234);
        productMap1.put("Purchased", false);
        productList.add(productMap1);
        
        Map<String,Object> productMap2 = new HashMap<String, Object>();
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
