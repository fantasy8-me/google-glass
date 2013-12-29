<!--
Copyright (C) 2013 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<%@ page import="com.google.api.client.auth.oauth2.Credential" %>
<%@ page import="com.google.api.services.mirror.model.Contact" %>
<%@ page import="com.rightcode.shoppinglist.glass.ref.MirrorClient" %>
<%@ page import="com.rightcode.shoppinglist.glass.ref.WebUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.api.services.mirror.model.TimelineItem" %>
<%@ page import="com.google.api.services.mirror.model.Subscription" %>
<%@ page import="com.google.api.services.mirror.model.Attachment" %>
<%@ page import="com.rightcode.shoppinglist.glass.ref.MainServlet" %>
<%@ page import="org.apache.commons.lang3.StringEscapeUtils" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!doctype html>
<%
	String userId = com.rightcode.shoppinglist.glass.ref.AuthUtil.getUserId(request);
  String appBaseUrl = WebUtil.buildUrl(request, "/");

  Credential credential = com.rightcode.shoppinglist.glass.ref.AuthUtil.getCredential(userId);

  Contact contact = MirrorClient.getContact(credential, MainServlet.CONTACT_ID);

  List<TimelineItem> timelineItems = MirrorClient.listItems(credential, 3L).getItems();


  List<Subscription> subscriptions = MirrorClient.listSubscriptions(credential).getItems();
  boolean timelineSubscriptionExists = false;
  boolean locationSubscriptionExists = false;


  if (subscriptions != null) {
    for (Subscription subscription : subscriptions) {
      if (subscription.getId().equals("timeline")) {
        timelineSubscriptionExists = true;
      }
      if (subscription.getId().equals("locations")) {
        locationSubscriptionExists = true;
      }
    }
  }
%>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Yet another Glassware Starter Project</title>
  <link href="/static/bootstrap/css/bootstrap.min.css" rel="stylesheet"
        media="screen">
  <link href="/static/bootstrap/css/bootstrap-responsive.min.css"
        rel="stylesheet" media="screen">
  <link href="/static/main.css" rel="stylesheet" media="screen">
</head>
<body>
<div class="navbar navbar-inverse navbar-fixed-top">
  <div class="navbar-inner">
    <div class="container">
      <a class="brand" href="#">Yet another Glassware Starter Project: Java Edition, subscription & bundle support</a>
    </div>
  </div>
</div>

