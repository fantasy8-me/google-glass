package com.rightcode.shoppinglist.glass.service;

import java.util.List;
import java.util.Map;

public interface ShoppingListProvider {
	
	public List<Map<String,Object>> getShoppingList(String userId, String category);
	
	public Map<String,List<Map<String,Object>>> getShoppingList(String userId);
	
	
	public void markProduct(String userId, int productNum, String cardId);
	
	public void unMarkProduct(String userId, int productNum, String cardId);
	
	public Map<String,Object> getProductData(String userId, int productNum);

}
