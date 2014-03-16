package com.rightcode.shoppinglist.glass.oath;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.rightcode.shoppinglist.glass.util.PMF;

/**
 * When deploy the sample app to GAE, found data can not be persistent in memory. 
 * This class is to store the credential to database.
 * 
 */
public class ListableDBCredentialStore implements CredentialStore {

    /**
     * Lock on access to the store.
     */
    private final Lock lock = new ReentrantLock();

    private static final Logger LOG = Logger.getLogger(ListableDBCredentialStore.class.getSimpleName());

    public void store(String userId, Credential credential) {
        lock.lock();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            CredentialModel item = (CredentialModel) pm.getObjectById(CredentialModel.class, userId);
            item.setAccessToken(credential.getAccessToken());
            item.setRefreshToken(credential.getRefreshToken());
            item.setExpirationTimeMillis(credential.getExpirationTimeMilliseconds());
            LOG.info("-----Credential is updated for:" + userId + " NewToken:" + credential.getAccessToken());
        } catch (JDOObjectNotFoundException e) {
            CredentialModel newItem = new CredentialModel(userId, credential.getAccessToken(),
                    credential.getRefreshToken(), credential.getExpirationTimeMilliseconds());
            pm.makePersistent(newItem);
            LOG.info("-----Credential is created for:" + userId + " Token:" + credential.getAccessToken());
        } catch (Exception e){ //Avoid breaking the entire flow
            LOG.log(Level.SEVERE, "Unexpected error occur, might happen after you re-deploy appliation with new proejct id, try access again",e);
        } finally {
            pm.close();
            lock.unlock();
        }
    }

    public void delete(String userId, Credential credential) {
        lock.lock();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            CredentialModel item = (CredentialModel) pm.getObjectById(CredentialModel.class, userId);
            pm.deletePersistent(item);
            LOG.info("-----Credential is deleted for:" + userId + " Token:" + credential.getAccessToken());
        } catch (JDOObjectNotFoundException e) {
            LOG.warning("-----Can not find credential for user:" + userId);
        } finally {
            pm.close();
            lock.unlock();
        }
    }

    public boolean load(String userId, Credential credential) {
        lock.lock();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            CredentialModel item = (CredentialModel) pm.getObjectById(CredentialModel.class, userId);
            credential.setAccessToken(item.getAccessToken());
            credential.setRefreshToken(item.getRefreshToken());
            credential.setExpirationTimeMilliseconds(item.getExpirationTimeMillis());
        } catch (JDOObjectNotFoundException e) {
            LOG.warning("-----Credential can not be loaded for:" + userId + credential.getAccessToken());
            return false;
        } finally {
            pm.close();
            lock.unlock();
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public List<String> listAllUsers() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select userId from " + CredentialModel.class.getName());
        List<String> result = null;
        try {
            result = (List<String>) q.execute();
        } finally {
            pm.close();
        }
        return result;
    }

}