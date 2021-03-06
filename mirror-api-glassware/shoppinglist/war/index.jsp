<%@ page import="com.rightcode.shoppinglist.glass.util.Util"%>
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
iframe{
	border-bottom: solid 2px black;
}
</style>

</head>
<body style="background-image: url('http://www.somersetdesign.co.uk/blog/wp-content/uploads/2013/05/google-glass-.jpg'); background-color: #ffffff; background-size: 100%; background-repeat: no-repeat;">
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
			<h1 style="color: white">Glass Shopping List</h1>
		</div>
		<div class="row">
			<h4 style="color: white">Admin Block (For Developers Only)</h4>
			<div class="row">
			<%
			    String flash = WebUtil.getClearFlash(request);
						      if (flash != null && !flash.equals("")) {
			%>
			<div class="span8">
				<h5 style="color: red; font-weight: bold">Notice:</h5>
				<div class="alert alert-info"><%=StringEscapeUtils.escapeHtml4(flash)%></div>
			</div>
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
								onclick="document.getElementById('adminOperation').value='admin_initialShoppingListApp'">Clean timeline, database and send welcome card to timeline</button>

						</td>
						<td>
							<button class="btn btn-block btn-danger" type="submit"
								onclick="document.getElementById('adminOperation').value='admin_cleanToken'">Authorize another Google account to the glassware</button>
						</td>
						<td>
							<button class="btn btn-block btn-warning" type="submit"
								onclick="document.getElementById('adminOperation').value='admin_testConn'">Test Connection to external
								server </button>
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
		<div class="row" style="color: white">
			<h4 style="color: white">Playground:</h4>
			Use this playground to insert cards to Glass, modify existing cards, preview cards and delete cards from Glass
			timeline.<br /> Insert Client ID - <%= Util.getProjectClientId() %> and press the red "Authorize" button.
			<br /> <br />
			<iframe width="1040px" height="800px" frameBorder="0" src="https://mirror-api-playground.appspot.com/"></iframe>
		</div>
		<hr />
		<!-- footer -->
		<div class="row" style="color: white">© Rightcode LTD 2013, All Rights Reserved. No part of this website or any of its contents may
			be reproduced, copied, modified or adapted, without the prior written consent of the author, unless otherwise
			indicated for stand-alone materials.</div>
	</div>
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
	<script src="/static/bootstrap/js/bootstrap.min.js"></script>
	<script type="text/javascript" charset="utf-8" src="/static/tubular/js/jquery.tubular.1.0.js"></script>
	<script type="text/javascript">
		$(".container").ready(function() {
			$('.container').tubular({videoId: 'v1uyQZNg2vE'}); // where idOfYourVideo is the YouTube ID.
		});
	</script>
</body>
</html>