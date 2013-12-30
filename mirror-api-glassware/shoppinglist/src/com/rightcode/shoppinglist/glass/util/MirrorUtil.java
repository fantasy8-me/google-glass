package com.rightcode.shoppinglist.glass.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.services.mirror.Mirror;

public final class MirrorUtil {
    
    private static final Logger LOG = Logger.getLogger(MirrorUtil.class.getSimpleName());
    
    /**
     * Touch a card by "PATCH" method to bring the card to front
     * @param mirrorClient
     * @param cardId
     */
    public static void touchCard(Mirror mirrorClient, String cardId){
        try {
            mirrorClient.timeline().patch(cardId, null).execute();
        } catch (IOException e) {
            LOG.severe("Error occur when touch card:" + cardId);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
