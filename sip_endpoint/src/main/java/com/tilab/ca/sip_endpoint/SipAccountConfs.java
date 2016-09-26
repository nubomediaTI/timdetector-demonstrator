package com.tilab.ca.sip_endpoint;

public class SipAccountConfs {	
		
	private String host;    
    private Integer port;     
    
    private String username;
    private String password;
    
    private int maxNumRegAttempts;
    
    private String stunServerHost;
    private int stunServerPort;
    
    private String transport;
    
    
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getMaxNumRegAttempts() {
		return maxNumRegAttempts;
	}
	public void setMaxNumRegAttempts(int maxNumRegAttempts) {
		this.maxNumRegAttempts = maxNumRegAttempts;
	}
	
	public String getStunServerHost() {
		return stunServerHost;
	}
	public void setStunServerHost(String stunServerHost) {
		this.stunServerHost = stunServerHost;
	}
	public int getStunServerPort() {
		return stunServerPort;
	}
	public void setStunServerPort(int stunServerPort) {
		this.stunServerPort = stunServerPort;
	}
	public String getFromSipUriStr(){
		return String.format("sip:%s@%s:%d",username,host,port);
	}
	public String getTransport() {
		return transport;
	}
	public void setTransport(String transport) {
		this.transport = transport;
	}
	
	
}
