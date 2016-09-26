package com.tilab.ca.call_on_detect;

public enum NotificationKeys {
	SIP_REGISTRATION("sipRegistration"),
	CALL("call"),
	FACE_DETECTED("faceDetected");
	
	private String keyName;
	
	private NotificationKeys(String keyName){
		this.keyName = keyName;
	}
	
	public String getKey(){
		return keyName;
	}
}
