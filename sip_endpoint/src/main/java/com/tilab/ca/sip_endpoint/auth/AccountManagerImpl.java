package com.tilab.ca.sip_endpoint.auth;

import javax.sip.ClientTransaction;

import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.UserCredentials;

public class AccountManagerImpl implements AccountManager{
	
	private final UserCredentials UserCredentials;

    public AccountManagerImpl(UserCredentials UserCredentials) {
        this.UserCredentials = UserCredentials;
    }
    
    @Override
    public UserCredentials getCredentials(ClientTransaction ct, String string) {
        return UserCredentials; 
    }
}
