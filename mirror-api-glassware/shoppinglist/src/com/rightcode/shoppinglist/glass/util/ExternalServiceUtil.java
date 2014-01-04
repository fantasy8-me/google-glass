package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.rightcode.shoppinglist.glass.service.DemoShoppingListProvider;

public class ExternalServiceUtil {
    
    private static final Logger LOG = Logger.getLogger(ExternalServiceUtil.class.getSimpleName());
    
    public static List<Map<String,Object>> getAllShoppingLists() throws IOException{
        
        JsonFactory jsonFactory = new JacksonFactory();
        List<Map<String,Object>> data = null;
            
        data = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData_external.json"),null);
        
        LOG.info("Exteranl data is got successfully, total number of shopping list:" + (data==null ? 0 : data.size()));
        return data;
    }
    
    public static Map<String,Object> getShoppnigList(String userId, String shoppingListId) throws IOException{
        return null;
    }

}
