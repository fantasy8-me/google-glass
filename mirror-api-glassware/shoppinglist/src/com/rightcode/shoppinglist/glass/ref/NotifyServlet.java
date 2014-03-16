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
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.Notification;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.api.services.mirror.model.UserAction;
import com.rightcode.shoppinglist.glass.AppController;
import com.rightcode.shoppinglist.glass.Constants;

/**
 * Handles the notifications sent back from subscriptions, modify based on the version in google sample app
 * 
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class NotifyServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(NotifyServlet.class.getSimpleName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        LOG.info("**********Great, got a notification");
        BufferedReader notificationReader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String notificationString = "";

        String sCurrentLine;
        int lines = 0;
        while ((sCurrentLine = notificationReader.readLine()) != null) {
            notificationString += sCurrentLine;
            lines++;
            if (lines > 1000) {
                throw new IOException("Attempted to parse notification payload that was unexpectedly long.");
            }
        }

        LOG.info("got raw notification [" + notificationString + "]");

        // Respond with OK and status 200 in a timely fashion to prevent re-delivery
        response.setContentType("text/html");
        Writer writer = response.getWriter();
        writer.append("OK");
        writer.close();

        JsonFactory jsonFactory = new JacksonFactory();

        Notification notification = jsonFactory.fromString(notificationString, Notification.class);

        LOG.info("Got a notification with ID: " + notification.getItemId());

        // Figure out the impacted user and get their credentials for API calls
        String userId = notification.getUserToken();
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);

        if (notification.getCollection().equals("timeline")) {
            // Get the impacted timeline item
            TimelineItem timelineItem = null;
            // Even the item is deleted from timeline. below method won't return error but a incomplete item(with id and bundle_id, but not
            // html, text)
            try {
                timelineItem = mirrorClient.timeline().get(notification.getItemId()).execute();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, t.getMessage(), t);
                throw new RuntimeException(t);
            }
            LOG.info("Notification impacted timeline item with ID: " + timelineItem.getId());
            if (notification.getUserActions() != null) {
                LOG.info("--------Action type & payload:" + notification.getUserActions().get(0).getType() + ":"
                        + notification.getUserActions().get(0).getPayload());
                if (notification.getUserActions().get(0).getType().equals("CUSTOM")) {
                    UserAction ua = notification.getUserActions().get(0);
                    AppController appController = AppController.getInstance();
                    if (Constants.MENU_ID_MARK.equals(ua.getPayload())) {
                        appController.markOrUnMarkProduct(mirrorClient, userId, timelineItem, true);
                    } else if (Constants.MENU_ID_UNMARK.equals(ua.getPayload())) {
                        appController.markOrUnMarkProduct(mirrorClient, userId, timelineItem, false);
                    } else if (Constants.MENU_ID_STARTSHOPPING.equals(ua.getPayload())) {
                        appController.actionStartShopping(userId, timelineItem.getId());
                    } else if (Constants.MENU_ID_IC_FETCH.equals(ua.getPayload())) {
                        appController.actionFetchShoppingLists(userId, timelineItem.getId());
                    } else if (Constants.MENU_ID_FINISHSHOPPING.equals(ua.getPayload())) {
                        appController.actionFinishShopping(userId, timelineItem.getId());
                    } else if (Constants.MENU_ID_IC_RESTART.equals(ua.getPayload())) {
                        appController.actionRestart(userId, timelineItem.getId());
                    } else if (Constants.MENU_ID_IC_REFRESH.equals(ua.getPayload())) {
                        appController.actionRefresh(userId);
                    }
                } else {
                    LOG.warning("I don't know what to do with this notification, so I'm ignoring it.");
                }
            } else {
                LOG.warning("Update is not triggered by any user action, so I'm ignoring it.");
            }
        }
        LOG.info("*****Notification flow done");
    }
}
