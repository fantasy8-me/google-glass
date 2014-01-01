package com.rightcode.shoppinglist.glass.service;

import java.util.List;
import java.util.Map;

public interface ShoppingListProvider {

    /**
     * To get a list of product by specify user id and category id.
     * 
     * Meta data of category are defined in category.json
     * 
     * @param userId
     *            google user id
     * @param category
     *            category id
     * @return List of product in specified category
     */
    public List<Map<String, Object>> getShoppingList(String userId, String category);

    /**
     * Get all products in shopping list, product are group by category and
     * stored in a Map
     * 
     * Refer to productData.json to understand the data structure
     * 
     * @param userId
     *            google user id
     * @return All products in a Map, key of the map is category id.
     */
    public Map<String, List<Map<String, Object>>> getShoppingList(String userId);

    /**
     * @param userId google user id
     * @param productNum product number, unique key of a product
     * @return
     */
    public Map<String, Object> getProductData(String userId, int productNum);
    
    
    /**
     * Mark the purchase status to true
     * 
     * Without the external service to store the purchase statue, we have to
     * store the purchase status in our own database, along with the product
     * card.
     * 
     * @param userId
     *            google user id
     * @param productNum
     *            product number , unique key of a product
     * @param cardId
     *            optional parameter. If the external shopping list service
     *            allow status update, then we don't need to mark the purchase
     *            status to Card table
     * 
     */
    public void markProduct(String userId, int productNum, String cardId);

    /**
     * Mark the purchase status to false
     * 
     * Without the external service to store the purchase statue, we have to
     * store the purchase status in our own database, along with the product
     * card.
     * 
     * @param userId
     *            google user id
     * @param productNum
     *            product number which is the unique key of a product
     * @param cardId
     *            optional parameter. If the external shopping list service
     *            allow status update, then we don't need to mark the purchase
     *            status to Card table
     */
    public void unMarkProduct(String userId, int productNum, String cardId);

}