<div class="container">
	

  <% String flash = WebUtil.getClearFlash(request);
    if (flash != null) { %>
  <div class="alert alert-info"><%= StringEscapeUtils.escapeHtml4(flash) %></div>
  <% } %>
  <div class="row">
    <div class="span6">
      <form class="form-horizontal" action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
      	<input type="hidden" name="operation" value="rawhttp">
		  <div class="control-group">
		    <label class="control-label">Service Endpoint</label>
		    <div class="controls">
			    <select name="url">
			       	<option selected value="timeline">timeline url</option>
					<option value="subscriptions">Subscriptions url</option>
				</select>   
		    </div>
		  </div>      
    	  <div class="control-group">
		    <label class="control-label">Json Message</label>
		    <div class="controls">
		    	<textarea class="span4" name="jsonMsg" id="jsonMsg" style="height:250px"></textarea>
		    </div>
		  </div> 
    	  <div class="control-group">
		    <label class="control-label">Message Template</label>
		    <div class="controls">
		        <select onchange="insertTemplate(this)">
		        	<option selected>Select A Template</option>
					<option value="simpleText">Simple Text Card</option>
					<option value="html">Html Card</option>
				</select>
		    </div>
		  </div>        
          <button class="btn btn-block" type="submit">
            Insert the above json message
          </button>
      </form>
    </div>
    <div class="span5 offset1">
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
      	<input type="hidden" name="operation" value="initialShoppingListApp">
        <button class="btn btn-block btn-primary" type="submit">
          Initial Shopping List Glassware
        </button>
      </form>
     
     <hr>
     <h3>Debug Block<h4>
     <form class="form-horizontal" action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
      	<input type="hidden" id="operationInTest" name="operation" value="">
      	<div class="control-group">
		    <label class="control-label">Additional Info</label>
		    <div class="controls">
		       <input type="text" class="input-lg" name="addInfo"/>
		    </div>
		</div>  
        <button class="btn btn-warning" type="submit" onclick="document.getElementById('operationInTest').value='testing1'">
          Testing Button A
        </button>
        <button class="btn btn-warning" type="submit" onclick="document.getElementById('operationInTest').value='testing2'">
          Testing Button B
        </button>
      </form>      
    </div>
  </div>
  <h1>Your Recent Timeline</h1>
  <div class="row">

    <div style="margin-top: 5px;">

      <% if (timelineItems != null && !timelineItems.isEmpty()) {
        for (TimelineItem timelineItem : timelineItems) { %>
      <div class="span4">
        <table class="table table-bordered">
          <tbody>
            <tr>
              <th>ID</th>
              <td><%= timelineItem.getId() %></td>
            </tr>
            <tr>
              <th>Text</th>
              <td><%= StringEscapeUtils.escapeHtml4(timelineItem.getText()) %></td>
            </tr>
            <tr>
              <th>HTML</th>
              <td><%= StringEscapeUtils.escapeHtml4(timelineItem.getHtml()) %></td>
            </tr>
            <tr>
              <th>Attachments</th>
              <td>
                <%
                if (timelineItem.getAttachments() != null) {
                  for (Attachment attachment : timelineItem.getAttachments()) {
                    if (MirrorClient.getAttachmentContentType(credential, timelineItem.getId(), attachment.getId()).startsWith("")) { %>
                <img src="<%= appBaseUrl + "attachmentproxy?attachment=" +
                  attachment.getId() + "&timelineItem=" + timelineItem.getId() %>">
                <%  } else { %>
                <a href="<%= appBaseUrl + "attachmentproxy?attachment=" +
                  attachment.getId() + "&timelineItem=" + timelineItem.getId() %>">
                  Download</a>
                <%  }
                  }
                } else { %>
                <span class="muted">None</span>
                <% } %>
              </td>
            </tr>
            <tr>
              <td colspan="2">
                <form class="form-inline"
                      action="<%= WebUtil.buildUrl(request, "/main") %>"
                      method="post">
                  <input type="hidden" name="itemId"
                         value="<%= timelineItem.getId() %>">
                  <input type="hidden" name="operation"
                         value="deleteTimelineItem">
                  <button class="btn btn-block btn-danger"
                          type="submit">Delete</button>
                </form>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <% }
      } else { %>
      <div class="span12">
        <div class="alert alert-info">
          You haven't added any items to your timeline yet. Use the controls
          below to add something!
        </div>
      </div>
      <% } %>
    </div>
    <div style="clear:both;"></div>
  </div>

  <hr/>

  <div class="row">
    <div class="span4">
      <h2>Timeline</h2>

      <p>When you first sign in, this Glassware inserts a welcome message. Use
        these controls to insert more items into your timeline. Learn more
        about the timeline APIs
        <a href="https://developers.google.com/glass/timeline">here</a>.</p>


      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertItem">
        <textarea class="span4" name="message">Hello World!</textarea><br/>
        <button class="btn btn-block" type="submit">
          Insert the above message
        </button>
      </form>

      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertItem">
        <input type="hidden" name="message" value="Chipotle says 'hi'!">
        <input type="hidden" name="imageUrl" value="<%= appBaseUrl +
               "static/images/chipotle-tube-640x360.jpg" %>">
        <input type="hidden" name="contentType" value="image/jpeg">

        <button class="btn btn-block" type="submit">Insert a picture
          <img class="button-icon" src="<%= appBaseUrl +
               "static/images/chipotle-tube-640x360.jpg" %>">
        </button>
      </form>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertPaginatedItem">
        <button class="btn btn-block" type="submit">
          Insert a card with long paginated HTML</button>
      </form>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertItemWithAction">
        <button class="btn btn-block" type="submit">
          Insert a card you can reply to</button>
      </form>
      <hr>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertItemAllUsers">
        <button class="btn btn-block" type="submit">
          Insert a card to all users</button>
      </form>
    </div>

    <div class="span4">
      <h2>Contacts</h2>

      <p>By default, this project inserts a single contact that accepts
        all content types. Learn more about contacts
        <a href="https://developers.google.com/glass/contacts">here</a>.</p>

      <% if (contact == null) { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertContact">
        <input type="hidden" name="iconUrl" value="<%= appBaseUrl +
               "static/images/chipotle-tube-640x360.jpg" %>">
        <input type="hidden" name="id"
               value="<%= MainServlet.CONTACT_ID %>">
        <input type="hidden" name="name"
               value="<%= MainServlet.CONTACT_NAME %>">
        <button class="btn btn-block btn-success" type="submit">
          Insert Java Quick Start Contact
        </button>
      </form>
      <% } else { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="deleteContact">
        <input type="hidden" name="id" value="<%= MainServlet.CONTACT_ID %>">
        <button class="btn btn-block btn-danger" type="submit">
          Delete Java Quick Start Contact
        </button>
      </form>
      <% } %>

      <h3>Voice Commands</h3>
      <p>The "Java Quick Start" contact also accepts the <strong>take a
        note</strong> command. Take a note with the "Java Quick Start" contact
        and the cat in the server will record your note and reply with one of
        a few cat utterances.</p>
    </div>

    <div class="span4">
      <h2>Subscriptions</h2>

      <p>By default a subscription is inserted for changes to the
        <code>timeline</code> collection. Learn more about subscriptions
        <a href="https://developers.google.com/glass/subscriptions">here</a>.
      </p>

      <p class="alert alert-info">Note: Subscriptions require SSL. They will
        not work on localhost.</p>

      <% if (timelineSubscriptionExists) { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>"
            method="post">
        <input type="hidden" name="subscriptionId" value="timeline">
        <input type="hidden" name="operation" value="deleteSubscription">
        <button class="btn btn-block btn-danger" type="submit" class="delete">
          Unsubscribe from timeline updates
        </button>
      </form>
      <% } else { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertSubscription">
        <input type="hidden" name="collection" value="timeline">
        <button class="btn btn-block btn-success" type="submit">
          Subscribe to timeline updates
        </button>
      </form>
      <% } %>

      <% if (locationSubscriptionExists) { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>"
            method="post">
        <input type="hidden" name="subscriptionId" value="locations">
        <input type="hidden" name="operation" value="deleteSubscription">
        <button class="btn btn-block btn-danger" type="submit" class="delete">
          Unsubscribe from location updates
        </button>
      </form>
      <% } else { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertSubscription">
        <input type="hidden" name="collection" value="locations">
        <button class="btn btn-block btn-success" type="submit">
          Subscribe to location updates
        </button>
      </form>
      <% } %>
    </div>
  </div>
</div>

<script
    src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script src="/static/bootstrap/js/bootstrap.min.js"></script>
<script type="text/javascript">

var jsonTemplates = {
"simpleText" : 
'    {\n\
        "text": "This item auto-resizes according to the text length",\n\
	    "notification": {\n\
            "level": "DEFAULT"\n\
        }\n\
    }',
"html" : 
'  {\n\
      "html": "<strong class=\\\"blue\\\">HTML</strong>",\n\
      "notification": {\n\
          "level": "DEFAULT"\n\
      }\n\
   }'
};
			
	function insertTemplate(templates){
		if(templates.selectedIndex !== 0){
			console.log(templates.selectedIndex)
			document.getElementById("jsonMsg").value = jsonTemplates[templates[templates.selectedIndex].value];
		}
	}
</script>
</body>
</html>
