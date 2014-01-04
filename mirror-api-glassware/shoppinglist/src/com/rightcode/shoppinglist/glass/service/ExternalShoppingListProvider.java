package com.rightcode.shoppinglist.glass.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rightcode.shoppinglist.glass.dao.CardDao;
import com.rightcode.shoppinglist.glass.util.ExternalServiceUtil;

public class ExternalShoppingListProvider implements ShoppingListProvider {

    private Map<String, Map<String, List<Map<String, Object>>>> productData = null;

    private static ExternalShoppingListProvider externalShoppingListProvider = null;

    private static final Logger LOG = Logger.getLogger(DemoShoppingListProvider.class.getSimpleName());

    private CardDao cardDao = null;

    private ExternalShoppingListProvider() {
        try {
            cardDao = CardDao.getInstance();
            // Shopping list will be cached when initialize
            // ExternalShoppingListProvider
            productData = convertExternalDataToInternalFormat(ExternalServiceUtil.getAllShoppingLists());
            
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Can not get data from external service, will switch to DemoShoppinglistProvider");
        }
    };

    private Map<String, Map<String, List<Map<String, Object>>>> convertExternalDataToInternalFormat(List<Map<String,Object>> externalData){
        throw new UnsupportedOperationException("Will implement later");
    }
    
    
    public synchronized static ShoppingListProvider getInstance() {
        if (externalShoppingListProvider == null) {
            externalShoppingListProvider = new ExternalShoppingListProvider();
        }
        return externalShoppingListProvider;
    }

    @Override
    public Map<String, Map<String, List<Map<String, Object>>>> getAllShoppingLists(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Map<String, Object>> getShoppingList(String userId, String shoppingListId, String category) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, List<Map<String, Object>>> getShoppingList(String userId, String shoppingListId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getProductData(String userId, String shoppingListId, String productId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void markProduct(String userId, String shoppingListId, String productId, String cardId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void unMarkProduct(String userId, String shoppingListId, String productId, String cardId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getShoppingListName(String userId, String shoppingListId) {
        // TODO Auto-generated method stub
        return null;
    }


}
