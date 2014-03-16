package com.rightcode.shoppinglist.glass.oath;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * As GAE can't support store credential in memory, so have to implement a solution bases on db
 * This class is the credential model which will be persistent in db
 *
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class CredentialModel {

    @PrimaryKey
    private String userId;

    @Persistent
    private String accessToken;


    @Persistent
    private String refreshToken;

    @Persistent
    private long expirationTimeMillis;

    public CredentialModel(String userId, String accessToken, String refreshToken, long expirationTimeMillis) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.expirationTimeMillis = expirationTimeMillis;
    }

    public long getRef() {
        return expirationTimeMillis;
    }

    public String getUserId() {
        return userId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setExpirationTimeMillis(long expirationTimeMillis) {
        this.expirationTimeMillis = expirationTimeMillis;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpirationTimeMillis() {
        return expirationTimeMillis;
    }

}
