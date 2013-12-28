package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.jsp.ah.inboundMailBody_jsp;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.rightcode.shoppinglist.glass.Constants;

public class ReferenceDataManager {
	
	private static ReferenceDataManager refDataManager = null;
	
	private Map<String, Map<String,Object>> categorySetting = null;
	
	private static final Logger LOG = Logger.getLogger(ReferenceDataManager.class.getSimpleName());
	
	private ReferenceDataManager() {
		JsonFactory jsonFactory = new JacksonFactory();
		try {
			this.categorySetting = jsonFactory.fromInputStream(ReferenceDataManager.class.getResourceAsStream("/category.json"),
					null);
		} catch (IOException e) {
			LOG.severe("Can not init category data");
			e.printStackTrace();
		}
	}
	
	public synchronized static ReferenceDataManager getInstance(){
		if(refDataManager == null){
			refDataManager = new ReferenceDataManager();
		}
		return refDataManager;
	}
	
	/**
	 * @param category ref to category name defined com.rightcode.shoppinglist.glass.Contants.
	 * @return the setting Map
	 */
	public Map<String, Object> getCategorySetting(String category){
		return categorySetting.get(category);
	}
	
	public Map<String, String> getCategoryTitleMap(){
	    Map titleMap = new HashMap<String, String>();
	    Iterator<String> iter  = categorySetting.keySet().iterator();
	    while (iter.hasNext()) {
          String categoryId = (String) iter.next();
          titleMap.put(categoryId, categorySetting.get(categoryId).get("title"));
        }
	    return titleMap;
	}
	
	public static void main(String[] args) {
		System.out.println(ReferenceDataManager.getInstance().getCategorySetting("category1"));
	}
}
