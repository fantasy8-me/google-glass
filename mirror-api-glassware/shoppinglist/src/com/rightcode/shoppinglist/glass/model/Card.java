package com.rightcode.shoppinglist.glass.model;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Card {

  @PrimaryKey
  private String cardId;

  @Persistent
  private String userid;

  /**
   * Refer to Constants.CARD_TYPE_MAIN Constants.CARD_TYPE_BUNDLE
   * Constants.CARD_TYPE_PRODUCT
   */
  @Persistent
  private String type;

  /**
   * For product card, it is the product number, no use for other types
   */
  @Persistent
  private String ref;

  @Persistent
  private boolean isPurchased = false; //Eric.TODO double check whether it is required

  public Card(String cardId, String userid, String type, String ref) {
    this.cardId = cardId;
    this.userid = userid;
    this.type = type;
    this.ref = ref;
  }

  public String getCardId() {
    return cardId;
  }

  public String getRef() {
    return ref;
  }

  public String getUserid() {
    return userid;
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
