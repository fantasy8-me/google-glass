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
<%@ page import="com.google.api.client.auth.oauth2.Credential"%>
<%@ page import="com.google.api.services.mirror.model.Contact"%>
<%@ page import="com.rightcode.shoppinglist.glass.ref.MirrorClient"%>
<%@ page import="com.rightcode.shoppinglist.glass.ref.WebUtil"%>
<%@ page import="java.util.List"%>
<%@ page import="com.google.api.services.mirror.model.TimelineItem"%>
<%@ page import="com.google.api.services.mirror.model.Subscription"%>
<%@ page import="com.google.api.services.mirror.model.Attachment"%>
<%@ page import="com.rightcode.shoppinglist.glass.ref.MainServlet"%>
<%@ page import="org.apache.commons.lang3.StringEscapeUtils"%>

<%@ page contentType="text/html;charset=UTF-8" language="java"%>

<!doctype html>
<%
    String userId = com.rightcode.shoppinglist.glass.ref.AuthUtil.getUserId(request);
  String appBaseUrl = WebUtil.buildUrl(request, "/");
 
  Credential credential = com.rightcode.shoppinglist.glass.ref.AuthUtil.getCredential(userId);
 
  /* Contact contact = MirrorClient.getContact(credential, MainServlet.CONTACT_ID); */
 
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
<link href="/static/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
<link href="/static/bootstrap/css/bootstrap-responsive.min.css" rel="stylesheet" media="screen">
<link href="/static/main.css" rel="stylesheet" media="screen">
<link href="/static/custom/glass.css" rel="stylesheet" media="screen">
<style>
#adminForm button {
	height: 150px;
	width: 150px
}
</style>
</head>
<body style="background-image: url('http://www.somersetdesign.co.uk/blog/wp-content/uploads/2013/05/google-glass-.jpg'); background-color: #ffffff; background-size: 100%; background-repeat: no-repeat;">
	<div class="glass">
		<input id="switcheroo" type="checkbox" /> <label for="switcheroo" class="timeframe panel"> <time>
				<script type="text/javascript">
					var time = new Date();
					var h = time.getHours();
					if (h > 12) {
						h -= 12;
					} else if (h === 0) {
						h = 12;
					}
					var m = time.getMinutes();
					if (m < 10) {
						m = "0" + m;
					}
					document.write(h + ":" + m);
				</script>
			</time>
			<h2>
				<img src="http://i.imgur.com/0kEblEN.gif" style="vertical-align: middle" /> OK Glass, Loading...
			</h2>
		</label>
	</div>
	<div class="navbar navbar-inverse navbar-fixed-top">
		<div class="navbar-inner">
			<div class="container">
				<a class="brand" href="#">Rightcode Shopping List Google Glass App Administration Panel</a>
			</div>
		</div>
	</div>

	<div class="container" style="padding-top: 10px">
		<div class="row" align="center">
			<img src="http://i.imgur.com/kpZmaxp.png" /><br />
			<h1>Glass Shopping List</h1>
		</div>
		<div class="row">
			<h4>Admin Block (For Developers Only)</h4>
			<div class="row">
			<%
			    String flash = WebUtil.getClearFlash(request);
						      if (flash != null) {
			%>
			<h5 style="color: red; font-weight: bold">Notice:</h5>
			<div class="alert alert-info"><%=StringEscapeUtils.escapeHtml4(flash)%></div>
			<%
			    }
			%>
			</div>
			<form id="adminForm" action="<%=WebUtil.buildUrl(request, "/main")%>" method="post">
				<input type="hidden" id="adminOperation" name="adminOperation" value=""> 
				<input type="hidden" name="subscriptionId" value="timeline"> <input type="hidden" name="collection" value="timeline">
				<table>
					<tr>
						<td>
							<button class="btn btn-block btn-warning" type="submit"
								onclick="document.getElementById('adminOperation').value='admin_cleanCards'">Delete timeline cards
								and uncheck Items</button>
						</td>
						<td>
							<button class="btn btn-block btn-primary" type="submit"
								onclick="document.getElementById('adminOperation').value='admin_initialShoppingListAppFromExternal'">Fetch
								data from external server</button>
						</td>
						<td>
							<button class="btn btn-block btn-primary" type="submit"
								onclick="document.getElementById('adminOperation').value='admin_initialShoppingListApp'">Fetch data
								from dummy server</button>

						</td>
						<td>
							<button class="btn btn-block btn-danger" type="submit"
								onclick="document.getElementById('adminOperation').value='admin_cleanToken'">Login with another account</button>
						</td>
						<td>
							<button class="btn btn-block btn-warning" type="submit"
								onclick="document.getElementById('adminOperation').value='admin_testConn'">Test Connection to external
								server</button>
						</td>
						<td>
							<button class="btn btn-block btn-primary"
								onclick="window.open('https://code.google.com/apis/console/b/0/?pli=1#project:989966632667:quotas');return false;">View
								Quota Usage</button>
						</td>
					</tr>
				</table>
			</form>
		</div>
		<div class="row">
			<h4>Playground:</h4>
			Use this playground to insert cards to Glass, modify existing cards, preview cards and delete cards from Glass
			timeline.<br /> Insert Client ID - 989966632667.apps.googleusercontent.com and press the red "Authorize" button.
			<br /> <br />
			<iframe width="1040px" height="800px" src="https://mirror-api-playground.appspot.com/"></iframe>
		</div>
		<hr />
		<!-- footer -->
		<div class="row">© Rightcode LTD 2013, All Rights Reserved. No part of this website or any of its contents may
			be reproduced, copied, modified or adapted, without the prior written consent of the author, unless otherwise
			indicated for stand-alone materials.</div>
	</div>
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
	<script src="/static/bootstrap/js/bootstrap.min.js"></script>
	<script type="text/javascript">
		var jsonTemplates = {
			"simpleText" : '    {\n\
        "text": "This item auto-resizes according to the text length",\n\
            "notification": {\n\
            "level": "DEFAULT"\n\
        }\n\
    }',
			"html" : '  {\n\
      "html": "<strong class=\\\"blue\\\">HTML</strong>",\n\
      "notification": {\n\
          "level": "DEFAULT"\n\
      }\n\
   }'
		};

		function insertTemplate(templates) {
			if (templates.selectedIndex !== 0) {
				console.log(templates.selectedIndex)
				document.getElementById("jsonMsg").value = jsonTemplates[templates[templates.selectedIndex].value];
			}
		}
	</script>
</body>
</html>