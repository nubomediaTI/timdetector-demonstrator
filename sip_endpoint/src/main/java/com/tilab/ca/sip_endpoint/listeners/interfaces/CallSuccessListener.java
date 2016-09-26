package com.tilab.ca.sip_endpoint.listeners.interfaces;

import org.kurento.client.SdpEndpoint;

@FunctionalInterface
public interface CallSuccessListener {
	void onCallSuccess(SdpEndpoint sdpEndpoint);
}
