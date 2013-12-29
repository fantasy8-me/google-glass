package com.rightcode.shoppinglist.glass.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.model.Card;
import com.rightcode.shoppinglist.glass.util.PMF;

public class CardDao {

    private static CardDao cardDao = null;

    private CardDao() {

    }

    public synchronized static CardDao getInstance() {
        if (cardDao == null) {
            cardDao = new CardDao();
        }
        return cardDao;
    }

    public void insertCard(String cardId, String userId, String type, String ref) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Card c = new Card(cardId, userId, type, ref);
        try {
            pm.makePersistent(c);
        } finally {
            pm.close();
        }
    }

    public String getProdutNumByCardId(String userId, String cardId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select ref from " + Card.class.getName());
        q.setFilter("userId == userIdParm && cardId == cardIdParm");
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

    public String getShoppingListCardId(String userId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select cardId from " + Card.class.getName());
        q.setFilter("userId == userIdParm && type == '" + Constants.CARD_TYPE_MAIN + "'");
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
    public String getBundleCoverCardId(String userId, String bundleId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select cardId from " + Card.class.getName());
        q.setFilter("userId == userIdParm && ref == bundleIdParm && type == '" + Constants.CARD_TYPE_BUNDLE + "'");
        q.declareParameters("String userIdParm, String bundleIdParm");
        List<String> result = null;
        try {
            result = (List<String>) q.executeWithArray(new String[]{userId,bundleId});
        } finally {
            pm.close();
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return "";
        }
    }
    
    public Map<String,Boolean> getPurchaseStatus(String userId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery("select ref,isPurchased from " + Card.class.getName());
        q.setFilter("userId == userIdParm && type == '" + Constants.CARD_TYPE_PRODUCT + "'");
        q.declareParameters("String userIdParm");
        List<Object[]> result = null;
        Map<String,Boolean> resultMap = new HashMap<String, Boolean>();
        try {
            result = (List<Object[]>) q.executeWithArray(userId);
            for (int i = 0; i < result.size(); i++) {
                Object[] record = result.get(i);
                resultMap.put(((String)record[0]).trim(), (Boolean)record[1]);
            }
        } finally {
            pm.close();
        }
        return resultMap;
    }

    public void markPurchaseStatus(String userId, String cardId, boolean markPurchase){
        
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Transaction trans = pm.currentTransaction();
        try{ 
          trans.begin();
          Card card = (Card) pm.getObjectById(Card.class,cardId);
          card.setPurchased(markPurchase);
          trans.commit();
        }finally{
          if (trans.isActive()) {
            trans.rollback();
          }
          pm.close();
        }
    }
}
