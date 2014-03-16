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

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.Credential;
import com.rightcode.shoppinglist.glass.AppController;
import com.rightcode.shoppinglist.glass.util.ConnectivityTester;

/**
 * Handles POST requests from index.jsp, changed base on google's sample application
 * 
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class MainServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());

    /**
     * Do stuff when buttons on index.jsp are clicked
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String userId = AuthUtil.getUserId(req);
        Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);
        LOG.info("*****UserId[" + userId + "]" + "  Operation[" + req.getParameter("adminOperation") + "]  Credential["
                + credential.getAccessToken() + "]");
        AppController appController = AppController.getInstance();

        String message = "";

        if (req.getParameter("adminOperation").equals("admin_initialShoppingListApp")) {
            message = appController.adminInitApp(userId);
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
        } else {
            String operation = req.getParameter("adminOperation");
            LOG.warning("Unknown operation specified " + operation);
            message = "I don't know how to do that";
        }
        WebUtil.setFlash(req, message);
        res.sendRedirect(WebUtil.buildUrl(req, "/"));
    }
}
