package com.rightcode.shoppinglist.glass.model;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * 
 * @author me
 *
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Card {

  /**
 * Assume cardId would be unique for all users, if not, include userId to create a composite key
 */
@PrimaryKey
  private String cardId;

  @Persistent
  private String userId;
  
  @Persistent
  private String projectClientId;  

  /**
   * Refer to Constants.CARD_TYPE_MAIN Constants.CARD_TYPE_BUNDLE
   * Constants.CARD_TYPE_PRODUCT
   */
  @Persistent
  private String type;

  /**
   * For product card, it is the product number<br>
   * For bundle cover card, it is the bundleId<br>
   * Not use for the main(shopping list) card
   */
  @Persistent
  private String ref;
  
 /**
 * CardId of the shopping list card to which this card belongs 
 * Null for the shopping list card itself
 */
@Persistent
  private String shoppingListCardId;

  @Persistent
  private boolean isPurchased = false; //Required if we don't have external service to store the purchase status

  public Card(String cardId, String userId, String projectClientId,String type, String ref, String shoppinglistCardId) {
    this.cardId = cardId;
    this.projectClientId = projectClientId;
    this.userId = userId;
    this.type = type;
    this.ref = ref;
    this.shoppingListCardId = shoppinglistCardId;
  }

  public String getCardId() {
    return cardId;
  }

  public String getRef() {
    return ref;
  }

  public String getUserId() {
    return userId;
  }

  public String getType() {
    return type;
  }

  public boolean isPurchased() {
    return isPurchased;
  }

  public void setPurchased(boolean isPurchased) {
    this.isPurchased = isPurchased;
  }

}
