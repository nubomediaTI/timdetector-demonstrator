package com.tilab.ca.sip_endpoint.auth;

import gov.nist.javax.sip.clientauthutils.UserCredentials;

public class UserCredentialsImpl implements UserCredentials{
	
	private final String username;
    private final String password;
    private final String sipDomain;

    public UserCredentialsImpl(String username, String password, String sipDomain) {
        this.username = username;
        this.password = password;
        this.sipDomain = sipDomain;
    }
    
    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getSipDomain() {
        return sipDomain;
    }
}
