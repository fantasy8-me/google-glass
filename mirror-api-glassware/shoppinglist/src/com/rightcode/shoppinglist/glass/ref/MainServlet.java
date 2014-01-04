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
  private final class BatchCallback extends JsonBatchCallback<TimelineItem> {
    private int success = 0;
    private int failure = 0;

    @Override
    public void onSuccess(TimelineItem item, HttpHeaders headers) throws IOException {
      ++success;
    }

    @Override
    public void onFailure(GoogleJsonError error, HttpHeaders headers) throws IOException {
      ++failure;
      LOG.info("Failed to insert item: " + error.getMessage());
    }
  }

  private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());
  public static final String CONTACT_ID = "com.google.glassware.contact.java-quick-start";
  public static final String CONTACT_NAME = "Java Quick Start";

  private static final String PAGINATED_HTML =
      "<article class='auto-paginate'>"
      + "<h2 class='blue text-large'>Did you know...?</h2>"
      + "<p>Cats are <em class='yellow'>solar-powered.</em> The time they spend napping in "
      + "direct sunlight is necessary to regenerate their internal batteries. Cats that do not "
      + "receive sufficient charge may exhibit the following symptoms: lethargy, "
      + "irritability, and disdainful glares. Cats will reactivate on their own automatically "
      + "after a complete charge cycle; it is recommended that they be left undisturbed during "
      + "this process to maximize your enjoyment of your cat.</p><br/><p>"
      + "For more cat maintenance tips, tap to view the website!</p>"
      + "</article>";

  /**
   * Do stuff when buttons on index.jsp are clicked
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

    String userId = AuthUtil.getUserId(req);
    Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);
    LOG.info("credential:["+credential.getAccessToken()+"]");
    
    /*Shopping List Change*/
    if(preProcess(req, res, credential)){
    	return; //Skip all following processing if it is a json message request
    }
    /*Shopping List Change*/
    
    String message = "";

    if (req.getParameter("operation").equals("insertSubscription")) {

      // subscribe (only works deployed to production)
      try {
    	 /*Shopping List Code*/
//        MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId,
//            req.getParameter("collection"));
        String subscription_callback = Util.buildSubscriptionCallBack(req);
        MirrorClient.insertSubscription(credential, subscription_callback, userId,
                req.getParameter("collection"));    
        /*Shopping List Code*/        
        message = "Application is now subscribed to updates.";
      } catch (GoogleJsonResponseException e) {
        LOG.warning("Could not subscribe " + WebUtil.buildUrl(req, "/notify") + " because "
            + e.getDetails().toPrettyString());
        message = "Failed to subscribe. Check your log for details";
      }

    } else if (req.getParameter("operation").equals("deleteSubscription")) {

      // subscribe (only works deployed to production)
      MirrorClient.deleteSubscription(credential, req.getParameter("subscriptionId"));

      message = "Application has been unsubscribed.";

    } else if (req.getParameter("operation").equals("insertItem")) {
      LOG.fine("Inserting Timeline Item");
      TimelineItem timelineItem = new TimelineItem();

      if (req.getParameter("message") != null) {
        timelineItem.setText(req.getParameter("message"));
      }

      // Triggers an audible tone when the timeline item is received
      timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

      if (req.getParameter("imageUrl") != null) {
        // Attach an image, if we have one
        URL url = new URL(req.getParameter("imageUrl"));
        String contentType = req.getParameter("contentType");
        MirrorClient.insertTimelineItem(credential, timelineItem, contentType, url.openStream());
      } else {
        MirrorClient.insertTimelineItem(credential, timelineItem);
      }

      message = "A timeline item has been inserted.";

    } else if (req.getParameter("operation").equals("insertPaginatedItem")) {
      LOG.fine("Inserting Timeline Item");
      TimelineItem timelineItem = new TimelineItem();
      timelineItem.setHtml(PAGINATED_HTML);

      List<MenuItem> menuItemList = new ArrayList<MenuItem>();
      menuItemList.add(new MenuItem().setAction("OPEN_URI").setPayload(
          "https://www.google.com/search?q=cat+maintenance+tips"));
      timelineItem.setMenuItems(menuItemList);

      // Triggers an audible tone when the timeline item is received
      timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

      MirrorClient.insertTimelineItem(credential, timelineItem);

      message = "A timeline item has been inserted.";

    } else if (req.getParameter("operation").equals("insertItemWithAction")) {
      LOG.fine("Inserting Timeline Item");
      TimelineItem timelineItem = new TimelineItem();
      timelineItem.setText("Tell me what you had for lunch :)");

      List<MenuItem> menuItemList = new ArrayList<MenuItem>();
      // Built in actions
      menuItemList.add(new MenuItem().setAction("REPLY"));
      menuItemList.add(new MenuItem().setAction("READ_ALOUD"));

      // And custom actions
      List<MenuValue> menuValues = new ArrayList<MenuValue>();
      menuValues.add(new MenuValue().setIconUrl(WebUtil.buildUrl(req, "/static/images/drill.png"))
          .setDisplayName("Drill In"));
      menuItemList.add(new MenuItem().setValues(menuValues).setId("drill").setAction("CUSTOM"));

      timelineItem.setMenuItems(menuItemList);
      timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

      MirrorClient.insertTimelineItem(credential, timelineItem);

      message = "A timeline item with actions has been inserted.";

    } else if (req.getParameter("operation").equals("insertContact")) {
      if (req.getParameter("iconUrl") == null || req.getParameter("name") == null) {
        message = "Must specify iconUrl and name to insert contact";
      } else {
        // Insert a contact
        LOG.fine("Inserting contact Item");
        Contact contact = new Contact();
        contact.setId(req.getParameter("id"));
        contact.setDisplayName(req.getParameter("name"));
        contact.setImageUrls(Lists.newArrayList(req.getParameter("iconUrl")));
        contact.setAcceptCommands(Lists.newArrayList(new Command().setType("TAKE_A_NOTE")));
        MirrorClient.insertContact(credential, contact);

        message = "Inserted contact: " + req.getParameter("name");
      }

    } else if (req.getParameter("operation").equals("deleteContact")) {

      // Insert a contact
      LOG.fine("Deleting contact Item");
      MirrorClient.deleteContact(credential, req.getParameter("id"));

      message = "Contact has been deleted.";

    } else if (req.getParameter("operation").equals("insertItemAllUsers")) {
      if (req.getServerName().contains("glass-java-starter-demo.appspot.com")) {
        message = "This function is disabled on the demo instance.";
      }

      // Insert a contact
      List<String> users = AuthUtil.getAllUserIds();
      LOG.info("found " + users.size() + " users");
      if (users.size() > 10) {
        // We wouldn't want you to run out of quota on your first day!
        message =
            "Total user count is " + users.size() + ". Aborting broadcast " + "to save your quota.";
      } else {
        TimelineItem allUsersItem = new TimelineItem();
        allUsersItem.setText("Hello Everyone!");

        BatchRequest batch = MirrorClient.getMirror(null).batch();
        BatchCallback callback = new BatchCallback();

        // TODO: add a picture of a cat
        for (String user : users) {
          Credential userCredential = AuthUtil.getCredential(user);
          MirrorClient.getMirror(userCredential).timeline().insert(allUsersItem)
              .queue(batch, callback);
        }

        batch.execute();
        message =
            "Successfully sent cards to " + callback.success + " users (" + callback.failure
                + " failed).";
      }


    } else if (req.getParameter("operation").equals("deleteTimelineItem")) {

      // Delete a timeline item
      LOG.fine("Deleting Timeline Item");
      MirrorClient.deleteTimelineItem(credential, req.getParameter("itemId"));
      
      //Shopping List Change
      CardDao.getInstance().deleteCard(req.getParameter("itemId"));
      //Shopping List Change

      message = "Timeline Item has been deleted.";

    } else {
      String operation = req.getParameter("operation");
      LOG.warning("Unknown operation specified " + operation);
      message = "I don't know how to do that";
    }
    WebUtil.setFlash(req, message);
    res.sendRedirect(WebUtil.buildUrl(req, "/"));
  }
  
  
