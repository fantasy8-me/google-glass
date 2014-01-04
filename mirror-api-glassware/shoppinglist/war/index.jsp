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
  <title>Rightcode Shopping List Google Glass App Administration Panel</title>
  <link href="/static/bootstrap/css/bootstrap.min.css" rel="stylesheet"
        media="screen">
  <link href="/static/bootstrap/css/bootstrap-responsive.min.css"
        rel="stylesheet" media="screen">
  <link href="/static/main.css" rel="stylesheet" media="screen">
</head>
<body style="background-image:url('http://www.somersetdesign.co.uk/blog/wp-content/uploads/2013/05/google-glass-.jpg'); background-color:#ffffff; background-size:100%; background-repeat: no-repeat;">
<div class="navbar navbar-inverse navbar-fixed-top">
  <div class="navbar-inner">
    <div class="container">
      <a class="brand" href="#">Rightcode Shopping List Google Glass App Administration Panel</a>
    </div>
  </div>
</div>
 
<div class="container">
    <div class="row" align="center">
          <img src="http://i.imgur.com/kpZmaxp.png" /><br/>
          <h1>Glass Shopping List</h1>
        </div>
        <div class="row">
        <h4>App Introduction:</h4>
          This Glassware is an app specifically designed for Google Glass.</br>
          The purpose of this Glassware is to manage external shopping list database located on the cloud, from the Google Glass device.</br>
          This demo will show how to enhance shopping capabilities & shopper experience using Google Glass.</br>
          <!-- <img src="http://i.imgur.com/1qpjNhu.png" /><br/> -->
          <!-- <img src="http://i.imgur.com/8CmINwW.png" /><br/> -->
        </div>
        <br/><br/><br/><br/><br/><br/>
        <div class="row">
        <h4>Playground:</h4>
        Use this playground to insert cards to Glass, modify existing cards, preview cards and delete cards from Glass timeline.<br/>
        Insert Client ID - 989966632667@developer.gserviceaccount.com and press the red "Authorize" button.
        <br/><br/>
        <iframe width="1040px" height="800px" src="https://mirror-api-playground.appspot.com/"></iframe>
        </div>
        <div class="row">
                        <h4>Admin Block (For Developers Only)</h4>
        <table>
        <tr><td>
                        <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
                        <input type="hidden" name="operation" value="initialShoppingListApp">
                        <button class="btn btn-block btn-primary" style="height: 200px; width: 200px;" type="submit">
                        Initial Shopping List Glassware
                        </button>
                </form>
        </td>
        <td>   
                <form class="form-horizontal" action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
                        <input type="hidden" id="adminOperation" name="operation" value="">
                        <button class="btn btn-block btn-danger" style="height: 200px; width: 200px;" type="submit" onclick="document.getElementById('adminOperation').value='admin_cleanToken'">
                        Whenever you re-deploy the app with another project id, click this button before any operations
                        </button>  
        </td><td>              
                        <button class="btn btn-block btn-warning" style="height: 200px; width: 200px;" type="submit" onclick="document.getElementById('adminOperation').value='admin_cleanCards'">
                        Clean Shopping List Cards and Reset Marked Items
                        </button>
           </form>
           </td>
        <td>
      <% if (timelineSubscriptionExists) { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>"
            method="post">
        <input type="hidden" name="subscriptionId" value="timeline">
        <input type="hidden" name="operation" value="deleteSubscription">
        <button class="btn btn-block btn-danger" style="height: 200px; width: 200px;" type="submit" class="delete">
          Unsubscribe from timeline updates
        </button>
      </form>
      <% } else { %>
      <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
        <input type="hidden" name="operation" value="insertSubscription">
        <input type="hidden" name="collection" value="timeline">
        <button class="btn btn-block btn-success" style="height: 200px; width: 200px;" type="submit">
          Subscribe to timeline updates
        </button>
      </form>
      <% } %>
      </td>
      <td>
      <button class="btn btn-block btn-primary" style="height: 200px; width: 200px;" onclick="window.open('https://code.google.com/apis/console/b/0/?pli=1#project:989966632667:quotas');">
          Quota Usage
        </button>
      </td>
      </tr>
      </table>
    </div>
    <div class="row">
    <h5>Admin Logs:</h5>
      <% String flash = WebUtil.getClearFlash(request);
      if (flash != null) { %>
  	  	<div class="alert alert-info"><%= StringEscapeUtils.escapeHtml4(flash) %></div>
  	  <% } %>
    </div>
    <hr/>
<!-- footer -->
<div class="row">
© Rightcode LTD 2013, All Rights Reserved. No part of this website or any of its contents may be reproduced, copied, modified or adapted, without the prior written consent of the author, unless otherwise indicated for stand-alone materials.
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