package com.rightcode.shoppinglist.glass;

public class Constants {

    /*
     * Below constants define the attribute/columns of the internal product model There is no actual model class defined but only use map.
     * The reason is the data structure keep changing during development
     */
    public final static String ITEM_COL_PRD_ID = "prdId";
    public final static String ITEM_COL_PRDNAME = "prdName";
    public final static String ITEM_COL_IMGURL = "imgUrl";
    public final static String ITEM_COL_QUANTITY = "quantity";
    public final static String ITEM_COL_PRICE = "price";
    public final static String ITEM_COL_PROMO = "promo";
    public final static String ITEM_COL_PURCHASED = "purchased";

    // public final static String ITEM_PARM_MAXITEM = "maxItems";

    /* parameter used in velocity page */
    public final static String VELOCICY_PARM_AllPRODUCTS = "allProducts";
    public final static String VELOCICY_PARM_SHOPPING_LIST_NAME = "listName";
    public final static String VELOCICY_PARM_SHOPPING_LIST_SIMPLE = "isSimple";
    public final static String VELOCICY_PARM_SHOPPING_LIST_STATUS = "shoppingStatus";
    public final static String VELOCITY_PARM_TOTOAL = "total";
    public final static String VELOCITY_PARM_COMPLETED = "completed";
    public final static String VELOCITY_PARM_ITEMS_IN_SAME_LIST = "itemsInSameList";    
    

    /* Define different status of shopping list, status will be changed when user click the menu "startshopping", "finishShopping" */
    public final static String SHOPPING_LIST_STATUS_READY = "Ready";
    public final static String SHOPPING_LIST_STATUS_IN_PROGRESS = "In Progress";
    public final static String SHOPPING_LIST_STATUS_DONE = "Done";


    public final static String MENU_ID_MARK = "mark";
    public final static String MENU_ID_UNMARK = "unmark";
    public final static String MENU_ID_STARTSHOPPING = "startShopping";
    public final static String MENU_ID_FINISHSHOPPING = "finishShopping";
    public final static String MENU_ID_IC_FETCH = "fetch";
    public final static String MENU_ID_IC_RESTART = "restart";
    public final static String MENU_ID_IC_REFRESH = "refresh";

    public final static String MENU_NAME_MARK = "Check Off";
    public final static String MENU_NAME_UNMARK = "Uncheck";
    public final static String MENU_NAME_STARTSHOPPING = "Start Shopping";
    public final static String MENU_NAME_FINISHSHOPPING = "Finish Shopping";
    public final static String MENU_NAME_IC_FETCH = "Fetch";
    public final static String MENU_NAME_IC_RESTART = "Clean Timeline";
    public final static String MENU_NAME_IC_REFRESH = "Refresh";

    public final static String MENU_ICON_MARK = "http://i.imgur.com/IDQZwYS.png";
    public final static String MENU_ICON_UNMARK = "http://i.imgur.com/u8zAsCl.png";
    public final static String MENU_ICON_STARTSHOPPING = "http://i.imgur.com/RiO9mCR.png";
    public final static String MENU_ICON_FINISHSHOPPING = "http://i.imgur.com/RiO9mCR.png"; // Better to change the icon
    public final static String MENU_ICON_IC_FETCH = "http://i.imgur.com/RiO9mCR.png";
    public final static String MENU_ICON_IC_RESTART = "http://i.imgur.com/RiO9mCR.png";// Better to change the icon
    public final static String MENU_ICON_IC_REFRESH = "http://i.imgur.com/Mc862x6.png";

    /**
     * Initial Card
     */
    public final static String CARD_TYPE_IC = "IC";
    /**
     * Shopping list card
     */
    public final static String CARD_TYPE_SHOPPINGLIST = "SL";
    /**
     * Shopping list cover card
     */
    public final static String CARD_TYPE_LIST_COVER = "LC";
    /**
     * Shopping product card
     */
    public final static String CARD_TYPE_PRODUCT = "P";

    /*Below three constants are use to represent the result of application initialization*/
    public final static int INIT_APP_RESULT_SUCCESS_WITH_DUMMY = 1;
    public final static int INIT_APP_RESULT_SUCCESS_WITH_EXTERNAL = 2;
    public final static int INIT_APP_RESULT_FAIL = -1;

    /*Constants used to represent service type*/
    public final static String SERVICE_TYPE_EXTERNAL = "E";
    public final static String SERVICE_TYPE_DUMMY = "D";
    
    /**
     * Used when we cann't get the product image
     */
    public final static String DEFAULT_IMG = "http://i.imgur.com/8Bcyenr.png";

    public final static int EXTERNAL_SERVICE_TIMEOUT_IN_SECS = 50;
    public final static String EXTERNAL_MSG_ITEM_TYPE_FREETEXT = "FREETEXT";
    public final static String EXTERNAL_MSG_ITEM_TYPE_PRODUCT = "PRODUCT";

    public final static String EXTERNAL_MSG_TAG_LINES = "Lines";
    public final static String EXTERNAL_MSG_TAG_TYPE = "Type";
    public final static String EXTERNAL_MSG_TAG_ID = "Id";
    public final static String EXTERNAL_MSG_TAG_NAME = "Name";
    public final static String EXTERNAL_MSG_TAG_ITEM = "Item";
    public final static String EXTERNAL_MSG_TAG_QUANTITY = "Quantity";
    public final static String EXTERNAL_MSG_TAG_PRICE = "Price";
    public final static String EXTERNAL_MSG_TAG_VALUE = "Value";
    public final static String EXTERNAL_MSG_TAG_CURRENCY = "Currency";
    public final static String EXTERNAL_MSG_TAG_DESC = "Description";
    public final static String EXTERNAL_MSG_TAG_TYPECODE = "TypeCode";
    public final static String EXTERNAL_MSG_TAG_IMG = "Image";
    public final static String EXTERNAL_MSG_TAG_URL = "Url";
    public final static String EXTERNAL_MSG_TAG_PROMO = "Promotions";

}
