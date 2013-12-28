package com.rightcode.shoppinglist.glass.util;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public final class PMF {
    private static final PersistenceManagerFactory pmfInstance =
        JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PMF() {}

    public static PersistenceManagerFactory get() {
        return pmfInstance;
    }
    
    public static void main(String[] args) {
//    	Card c = new Card("itemid","userid","Bundle","prdNum");
//    	PersistenceManager pm = PMF.get().getPersistenceManager();
//    	try {
//    		pm.makePersistent(c);
//    	} finally {
//    		pm.close();
//    	}
	}
}