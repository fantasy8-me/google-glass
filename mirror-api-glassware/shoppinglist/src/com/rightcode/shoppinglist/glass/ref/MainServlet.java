/*
 * Copyright (C) 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.rightcode.shoppinglist.glass.ref;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.Command;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.common.collect.Lists;
import com.rightcode.shoppinglist.glass.AppController;
import com.rightcode.shoppinglist.glass.Constants;
import com.rightcode.shoppinglist.glass.dao.CardDao;
import com.rightcode.shoppinglist.glass.util.ConnectivityTester;
import com.rightcode.shoppinglist.glass.util.Util;

/**
 * Handles POST requests from index.jsp
 * 
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class MainServlet extends HttpServlet {

    /**
     * Private class to process batch request results.
     * <p/>
     * For more information, see
     * https://code.google.com/p/google-api-java-client/wiki/Batch.
     */

    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());

    /**
     * Do stuff when buttons on index.jsp are clicked
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String userId = AuthUtil.getUserId(req);
        Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);
        LOG.info("*****UserId[" + userId +"]" +"  Operation[" + req.getParameter("adminOperation") + "]  Credential[" + credential.getAccessToken() + "]");
        AppController appController = AppController.getInstance();

        String message = "";

        if (req.getParameter("adminOperation").equals("rawhttp")) {
            if (!req.getParameter("jsonMsg").isEmpty()) {
                try {
                    String url = getUrl(req.getParameter("url"));
                    LOG.info("JSON Msg: \nurl:" + url + "\n" + req.getParameter("jsonMsg"));

                    message = sendJson(req.getParameter("jsonMsg"), credential.getAccessToken(), url);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, e.getMessage(), e);
                    message = e.getMessage();
                }
            }
        } else if (req.getParameter("adminOperation").equals("admin_initialShoppingListApp")) {
            if (CardDao.getInstance().getNumberOfCards(userId) == 0) {
                int result = appController.initApp(userId, Constants.SERVICE_TYPE_DUMMY);
                if (Constants.INIT_APP_RESULT_FAIL != result)
                    message = "We have initialized our glassware in you glass with local data, you should able to see a intial card created for you, pin it and use it to start shopping";
                else {
                    message = "Fail to initialize the glassware, please check log";
                }
            } else {
                appController.bringICToFront(userId);
                message = "You initialized our glassware before, we just bring your initial card to front";
            }
        } else if (req.getParameter("adminOperation").equals("admin_initialShoppingListAppFromExternal")) {
            if (CardDao.getInstance().getNumberOfCards(userId) == 0) {
                int result = appController.initApp(userId, Constants.SERVICE_TYPE_EXTERNAL);
                if (Constants.INIT_APP_RESULT_FAIL == result) {
                    message = "Fail to initialize the glassware, please check log";
                } else if (Constants.INIT_APP_RESULT_SUCCESS_WITH_DUMMY == result) {
                    message = "We have swapped from external server and initialized our glassware in you glass with local data, you should able to see a intial card created for you, pin it and use it to start shopping";
                } else if (Constants.INIT_APP_RESULT_SUCCESS_WITH_EXTERNAL == result) {
                    message = "We have initialized our glassware in you glass with external data, you should able to see a intial card created for you, pin it and use it to start shopping";
                }
            } else {
                appController.bringICToFront(userId);
                message = "You initialized our glassware before, we just bring your initial card to front";
            }
        } else if (req.getParameter("adminOperation").equals("admin_insertCoupon")) {
            appController.insertCoupon(userId, req.getParameter("couponContent"));
            message = "Coupon is created";
        } else if (req.getParameter("adminOperation").equals("admin_cleanCards")) {
            if (appController.cleanUpAllCards(userId, null))
                message = "All your shopping list cards have been removed from your timeline, as well as the data in our application database";
            else
                message = "Fail to clean up all cards, pleaes check log for details";
        } else if (req.getParameter("adminOperation").equals("admin_cleanToken")) {
            // after token clean, user will be redirected to login page
            appController.adminCleanUpToken();
            req.getSession().removeAttribute("userId");
        } else if (req.getParameter("adminOperation").equals("admin_testConn")) {
            if (ConnectivityTester.run())
                message = "Successfully connect to exneral serivce";
            else
                message = "Can not connect to exneral serivce";
        } else if (req.getParameter("adminOperation").equals("admin_insertSubscription")) {

            try {
                String subscription_callback = Util.buildSubscriptionCallBack(req);
                MirrorClient.insertSubscription(credential, subscription_callback, userId,
                        req.getParameter("collection"));
                message = "Application is now subscribed to updates.";
            } catch (GoogleJsonResponseException e) {
                LOG.warning("Could not subscribe " + WebUtil.buildUrl(req, "/notify") + " because "
                        + e.getDetails().toPrettyString());
                message = "Failed to subscribe. Check your log for details";
            }
        } else if (req.getParameter("adminOperation").equals("admin_deleteSubscription")) {
            MirrorClient.deleteSubscription(credential, req.getParameter("subscriptionId"));
            message = "Application has been unsubscribed.";
        } else if (req.getParameter("adminOperation").startsWith("testing")) {
            Mirror mirrorClient = MirrorClient.getMirror(credential);
            if (req.getParameter("operation").equals("testing1")) {
                AppController.getInstance().markItem(mirrorClient, userId, req.getParameter("addInfo"));
            } else {
                AppController.getInstance().unMarkItem(mirrorClient, userId, req.getParameter("addInfo"));
            }
        } else {
            String operation = req.getParameter("adminOperation");
            LOG.warning("Unknown operation specified " + operation);
            message = "I don't know how to do that";
        }
        WebUtil.setFlash(req, message);
        res.sendRedirect(WebUtil.buildUrl(req, "/"));
    }

    /**
     * Make a raw http request with json message
     * 
     * @param jsonStr
     * @param accessToken
     * @return
     * @throws Exception
     */
    public static String sendJson(String jsonStr, String accessToken, String url) throws Exception {

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        // add header
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(jsonStr));

        HttpResponse response = client.execute(post);
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("Response Code : " + statusCode);
        LOG.info("Response Code : " + statusCode);

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer("Response Code : " + statusCode + "\n");
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        System.out.println(result.toString());
        LOG.info("Full Response: " + result.toString());
        return result.toString();
    }

    private String getUrl(String key) {
        if ("timeline".equals(key)) {
            return "https://www.googleapis.com/mirror/v1/timeline";
        } else if ("subscriptions".equals(key)) {
            return "https://www.googleapis.com/mirror/v1/subscriptions";
        } else {// default
            return "https://www.googleapis.com/mirror/v1/timeline";
        }

    }
    /* Shopping List Change */
}
