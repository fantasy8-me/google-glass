package com.rightcode.shoppinglist.glass.model;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

/**
 * 
 * @author me
 * 
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class ServiceCache {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;
    
    @Persistent
    private String userId;
    
    @Persistent
    private String projectClientId; 
    
    @Persistent
    private String currentService;

    @Persistent
    private Text cachedListData;
    
    //Cache the ShoppingListCollection message
    @Persistent
    private Text cachedListNames;

    public ServiceCache(String userId, String projectClientId,String currentService, String cachedListData, String cachedListNames) {
        this.userId = userId;
        this.currentService = currentService;
        this.projectClientId = projectClientId;
        this.cachedListData = new Text(cachedListData);
        this.cachedListNames = new Text(cachedListNames);
    }

    
    public String getCurrentService() {
        return currentService;
    }

    public void setCurrentService(String currentService) {
        this.currentService = currentService;
    }


    public String getCachedListData() {
        return cachedListData.getValue();
    }


    public void setCachedListData(String cachedMessage) {
        this.cachedListData = new Text(cachedMessage);
    }


    public String getCachedListNames() {
        return cachedListNames.getValue();
    }


    public void setCachedListNames(String cachedListNames) {
        this.cachedListNames = new Text(cachedListNames);
    }

}
