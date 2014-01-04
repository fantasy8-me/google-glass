package com.rightcode.shoppinglist.glass.dao;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.model.Card;
import com.rightcode.shoppinglist.glass.util.PMF;

/**
 * @author me
 * 
 */
public class CardDao {

    private static final Logger LOG = Logger.getLogger(CardDao.class.getSimpleName());

    private static CardDao cardDao = null;

    private String projectClientId = null;

    private CardDao() {

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

    public synchronized static CardDao getInstance() {
        if (cardDao == null) {
            cardDao = new CardDao();
        }
        return cardDao;
    }

    public void insertCard(String cardId, String userId, String type, String ref, String shoppingListCardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Card c = new Card(cardId, userId, this.projectClientId, type, ref, shoppingListCardId);
        try {
            pm.makePersistent(c);
        } finally {
            pm.close();
        }
    }

    public String getCardRefById(String userId, String cardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select ref from " + Card.class.getName());
        q.setFilter("userId == userIdParm && cardId == cardIdParm && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm, String cardIdParm");
        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(new String[] { userId, cardId });
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
    }

    public String getShoppingListCardByProductCard(String userId, String cardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select shoppingListCardId from " + Card.class.getName());
        q.setFilter("userId == userIdParm && cardId == cardIdParm && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm, String cardIdParm");
        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(new String[] { userId, cardId });
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
    }

    public String getBundleCoverCardId(String userId, String bundleId, String shoppingListCardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select cardId from " + Card.class.getName());
        q.setFilter("userId == userIdParm && ref == bundleIdParm && shoppingListCardId == shoppingListCardIdParam && type == '"
                + Constants.CARD_TYPE_BUNDLE + "' && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm, String bundleIdParm, String shoppingListCardIdParam");
        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(new String[] { userId, bundleId, shoppingListCardId });
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
    }

    public Map<String, Boolean> getPurchaseStatus(String userId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select ref,isPurchased from " + Card.class.getName());
        q.setFilter("userId == userIdParm && type == '" + Constants.CARD_TYPE_PRODUCT + "' && projectClientId=='"
                + this.projectClientId + "'");
        q.declareParameters("String userIdParm");
        List<Object[]> result = null;
        Map<String, Boolean> resultMap = new HashMap<String, Boolean>();
        try {
            result = (List<Object[]>) q.executeWithArray(userId);
            for (int i = 0; i < result.size(); i++) {
                Object[] record = result.get(i);
                resultMap.put(((String) record[0]).trim(), (Boolean) record[1]);
            }
        } finally {
            pm.close();
        }
        return resultMap;
    }

    public void markPurchaseStatus(String userId, String cardId, boolean markPurchase) {

        PersistenceManager pm = PMF.get().getPersistenceManager();
        Transaction trans = pm.currentTransaction();
        try {
            trans.begin();
            Card card = (Card) pm.getObjectById(Card.class, cardId);
            card.setPurchased(markPurchase);
            trans.commit();
        } finally {
            if (trans.isActive()) {
                trans.rollback();
            }
            pm.close();
        }
    }

    /**
     * 
     * @param userId
     * @return list of bundle cover card id for specified user
     */
    // public List<String> getAllBundleConvers(String userId) {
    // PersistenceManager pm = PMF.get().getPersistenceManager();
    // Query q = pm.newQuery("select cardId from " + Card.class.getName());
    // q.setFilter("userId == userIdParm && type == '" +
    // Constants.CARD_TYPE_BUNDLE + "' && projectClientId=='"
    // + this.projectClientId + "'");
    // q.declareParameters("String userIdParm");
    //
    // List<String> result = null;
    // try {
    // result = (List<String>) q.execute(userId);
    // } finally {
    // pm.close();
    // }
    // return result;
    // }

    /**
     * 
     * To get the number of card for specified user, can be used to determine
     * whether the app is initialized for the user
     * 
     * @param userId
     * @return number of cards for user
     */
    public int getNumberOfCards(String userId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        // Eric.TODO, can not find the proper count approach in JDO, will double
        // check later
        Query q = pm.newQuery("select cardId from " + Card.class.getName());
        q.setFilter("userId == userIdParm && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm");
        List<String> result = null;
        try {
            result = (List<String>) q.execute(userId);
        } finally {
            pm.close();
        }
        return result.size();
    }

    /**
     * To get cards for specified user by type
     * 
     * @param userId
     * @param type
     * @return
     */
    public List<String> getCardsByType(String userId, String type, String shoppingListCardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery("select cardId from " + Card.class.getName());

        String filterStr = "userId == userIdParm && projectClientId=='" + this.projectClientId +"'";
        String declareParamStr = "String userIdParm";
        List<String> params = new ArrayList<String>();
        params.add(userId);

        if (type != null) {
            filterStr += " && type == typeParm";
            declareParamStr += ", String typeParm";
            params.add(type);
        }
        if (shoppingListCardId != null) {
            filterStr += " && shoppingListCardId == shoppingListCardIdParm";
            declareParamStr += ", String shoppingListCardIdParm";
            params.add(shoppingListCardId);
        }
        q.setFilter(filterStr);
        q.declareParameters(declareParamStr);

        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(params.toArray());
        } finally {
            pm.close();
        }
        return result;
    }

    /**
     * Delete a card
     * 
     * @param cardId
     */
    public void deleteCard(String cardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Card card = (Card) pm.getObjectById(Card.class, cardId);
            pm.deletePersistent(card);
            LOG.severe("------Deleted card:" + cardId);
        } catch (JDOObjectNotFoundException e) {
            LOG.warning("Can not find the card ["
                    + cardId
                    + "] in db, this might not be an error if you are deleting a non-shoppinglist card, or the data was cleannd up by others");
        } catch (Exception e) {
            // Catch the exception to avoid breaking the main flow
            LOG.severe("Error occur when delete card:" + cardId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            pm.close();
        }
    }

}
