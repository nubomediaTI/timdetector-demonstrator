package com.tilab.ca.sip_endpoint.utils;

import java.io.IOException;

import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.AttributeFactory;
import org.ice4j.attribute.ChangeRequestAttribute;
import org.ice4j.attribute.ChangedAddressAttribute;
import org.ice4j.attribute.MappedAddressAttribute;
import org.ice4j.attribute.XorMappedAddressAttribute;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;
import org.ice4j.socket.IceSocketWrapper;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.socket.SafeCloseDatagramSocket;
import org.ice4j.stack.StunStack;
import org.ice4j.stunclient.BlockingRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationDiscoveryProcessPatch{

	/**
     * Our class logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(NetworkConfigurationDiscoveryProcessPatch.class);
    /**
     * Indicates whether the underlying stack has been initialized and started
     * and that the discoverer is operational.
     */
    private boolean started = false;

    /**
     * The point where we'll be listening.
     */
    private TransportAddress localAddress  = null;

    /**
     * The address of the stun server
     */
    private TransportAddress serverAddress = null;

    /**
     * A utility used to flatten the multi thread architecture of the Stack
     * and execute the discovery process in a synchronized manner
     */
    private BlockingRequestSender requestSender = null;

    /**
     * The <tt>DatagramSocket</tt> that we are going to be running the
     * discovery process through.
     */
    private IceSocketWrapper sock = null;

    /**
     * The <tt>StunStack</tt> used by this instance for the purposes of STUN
     * communication.
     */
    private final StunStack stunStack;

    /**
     * Initializes a <tt>StunAddressDiscoverer</tt> with a specific
     * <tt>StunStack</tt>. In order to use it one must first start it.
     *
     * @param stunStack the <tt>StunStack</tt> to be used by the new instance
     * @param localAddress  the address where the stack should bind.
     * @param serverAddress the address of the server to interrogate.
     */
    public NetworkConfigurationDiscoveryProcessPatch(
            StunStack stunStack,
            TransportAddress localAddress, TransportAddress serverAddress)
    {
        if (stunStack == null)
            throw new NullPointerException("stunStack");

        this.stunStack = stunStack;
        this.localAddress  = localAddress;
        this.serverAddress = serverAddress;
    }

    /**
     * Shuts down the underlying stack and prepares the object for garbage
     * collection.
     */
    public void shutDown()
    {
        stunStack.removeSocket(localAddress);
        sock.close();
        sock = null;

        localAddress  = null;
        requestSender = null;

        this.started = false;
    }

    /**
     * Puts the discoverer into an operational state.
     * @throws IOException if we fail to bind.
     * @throws StunException if the stun4j stack fails start for some reason.
     */
    public void start()
        throws IOException, StunException
    {
        sock = new IceUdpSocketWrapper(
            new SafeCloseDatagramSocket(localAddress));

        stunStack.addSocket(sock);

        requestSender = new BlockingRequestSender(stunStack, localAddress);

        started = true;
    }

    /**
     * Implements the discovery process itself (see class description).
     * @return a StunDiscoveryReport containing details about the network
     * configuration of the host where the class is executed.
     *
     * @throws StunException ILLEGAL_STATE if the discoverer has not been started
     * @throws IOException if a failure occurs while executing the discovery
     * algorithm.
     */
    public StunDiscoveryReport determineAddress()
        throws StunException, IOException
    {
        checkStarted();
        StunDiscoveryReport report = new StunDiscoveryReport();
        StunMessageEvent evt = doTestI(serverAddress);

        if(evt == null)
        {
            //UDP Blocked
            report.setNatType(StunDiscoveryReport.UDP_BLOCKING_FIREWALL);
            return report;
        }
        else
        {
        	TransportAddress mappedAddress = getMappedAddressFromStunMessageEvent(evt);
        	
            if(mappedAddress == null)
            {
              /* maybe we contact a STUNbis server and which do not
               * understand our request.
               */
              logger.error("Failed to do the network discovery");
              return null;
            }

            logger.info("mapped address is="+mappedAddress
                        +", name=" + mappedAddress.getHostAddress());

            TransportAddress backupServerAddress = null;
            
            ChangedAddressAttribute   changedAddressAttribute=((ChangedAddressAttribute) evt.getMessage()
                  .getAttribute(Attribute.CHANGED_ADDRESS));
            
            if(changedAddressAttribute!=null){
            	backupServerAddress = changedAddressAttribute.getAddress();
            	logger.info("backup server address is="+backupServerAddress
                        + ", name=" + backupServerAddress.getHostAddress());
            }else{
            	logger.warn("failed to get backup server address.");
            }

            report.setPublicAddress(mappedAddress);
            if (mappedAddress.equals(localAddress))
            {
                evt = doTestII(serverAddress);
                if (evt == null)
                {
                    //Sym UDP Firewall
                    report.setNatType(StunDiscoveryReport
                                        .SYMMETRIC_UDP_FIREWALL);
                    return report;
                }
                else
                {
                    //open internet
                    report.setNatType(StunDiscoveryReport.OPEN_INTERNET);
                    return report;

                }
            }
            else
            {
                evt = doTestII(serverAddress);
                if (evt == null){
                	
                	if(backupServerAddress!=null){
	                    evt = doTestI(backupServerAddress);
	                    if(evt == null)
	                    {
	                        logger.info("Failed to receive a response from "
	                                    +"backup stun server!");
	                        return report;
	                    }
	                    TransportAddress mappedAddress2 = getMappedAddressFromStunMessageEvent(evt);
	                       
	                    if(mappedAddress.equals(mappedAddress2))
	                    {
	                        evt = doTestIII(serverAddress);
	                        if(evt == null)
	                        {
	                            //port restricted cone
	                            report.setNatType(StunDiscoveryReport
	                                              .PORT_RESTRICTED_CONE_NAT);
	                            return report;
	                        }
	                        else
	                        {
	                            //restricted cone
	                            report.setNatType(StunDiscoveryReport
	                                              .RESTRICTED_CONE_NAT);
	                            return report;
	
	                        }
	                    }
	                    else
	                    {
	                        //Symmetric NAT
	                        report.setNatType(StunDiscoveryReport.SYMMETRIC_NAT);
	                        return report;
	                    }
                	}else{
                		logger.warn("no backup stun server found. Return report..");
                		return report;
                	}
                }
                else
                {
                    //full cone
                    report.setNatType(StunDiscoveryReport.FULL_CONE_NAT);
                    return report;
                }
            }
        }

    }
    
    private TransportAddress getMappedAddressFromStunMessageEvent(StunMessageEvent evt){
    	
    	MappedAddressAttribute mappedAddressAttr = ((MappedAddressAttribute)evt.getMessage()
                .getAttribute(Attribute.MAPPED_ADDRESS));
    	
    	if(mappedAddressAttr == null){
    		XorMappedAddressAttribute xorMappedAddressAttribute = ((XorMappedAddressAttribute)evt.getMessage()
                    .getAttribute(Attribute.XOR_MAPPED_ADDRESS));
    		return xorMappedAddressAttribute.getAddress();
    		
    	}
    	return mappedAddressAttr.getAddress();        		
    }

    /**
     * Sends a binding request to the specified server address. Both change IP
     * and change port flags are set to false.
     * @param serverAddress the address where to send the bindingRequest.
     * @return The returned message encapsulating event or null if no message
     * was received.
     *
     * @throws StunException if an exception occurs while sending the messge
     * @throws IOException if an error occurs while sending bytes through
     * the socket.
     */
    private StunMessageEvent doTestI(TransportAddress serverAddress)
        throws IOException, StunException
    {
        Request request = MessageFactory.createBindingRequest();

/*
        ChangeRequestAttribute changeRequest
            = (ChangeRequestAttribute)request
                .getAttribute(Attribute.CHANGE_REQUEST);
        changeRequest.setChangeIpFlag(false);
        changeRequest.setChangePortFlag(false);
*/
        /* add a change request attribute */
        ChangeRequestAttribute changeRequest
            = AttributeFactory.createChangeRequestAttribute();
        changeRequest.setChangeIpFlag(false);
        changeRequest.setChangePortFlag(false);
        request.putAttribute(changeRequest);

        StunMessageEvent evt = null;
        try
        {
            evt = requestSender.sendRequestAndWaitForResponse(
                    request, serverAddress);
        }
        catch (StunException ex)
        {
            //this shouldn't happen since we are the ones that created the
            //request
            logger.error("Internal Error. Failed to encode a message",ex);
            return null;
        }

        if(evt != null)
            logger.info("TEST I res="+evt.getRemoteAddress().toString()
                               +" - "+ evt.getRemoteAddress().getHostAddress());
        else
            logger.info("NO RESPONSE received to TEST I.");
        return evt;
    }

    /**
     * Sends a binding request to the specified server address with both change
     * IP and change port flags are set to true.
     * @param serverAddress the address where to send the bindingRequest.
     * @return The returned message encapsulating event or null if no message
     * was received.
     *
     * @throws StunException if an exception occurs while sending the messge
     * @throws IOException if an exception occurs while executing the algorithm.
     */
    private StunMessageEvent doTestII(TransportAddress serverAddress)
        throws StunException, IOException
    {
        Request request = MessageFactory.createBindingRequest();

        /* ChangeRequestAttribute changeRequest
         *  = (ChangeRequestAttribute)request
         *   .getAttribute(Attribute.CHANGE_REQUEST); */
        /* add a change request attribute */
        ChangeRequestAttribute changeRequest = AttributeFactory.createChangeRequestAttribute();
        changeRequest.setChangeIpFlag(true);
        changeRequest.setChangePortFlag(true);
        request.putAttribute(changeRequest);

        StunMessageEvent evt
            = requestSender.sendRequestAndWaitForResponse(request,
                                                          serverAddress);
        if(evt != null)
            logger.info("Test II res="+evt.getRemoteAddress().toString()
                            +" - "+ evt.getRemoteAddress().getHostAddress());
        else
            logger.info("NO RESPONSE received to Test II.");

        return evt;
    }

    /**
     * Sends a binding request to the specified server address with only change
     * port flag set to true and change IP flag - to false.
     * @param serverAddress the address where to send the bindingRequest.
     * @return The returned message encapsulating event or null if no message
     * was received.
     * @throws StunException if an exception occurs while sending the messge
     * @throws IOException if an exception occurs while sending bytes through
     * the socket.
     */
    private StunMessageEvent doTestIII(TransportAddress serverAddress)
        throws StunException, IOException
    {
        Request request = MessageFactory.createBindingRequest();

        /* ChangeRequestAttribute changeRequest = (ChangeRequestAttribute)request.getAttribute(Attribute.CHANGE_REQUEST); */
        /* add a change request attribute */
        ChangeRequestAttribute changeRequest = AttributeFactory.createChangeRequestAttribute();
        changeRequest.setChangeIpFlag(false);
        changeRequest.setChangePortFlag(true);
        request.putAttribute(changeRequest);

        StunMessageEvent evt = requestSender.sendRequestAndWaitForResponse(
            request, serverAddress);
        if(evt != null)
            logger.info("Test III res="+evt.getRemoteAddress().toString()
                            +" - "+ evt.getRemoteAddress().getHostAddress());
        else
            logger.info("NO RESPONSE received to Test III.");

        return evt;
    }

    /**
     * Makes shure the discoverer is operational and throws an
     * StunException.ILLEGAL_STATE if that is not the case.
     * @throws StunException ILLEGAL_STATE if the discoverer is not operational.
     */
    private void checkStarted()
        throws StunException
    {
        if(!started)
            throw new StunException(StunException.ILLEGAL_STATE,
                                    "The Discoverer must be started before "
                                    +"launching the discovery process!");
    }

}
