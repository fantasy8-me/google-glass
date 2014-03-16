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
 * DAO class of Card model
 *
 */
public class CardDao {

    private static final Logger LOG = Logger.getLogger(CardDao.class.getSimpleName());

    private static CardDao cardDao = null;

    /**
     * google project client id, which is related to oauth authorization
     */
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

    /**
     * Insert a card to Card table
     * 
     * @param cardId
     * @param userId
     * @param type
     * @param ref check definicaton in model {@link Card}
     * @param shoppingListCardId
     */
    public void insertCard(String cardId, String userId, String type, String ref, String shoppingListCardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Card c = new Card(cardId, userId, this.projectClientId, type, ref, shoppingListCardId);
        try {
            pm.makePersistent(c);
        } finally {
            pm.close();
        }
    }

    @SuppressWarnings("unchecked")
    public String getCardRefById(String userId, String cardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select ref from " + Card.class.getName());
        q.setFilter("userId == userIdParm && cardId == cardIdParm && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm, String cardIdParm");
        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(new Object[] { userId, cardId });
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public String getShoppingListCardByProductCard(String userId, String cardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select shoppingListCardId from " + Card.class.getName());
        q.setFilter("userId == userIdParm && cardId == cardIdParm && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm, String cardIdParm");
        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(new Object[] { userId, cardId });
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
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
     * To get the number of card for specified user, can be used to determine
     * whether the app is initialized for the user
     * 
     * @param userId
     * @return number of cards for user
     */
    @SuppressWarnings("unchecked")
    public int getNumberOfCards(String userId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        // Can not find the proper count approach in JDO
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
    @SuppressWarnings("unchecked")
    public List<String> getCardsByType(String userId, String type, String shoppingListCardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery("select cardId from " + Card.class.getName());

        String filterStr = "userId == userIdParm && projectClientId =='" + this.projectClientId +"'";
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

    @SuppressWarnings("unchecked")
    public String getBundleIdFromListCoverCard(String userId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select ref from " + Card.class.getName());
        q.setFilter("userId == userIdParm && type == '" + Constants.CARD_TYPE_LIST_COVER + "' && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm");
        List<String> result = null;
        try {
            result = (List<String>) q.execute(userId);
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
    }
    
    /**
     * Get the card id by ref, usually, ref is the prodcut id or shopping list id
     * @param userId
     * @param id
     * @param type 
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getCardIdByRef(String userId, String id, String type) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select cardId from " + Card.class.getName());
        q.setFilter("userId == userIdParm && ref == idParm && type == typeParm" + " && projectClientId=='" + this.projectClientId + "'");
        q.declareParameters("String userIdParm, String idParm, String typeParm");
        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(new Object[] { userId, id, type });
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
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
            LOG.info("------Deleted card:" + cardId);
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
