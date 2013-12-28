package com.rightcode.shoppinglist.glass.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

public class DemoShoppingListProvider implements ShoppingListProvider {

	private Map<String, List> productData = null;

	private static DemoShoppingListProvider demoShoppingListProvider = null;

	private static final Logger LOG = Logger.getLogger(DemoShoppingListProvider.class.getSimpleName());

	private DemoShoppingListProvider() {
		JsonFactory jsonFactory = new JacksonFactory();
		try {
			productData = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData.json"),
					HashMap.class);
		} catch (IOException e) {
			LOG.severe("Can not init produc data");
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
			LOG.severe("Can not init produc data");
			e.printStackTrace();
		}
	}

	public List<Map> getShoppingList(String userId) {
		// TODO.Eric before the implementaion of local json db, we need to hard
		// the dummy user id
		return productData.get("dummyusrId1");
		// return productData.get(userId);
	}
	
	public static void main(String[] args) {
		DemoShoppingListProvider.getInstance();
	}
}
