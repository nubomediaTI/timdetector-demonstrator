package com.tilab.ca.call_on_detect;


public class UserSettings {
    
	private String username;
	private String password;
	
	private String destUser;
	
	private String sipHost;
	
	private Integer sipPort;
	
	private String rtspUrl;
	
	private String kmsIp;
	
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
	public String getDestUser() {
		return destUser;
	}
	public void setDestUser(String destUser) {
		this.destUser = destUser;
	}
	
	public String getSipHost() {
		return sipHost;
	}
	public void setSipHost(String sipHost) {
		this.sipHost = sipHost;
	}
	public Integer getSipPort() {
		return sipPort;
	}
	public void setSipPort(Integer sipPort) {
		this.sipPort = sipPort;
	}
	public String getRtspUrl() {
		return rtspUrl;
	}
	public void setRtspUrl(String rtspUrl) {
		this.rtspUrl = rtspUrl;
	}
	public String getKmsIp() {
		return kmsIp;
	}
	public void setKmsIp(String kmsIp) {
		this.kmsIp = kmsIp;
	}
}
