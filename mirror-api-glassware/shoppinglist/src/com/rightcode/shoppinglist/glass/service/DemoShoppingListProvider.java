package com.rightcode.shoppinglist.glass.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.rightcode.shoppinglist.glass.Constants;

public class DemoShoppingListProvider implements ShoppingListProvider {

	private Map<String, Map<String,List<Map>>> productData = null;

	private static DemoShoppingListProvider demoShoppingListProvider = null;

	private static final Logger LOG = Logger.getLogger(DemoShoppingListProvider.class.getSimpleName());

	private DemoShoppingListProvider() {
		JsonFactory jsonFactory = new JacksonFactory();
		try {
			productData = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData.json"),null);
		} catch (IOException e) {
			LOG.severe("Can not init product data");
			e.printStackTrace();
		}
	};

	public synchronized static ShoppingListProvider getInstance() {
		if (demoShoppingListProvider == null) {
			demoShoppingListProvider = new DemoShoppingListProvider();
		}
		return demoShoppingListProvider;

	}
	
	/**
	 * For testing use only
	 */
	public void refreshData(){
		JsonFactory jsonFactory = new JacksonFactory();
		try {
			productData = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData.json"),
					HashMap.class);
		} catch (IOException e) {
			LOG.severe("Can not init product data");
			e.printStackTrace();
		}
	}

	public List<Map> getShoppingList(String userId, String category) {
		// TODO.Eric before the implementaion of local json db, we need to hard
		// the dummy user id
		return productData.get("dummyusrId1").get(category);
		// return productData.get(userId);
	}

	@Override
	public Map<String, List<Map>> getShoppingList(String userId) {
		return productData.get("dummyusrId1");
	}
	
	
	public static void main(String[] args) {
		System.out.println(DemoShoppingListProvider.getInstance().getShoppingList("", "category1"));
	}

}
