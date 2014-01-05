package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.tools.ant.filters.StringInputStream;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.service.DemoShoppingListProvider;

public class ExternalServiceUtil {
    
    private static final Logger LOG = Logger.getLogger(ExternalServiceUtil.class.getSimpleName());
    
    private final static String DFT_IMG = "http://i.imgur.com/ZWChwLu.png";
    
    public static List<Map<String,Object>> getAllShoppingLists() throws IOException{
        
        JsonFactory jsonFactory = new JacksonFactory();
        Map<String, String> data = null;
            
        data = jsonFactory.fromInputStream(DemoShoppingListProvider.class.getResourceAsStream("/productData_external_DetailsLists.json"),null);
//        Map<String,Object> list = jsonFactory.fromInputStream(new StringInputStream(data.get("61c368e0-4951-446c-9857-a291015149c2")),null);
//        Map<String,Object> list = jsonFactory.fromInputStream(new StringInputStream(data.get("7c37d977-ad5d-40a2-a5ec-a2a8008d2591")),null);
        Map<String,Object> list = jsonFactory.fromInputStream(new StringInputStream(data.get("d17bb2da-ce13-4eea-99d4-a2a800b68217")),null);
        
        List<Map<String,Object>> lines = (List<Map<String,Object>>)list.get("Lines");
        
        List<Map<String,Object>> newList = new ArrayList<Map<String,Object>>();
        for (int i = 0; i < lines.size(); i++) {
            Map<String,Object> productFromExt = lines.get(i);
            newList.add(convertProduct(productFromExt));
        }
        LOG.info(jsonFactory.toPrettyString(newList));
        
        LOG.info("Exteranl data is got successfully, total number of shopping list:" + (data==null ? 0 : data.size()));
        return null;
    }
    
    public static Map<String,Object> getShoppnigList(String userId, String shoppingListId) throws IOException{
        return null;
    }
    
    private static Map<String,Object>  convertProduct(Map<String,Object> productFromExternal){
        Map<String,Object> product = new HashMap<String, Object>();
        
        product.put(Constants.ITEM_COL_PRD_ID,productFromExternal.get("Id"));
        product.put(Constants.ITEM_COL_PRDNAME,extractDescription((Map<String,Object>)productFromExternal.get("Item")));
        
        String img = (String)productFromExternal.get("Image");
        if(img == null || img.isEmpty()){
            img = DFT_IMG;
        }
        product.put(Constants.ITEM_COL_IMGURL,img);
        
        product.put(Constants.ITEM_COL_QUANTITY,productFromExternal.get("Quantity"));
        
        product.put(Constants.ITEM_COL_PRICE,extractPrice((Map<String,Object>)productFromExternal.get("Item")));
        
        product.put(Constants.ITEM_COL_PROMO,extractPromotion((Map<String,Object>)productFromExternal.get("Item")));
        
//        product.put(Constants.ITEM_COL_CATEGORY,null);
        
        return product;
        
    }
    
    private static String extractPrice(Map<String,Object> itemDetailsMap){
        String prefix = "$";
        String currency = ((Map<String,String>)itemDetailsMap.get("Price")).get("Currency");
        if(currency != null && currency.equals("GBP")){
            prefix = "\u20A4";
        }
        return prefix + " " +((Map<String,String>)itemDetailsMap.get("Price")).get("Value");
    }
    
    private static String extractDescription(Map<String,Object> itemDetailsMap){
        List<Map<String,String>> descs = (List<Map<String,String>>)itemDetailsMap.get("Description");
        String longName = null;
        String shortName = null;
        for (int i = 0; i < descs.size(); i++) {
            Map<String,String> desc = descs.get(i);
            if("Long".equals(desc.get("TypeCode"))){
                longName = desc.get("Value");
            }else if("Short".equals(desc.get("TypeCode"))){
                shortName = desc.get("Value");
            }
        }
        if(longName != null){
            return longName;
        }else if(shortName != null){
            return shortName;
        }else{
            return "UnKnow Product";
        }
    }

    private static String extractPromotion(Map<String,Object> itemDetailsMap){
        List<Map<String,Object>> proms = (List<Map<String,Object>>)itemDetailsMap.get("Promotions");
        
        String longName = null;
        String shortName = null;
        
        if(proms != null && proms.size() > 0){
            Map<String,Object> prom = (Map<String,Object>)proms.get(0);
            List<Map<String,String>> descs = (List<Map<String,String>>)prom.get("Description");

            for (int i = 0; i < descs.size(); i++) {
                Map<String,String> desc = descs.get(i);
                if("Long".equals(desc.get("TypeCode"))){
                    longName = desc.get("Value");
                }else if("Short".equals(desc.get("TypeCode"))){
                    shortName = desc.get("Value");
                }
            }
        }
        if(longName != null){
            return longName;
        }else if(shortName != null){
            return shortName;
        }else{
            return null;
        }
    }
    
    public static void main(String[] args) throws IOException {
        ExternalServiceUtil.getAllShoppingLists();
    }
}