/*Shopping List Change*/  
  /**
 * @param req
 * @param res
 * @param credential
 * @return true if it is json message request
 * @throws IOException
 */
private boolean preProcess(HttpServletRequest req, HttpServletResponse res, Credential credential) throws IOException{
    
      String userId = AuthUtil.getUserId(req);
      AppController appController = AppController.getInstance();
	  if (req.getParameter("operation").equals("rawhttp")) {
		  String message = null;
	      if (!req.getParameter("jsonMsg").isEmpty()) {
			try {
				String url = getUrl(req.getParameter("url"));
				LOG.info("JSON Msg: \nurl:"+ url + "\n" + req.getParameter("jsonMsg"));
				
				message = sendJson(req.getParameter("jsonMsg"), credential.getAccessToken(),url);
			} catch (Exception e) {
			    LOG.log(Level.SEVERE, e.getMessage(), e);
				message = e.getMessage();
			}
	      }
          WebUtil.setFlash(req, message);
          res.sendRedirect(WebUtil.buildUrl(req, "/"));	      
	      return true;
	  }else if(req.getParameter("operation").equals("initialShoppingListApp")){
          if(CardDao.getInstance().getNumberOfCards(userId) == 0){
              appController.initApp(userId);
              WebUtil.setFlash(req,"We have initialized our glassware in you glass, you should able to see our shopping list cards in your glass now");
          }else{
              List<String> shoppingListCardIds = CardDao.getInstance().getCardsByType(userId, Constants.CARD_TYPE_MAIN, null);
              for (int i = 0; i < shoppingListCardIds.size(); i++) {
                  appController.startShopping(userId,shoppingListCardIds.get(i));  
              }
              WebUtil.setFlash(req,"You initialized our glassware before, we just create and bring all of your card to front");
          }
          res.sendRedirect(WebUtil.buildUrl(req, "/"));	   
          return true;
      }else if(req.getParameter("operation").equals("insertCoupon")){
           appController.insertCoupon(userId, req.getParameter("couponContent"));
           WebUtil.setFlash(req,"Coupon is created");
           res.sendRedirect(WebUtil.buildUrl(req, "/"));
           return true;
      }else if(req.getParameter("operation").startsWith("admin")){
          String msg = "";
          try{
              if(req.getParameter("operation").equals("admin_cleanCards")){
                 if(appController.cleanUpCards(userId))
                     msg = "All your shopping list cards have been removed from your timeline, as well as the data in our application database";
                 else
                     msg = "Fail to clean up all cards, pleaes check log for details";             
              }else if(req.getParameter("operation").equals("admin_cleanToken")){
                  appController.cleanUpToken(); //after token clean, user will be redirected to login page
                  req.getSession().removeAttribute("userId");
              }
          }catch(Throwable t){//try catch all exception from admin function to avoid impact the normal flow 
              msg = t.getMessage();
              LOG.log(Level.SEVERE,t.getMessage(),t);
          }
          
          WebUtil.setFlash(req,msg);
          res.sendRedirect(WebUtil.buildUrl(req, "/"));
          return true;
      }else if(req.getParameter("operation").startsWith("testing")){
          Mirror mirrorClient = MirrorClient.getMirror(credential);
          if(req.getParameter("operation").equals("testing1")){
              AppController.getInstance().markItem(mirrorClient, userId, req.getParameter("addInfo"));
          }else{
              AppController.getInstance().unMarkItem(mirrorClient, userId, req.getParameter("addInfo"));
          }
          res.sendRedirect(WebUtil.buildUrl(req, "/"));
          return true;
      }else{
		  return false;
	  }
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
	
	private String getUrl(String key){
		if("timeline".equals(key)){
			return "https://www.googleapis.com/mirror/v1/timeline";
		}else if("subscriptions".equals(key)){
			return "https://www.googleapis.com/mirror/v1/subscriptions";
		}else{//default
			return "https://www.googleapis.com/mirror/v1/timeline";
		}
		
	}
	/*Shopping List Change*/
}
