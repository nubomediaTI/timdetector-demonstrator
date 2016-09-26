package com.tilab.ca.sip_endpoint.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.stack.StunStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SipUtils {

	private static final Logger log = LoggerFactory.getLogger(SipUtils.class);
	
	//assure that the class is not instantiable
	private SipUtils(){}
	
	/**
	 * check if the provided string is blank (null or empty)
	 * @param str
	 * @return
	 */
	public static boolean isBlank(String str){
		return str == null || str.isEmpty();
	}
	
	/**
	 * check if one of the provided strings is blank (null or empty)
	 * @param strs
	 * @return
	 */
	public static boolean anyBlank(String... strs){
		return Arrays.asList(strs).stream().anyMatch(SipUtils::isBlank);
	}
	
	/**
	 * Returns the machine local ip address
	 * @return
	 * @throws SocketException 
	 */
	public static final String getLocalIp(String ifaceName) throws UnknownHostException, SocketException{
		if(!SipUtils.isBlank(ifaceName)){
			NetworkInterface iface = NetworkInterface.getByName(ifaceName);
			if(iface == null)
				throw new IllegalArgumentException("cannot find interface with name "+ifaceName+".");
			return iface.getInterfaceAddresses().stream()
										 .filter(a -> a.getAddress() instanceof java.net.Inet4Address)
										 .findAny()
										 .orElseThrow(() -> new IllegalArgumentException("0 or more than one ip associated to interface "+ifaceName))
										 .getAddress().getHostAddress();
		}
		Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
		while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = iaddresses.nextElement();
                if ((!iaddress.isLoopbackAddress()) && (!iaddress.isLinkLocalAddress())) {
                	return iaddress.getHostAddress();
                }
            }
        }
		return InetAddress.getLocalHost().getHostAddress();
	}
	
	/**
	 * Return stun binded ip address and port
	 * @return
	 * @throws Exception
	 **/
	public static final StunServerAddress getStunAddress(String stunServerHost,int stunServerPort,String localIp,int localPort) throws Exception{
		Transport protocol = Transport.UDP;
		TransportAddress localAddr = new TransportAddress(localIp, localPort, protocol);
        TransportAddress serverAddr = new TransportAddress(stunServerHost, stunServerPort,protocol);
        NetworkConfigurationDiscoveryProcessPatch addressDiscovery =
                new NetworkConfigurationDiscoveryProcessPatch(new StunStack(),localAddr, serverAddr);
        addressDiscovery.start();
        StunDiscoveryReport report = addressDiscovery.determineAddress();
        addressDiscovery.shutDown();
        log.info("Discovered NAT type is "+report.getNatType());
        if(report.getNatType().equals(StunDiscoveryReport.UDP_BLOCKING_FIREWALL) || report.getNatType().equals(StunDiscoveryReport.UNKNOWN))
        	throw new IllegalStateException("FAILED to contact stun server or blocking firewall. Please check your configurations");
        return new StunServerAddress(report.getPublicAddress().getHostAddress(),report.getPublicAddress().getPort());
	}
	
	
	
	
	/**
	 * Get the first free port available
	 * @return
	 * @throws IOException
	 */
	public synchronized static final int getFirstFreePort() throws IOException{
		ServerSocket s = new ServerSocket(0);
		int freePort = s.getLocalPort();
		s.close();
		return freePort;
	}
	
	public synchronized static final int getFirstFreePortFromRange(int portFrom,int portTo) throws IOException{
		ServerSocket s;
		
		for(int currPort=portFrom;currPort<=portTo;currPort++){
			try {
	            s = new ServerSocket(currPort);
	            int port = s.getLocalPort();
	            s.close();
	            return port;
	        } catch (IOException ex) {
	            continue; // try next port
	        }
		}
		throw new IllegalStateException(String.format("No available ports in the provided range %d - %d",portFrom,portTo));
	}
	
}
