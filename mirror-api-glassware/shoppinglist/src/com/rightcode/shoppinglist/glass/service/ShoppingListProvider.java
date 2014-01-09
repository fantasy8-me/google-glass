package com.rightcode.shoppinglist.glass.service;

import java.util.List;
import java.util.Map;

public interface ShoppingListProvider {
    
    
    
    /**
     * First Level Key is shopping list, Second Level Key is category 
     * 
     * @param userId
     * @return all shopping list of user
     */
    public Map<String,Map<String, List<Map<String, Object>>>> getAllShoppingLists(String userId);    

    /**
     * To get a list of product by specify user id and category id.
     * 
     * Meta data of category are defined in category.json
     * 
     * @param userId
     *            google user id
     * @param category
     *            category id
     * @param shoppingListId
     *            id of the shopping list
     * @return List of product in specified category
     */
    public List<Map<String, Object>> getShoppingList(String userId, String shoppingListId, String category);

    /**
     * Get all products in shopping list, product are group by category and
     * stored in a Map
     * 
     * Refer to productData.json to understand the data structure
     * 
     * @param userId
     *            google user id
     * @param shoppingListId
     *            id of the shopping list
     * @return All products in a Map, key of the map is category id.
     */
    public Map<String, List<Map<String, Object>>> getShoppingList(String userId, String shoppingListId);

    /**
     * @param userId  google user id
     * @param productId product id  unique key of a product
     * @param shoppingListId  id of the shopping list
     * @return
     */
    public Map<String, Object> getProductData(String userId, String shoppingListId, String productId);
    
    
    /**
     * Mark the purchase status to true
     * 
     * Without the external service to store the purchase statue, we have to
     * store the purchase status in our own database, along with the product
     * card.
     * 
     * @param userId
     *            google user id
     * @param shoppingListId
     *            id of the shopping list 
     * @param productId
     *            product id , unique key of a product
     * @param cardId
     *            optional parameter. If the external shopping list service
     *            allow status update, then we don't need to mark the purchase
     *            status to Card table
     * 
     */
    public void markProduct(String userId, String shoppingListId, String productId, String cardId);

    /**
     * Mark the purchase status to false
     * 
     * Without the external service to store the purchase statue, we have to
     * store the purchase status in our own database, along with the product
     * card.
     * 
     * @param userId
     *            google user id
     * @param shoppingListId
     *            id of the shopping list             
     * @param productId
     *            product id which is the unique key of a product
     * @param cardId
     *            optional parameter. If the external shopping list service
     *            allow status update, then we don't need to mark the purchase
     *            status to Card table
     */
    public void unMarkProduct(String userId, String shoppingListId, String productId, String cardId);
    
    
    public String getShoppingListName(String userId, String shoppingListId);
    
    public boolean refreshData(String userId);
}
