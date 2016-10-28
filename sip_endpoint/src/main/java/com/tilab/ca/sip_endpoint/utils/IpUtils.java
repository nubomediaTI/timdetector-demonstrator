package com.tilab.ca.sip_endpoint.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class IpUtils {
	
	private static final Logger log = LoggerFactory.getLogger(IpUtils.class);

	private IpUtils(){}
	
	public static String getKmsIpFromUrl(String kmsUrl){
		String[] kmsUrlParts = kmsUrl.split("//")[1].split(":");
		String kmsIp = kmsUrlParts[0];
		log.info("found kms ip -> "+kmsIp);
		if(!kmsIp.matches("([0-9]+\\.){3}[0-9]+")){
			log.info("kms ip is a name. Resolving to ip.. ");
			try {
				kmsIp = InetAddress.getByName(kmsIp).getHostAddress();
				log.info("found real kms ip -> "+kmsIp);
			} catch (UnknownHostException e) {
				throw new IllegalStateException("failed to resolve ip address from name "+kmsIp,e);
			}
		}
		return kmsIp;
	}
	
	public static boolean isPrivateV4OrLoopbackAddress(String ip) {
		InetAddress addr = InetAddresses.forString(ip);
		if(addr.isLoopbackAddress())
			return true;
	    int address = InetAddresses.coerceToInteger(addr);
	    return (((address >>> 24) & 0xFF) == 10)
	            || ((((address >>> 24) & 0xFF) == 172) 
	              && ((address >>> 16) & 0xFF) >= 16 
	              && ((address >>> 16) & 0xFF) <= 31)
	            || ((((address >>> 24) & 0xFF) == 192) 
	              && (((address >>> 16) & 0xFF) == 168));
	}
}
