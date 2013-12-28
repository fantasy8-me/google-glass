package com.rightcode.shoppinglist.glass.service;

import java.util.List;
import java.util.Map;

public interface ShoppingListProvider {
	
	public List<Map> getShoppingList(String userId);
	

}
