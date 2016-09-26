package com.tilab.ca.sip_endpoint;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipStack;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SipService {
	
	INSTANCE;
	
	private static Logger log = LoggerFactory.getLogger(SipService.class);
	
	private SipFactory sipFactory;          // Used to access the SIP API.
    private MessageFactory messageFactory;  // Used to create SIP message factory.
    private HeaderFactory headerFactory;    // Used to create SIP headers.
    private AddressFactory addressFactory;  // Used to create SIP URIs.
    private ConcurrentHashMap<String,SipStack> sipStacksMap; //used to allow multiple clients registrations 
        
    private boolean inFailedState = false;
     
    
    private SipService(){
    	initSipSystem();
    }
    
    
	public SipFactory getSipFactory() {
		return sipFactory;
	}
	
	public SipStack getSipStack(String stackName) throws Exception{
		return this.sipStacksMap.getOrDefault(stackName, _createSipStack(stackName));
	}
	
	public void removeSipStack(String stackName){
		this.sipStacksMap.remove(stackName);
	}
	
	public MessageFactory getMessageFactory() {
		return messageFactory;
	}
	
	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}
	
	public AddressFactory getAddressFactory() {
		return addressFactory;
	}
	
	
	public boolean isInFailedState() {
		return inFailedState;
	}
	
	private SipStack _createSipStack(String stackName) throws PeerUnavailableException{
		Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", stackName);
        SipStack sipStack = sipFactory.createSipStack(properties);
        this.sipStacksMap.put(stackName, sipStack);
        return sipStack;
	}

	private void initSipSystem() {
	    try {
	    	if(log == null)
	    		log = LoggerFactory.getLogger(SipService.class);
	        log.info("initializing sip system...");
	        // Create the SIP factory and set the path name.
	        this.sipFactory = SipFactory.getInstance();
	        this.sipFactory.setPathName("gov.nist");
	        // Create and set the SIP stack properties.
	        
	        // Create the SIP stack map.
	        this.sipStacksMap = new ConcurrentHashMap<>();
	        // Create the SIP message factory.
	        this.messageFactory = this.sipFactory.createMessageFactory();
	        // Create the SIP header factory.
	        this.headerFactory = this.sipFactory.createHeaderFactory();
	        // Create the SIP address factory.
	        this.addressFactory = this.sipFactory.createAddressFactory();
	    } catch (Exception e) {
	        log.error("error during sip system initialization.",e);
	        this.inFailedState = true;
	    }
    }

    
}
