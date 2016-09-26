package com.tilab.ca.sip_endpoint.utils;

public class StunServerAddress {

	private String host;
	private int port;
	
	public StunServerAddress(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	
}
