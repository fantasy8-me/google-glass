## Introduction

Host the glassware projects developed based on Mirror-API

## Structure

  * mirror-quickstart-java (Development of this project was stopped, all latest changes should be committed to "shoppinglist")

    >Eclipse project version of the official mirror-api sample project, refer to "https://developers.google.com/glass/develop/mirror/quickstart/java" for more details
    
    >The project can be directly imported to eclipse, just make sure you have maven plugin installed 
    
  * shoppinglist

    >Shopping list GAE project which can be directly deploy to GAE by eclipse plugin. 
    
    >The project can be directly imported to eclipse, just make sure you have GAE eclipse plugin&JKD1.7 installed.
     Refer to "https://developers.google.com/appengine/docs/java/tools/eclipse" for more details
    
    >If you got an "App Engine SDK" missing error right after import the project, please try to restart the workspace.
    
    >Package Definition :
    
    >>com.rightcode.shoppinglist.glass - Root package.
    
    >>com.rightcode.shoppinglist.glass.ref - Host all classes from original sample mirror api project provided by google
    
    >>com.rightcode.shoppinglist.glass.dao - Contains the dao object, for now, just CardDao.
    
    >>com.rightcode.shoppinglist.glass.model - Contains the model, for now, only one model defined.

    >>com.rightcode.shoppinglist.glass.service - Contains the service interface and implementation
    
    >>com.rightcode.shoppinglist.glass.util - Contains all utility class.
    
    >>com.rightcode.shoppinglist.glass.vm - Contains all vm pages, vm page is used to define the UI of all Cards.
    
    >Resources Files :
    
    >>productData.json - Provide the data for DemoShoppingListProvider
    
    >>category.json - Configurate the metadata of category, e.g. title, image url
    
    >>oauth.properties - Configurate the project client id, which is used for Mirror API Access
