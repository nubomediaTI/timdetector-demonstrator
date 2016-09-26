package com.tilab.ca.sip_endpoint.listeners.interfaces;

@FunctionalInterface
public interface ErrorListener {
	void onError(Exception ex);
}
