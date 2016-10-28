package com.tilab.ca.call_on_detect;


public class UserSettings {
    
	private transient String username;
	private transient String password;
	
	private String destUser;
	private String destHost;
	
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
	public String getDestHost() {
		return destHost;
	}
	public void setDestHost(String destHost) {
		this.destHost = destHost;
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
