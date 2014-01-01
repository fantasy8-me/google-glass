package com.rightcode.shoppinglist.glass.util;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.rightcode.shoppinglist.glass.ref.MainServlet;

/**
 * General Utilities
 * 
 * @author me
 * 
 */
public class Util {
    
    private static final Logger LOG = Logger.getLogger(Util.class.getSimpleName());

    public static String buildSubscriptionCallBack(HttpServletRequest req) {
        String serverName = req.getServerName();
        String subscription_callback = null;
        if (serverName.equals("localhost")) {
            subscription_callback = "https://localglassware.ngrok.com/notify";
        } else if (serverName.equals("glass.rightcode.co.il")) {
            // Eric.TODO, after the ssl enabled, remove this special handling
            subscription_callback = "https://glassshoppinglist.appspot.com/notify";
        } else {
            subscription_callback = "https://" + serverName + "/notify";
        }
        LOG.info("-----subscription_callback:" + subscription_callback);
        return subscription_callback;
    }

}
