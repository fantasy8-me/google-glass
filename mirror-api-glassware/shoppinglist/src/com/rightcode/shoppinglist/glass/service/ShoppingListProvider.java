package com.rightcode.shoppinglist.glass.service;

import java.util.List;
import java.util.Map;

public interface ShoppingListProvider {
    
    
    
    /**
     * First Level Key is shopping list 
     * 
     * @param userId
     * @return all shopping list of user
     */
    public Map<String, List<Map<String, Object>>> getAllShoppingLists(String userId);    

    /**
     * Get all products in shopping list
     * 
     * Refer to productData.json to understand the data structure
     * 
     * @param userId
     *            google user id
     * @param shoppingListId
     *            id of the shopping list
     * @return the shopping list.
     */
    public List<Map<String, Object>> getShoppingList(String userId, String shoppingListId);

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
    
    public int fetchShoppingLists(String userId);
}
