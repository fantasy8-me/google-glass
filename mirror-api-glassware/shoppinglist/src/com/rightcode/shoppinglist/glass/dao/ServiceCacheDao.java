package com.rightcode.shoppinglist.glass.dao;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.rightcode.shoppinglist.glass.model.ServiceCache;
import com.rightcode.shoppinglist.glass.util.PMF;

/**
 * In GAE, we can not make use of memory to cache data, so once we got the message from external source,
 * we need to cache it to database through this DAO class
 *
 */
public class ServiceCacheDao {

    private static final Logger LOG = Logger.getLogger(ServiceCacheDao.class.getSimpleName());

    private static ServiceCacheDao serviceCacheDao = null;
    
    /**
     * google project client id, which is related to oauth authorization
     */    
    private String projectClientId = null;

    private ServiceCacheDao() {
        URL resource = CardDao.class.getResource("/oauth.properties");

        Properties authProperties = null;
        try {
            File propertiesFile = new File(resource.toURI());
            FileInputStream authPropertiesStream = new FileInputStream(propertiesFile);
            authProperties = new Properties();
            authProperties.load(authPropertiesStream);

        } catch (Exception e) {
            LOG.severe("Can not load oauth in DAO");
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        projectClientId = authProperties.getProperty("client_id");
        if (projectClientId != null)
            projectClientId = projectClientId.trim();

        LOG.info("Got oauth file and client id is:" + projectClientId);
    }

    public synchronized static ServiceCacheDao getInstance() {
        if (serviceCacheDao == null) {
            serviceCacheDao = new ServiceCacheDao();
        }
        return serviceCacheDao;
    }

    /**
     * Cache the message to database. Only message from external will be cached
     * @param userId
     * @param serviceType
     * @param cachedMessage
     * @param cachedListNames
     */
    public void storeServiceCache(String userId,String serviceType, String cachedMessage, String cachedListNames) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            ServiceCache sc = getRecord(userId,pm);
            if (sc == null) {
                sc = new ServiceCache(userId, projectClientId, serviceType, cachedMessage, cachedListNames);
            } else {
                sc.setCurrentService(serviceType);
                sc.setCachedListData(cachedMessage);
                sc.setCachedListNames(cachedListNames);
            }
            pm.makePersistent(sc);
        } finally {
            pm.close();
        }
    }

    @SuppressWarnings("unchecked")
    private ServiceCache getRecord(String userId,PersistenceManager pm) {
        Query q = pm.newQuery(ServiceCache.class);
        q.setFilter("userId == '"+ userId +"' && projectClientId == '" + projectClientId +"'");
        List<ServiceCache> result = null;
        result = (List<ServiceCache>) q.execute();
        if (result != null && result.size() != 0) {
            return result.get(0);
        } else {
            return null;
        }
    }

    public ServiceCache getRecord(String userId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        ServiceCache sc = null;
        try {
            sc = getRecord(userId, pm);
        } finally {
            pm.close();
        }
        return sc;
    }
}
