package com.rightcode.shoppinglist.glass.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.rightcode.shoppinglist.glass.dao.CardDao;
import com.rightcode.shoppinglist.glass.ref.MainServlet;

/**
 * General Utilities
 * 
 */
public class Util {
    
    private static final Logger LOG = Logger.getLogger(Util.class.getSimpleName());

    /**
     * ssl link is required for subscription call back. This method is used to put all the 
     * ssl generation logic in one place for better maintenance
     * @param req
     * @return
     */
    public static String buildSubscriptionCallBack(HttpServletRequest req) {
        String serverName = req.getServerName();
        String subscription_callback = null;
        if (serverName.equals("localhost")) {
            subscription_callback = "https://localglassware.ngrok.com/notify";
        } else if (serverName.equals("glass.rightcode.co.il")) {
            // if ssl setting of glass.rightcode.co.il is ready, we can use 
            //https://glass.rightcode.co.il then remove this else if logic 
            subscription_callback = "https://glassshoppinglist.appspot.com/notify";
        } else {
            subscription_callback = "https://" + serverName + "/notify";
        }
        LOG.info("-----subscription_callback:" + subscription_callback);
        return subscription_callback;
    }
    
    /**
     * Get project client id from config file
     * 
     * @return current project client id
     */
    public static String getProjectClientId(){
        URL resource = Util.class.getResource("/oauth.properties");

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
        String projectClientId = authProperties.getProperty("client_id");
        if (projectClientId != null)
            return projectClientId;
        else
            return "unknown client id";
    }

}
