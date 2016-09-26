package com.tilab.ca.sip_endpoint;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.Test;

import com.tilab.ca.sip_endpoint.utils.SipUtils;
import com.tilab.ca.sip_endpoint.utils.StunServerAddress;

public class SipUtilsTest {

	@Test
	public void testIsBlank() {
		assertTrue(SipUtils.isBlank(""));
		assertTrue(SipUtils.isBlank(null));
		assertFalse(SipUtils.isBlank(" "));
		assertFalse(SipUtils.isBlank("nonEmptyString"));
	}
	
	@Test
	public void testGetFisrtFreePort() throws IOException {
		int port = SipUtils.getFirstFreePort();
		try{
			ServerSocket s = new ServerSocket(port);
			s.getLocalPort();
			s.close();
		}catch(Exception e){
			fail("expected free port "+port);
		}
	}
	
	@Test
	public void testGetLocalIp() throws IOException {
		String ip = SipUtils.getLocalIp("eth0");
		System.out.println(ip);
	}
	
	@Test
	public void testGetAddressFromStun() throws Exception {
		StunServerAddress ssa = SipUtils.getStunAddress("stun.l.google.com", 19302, SipUtils.getLocalIp(null), SipUtils.getFirstFreePort());
		System.out.println("Host is "+ssa.getHost());
		System.out.println("Port is "+ssa.getPort());
	}
	
}
