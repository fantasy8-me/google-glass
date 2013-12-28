package com.rightcode.shoppinglist.glass.dao;

import javax.jdo.PersistenceManager;

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

}
