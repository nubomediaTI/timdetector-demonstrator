package com.tilab.ca.sip_endpoint.listeners.interfaces;

@FunctionalInterface
public interface SipOperationFailedListener {
	void onFailure(int failureResponseCode);
}
