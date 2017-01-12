package com.tilab.ca.sip_endpoint;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TooManyListenersException;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tilab.ca.sip_endpoint.auth.AccountManagerImpl;
import com.tilab.ca.sip_endpoint.auth.UserCredentialsImpl;
import com.tilab.ca.sip_endpoint.utils.SipUtils;
import com.tilab.ca.sip_endpoint.utils.StunServerAddress;

import gov.nist.javax.sip.SipStackExt;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;

public class SipClient {
	
	private static final Logger log = LoggerFactory.getLogger(SipClient.class);

	private final String contactIp;
	private final int contactPort;
	private final SipAccountConfs sipAccountConfs;
	
	private SipListener sipListener;
	private SipService sipService;
	private ListeningPoint listeningPoint;
	private SipProvider sipProvider;
	
	private boolean isStunEnabled = false;
	
	private ClientTransaction lastActiveTransaction;
	
	private int maxNumAttempts;
	private int numRegAttempts;
	
	private String sipStackKey;
	
	private static final int REGISTRATION_EXPIRES = 3600;
	private static final int MAX_FORWARDS = 70;

	

	private SipClient(SipClientBuilder builder) throws Exception {
		this.sipAccountConfs = builder.sipAccountConfs;
		
		if (SipUtils.isBlank(sipAccountConfs.getStunServerHost())) {
			this.contactIp = builder.localIp;
			this.contactPort = builder.localPort;
		} else {
			StunServerAddress stunAddress = SipUtils.getStunAddress(sipAccountConfs.getStunServerHost(),
					sipAccountConfs.getStunServerPort(), builder.localIp, builder.localPort);
			this.contactIp = stunAddress.getHost();
			this.contactPort = stunAddress.getPort();
			isStunEnabled = true;
		}
		this.sipStackKey = sipAccountConfs.getUsername()+"_"+System.currentTimeMillis(); //use sip username that is expected to be unique plus timestamp millis
		this.maxNumAttempts = this.sipAccountConfs.getMaxNumRegAttempts();
		this.numRegAttempts = this.maxNumAttempts;

		_init(builder.localIp,builder.localPort,sipAccountConfs.getTransport());
	}
	
	public void register() throws Exception{
		_register(REGISTRATION_EXPIRES);
	}
	
	public void unregister() throws Exception{
		_register(0);
	}
	
	public void call(String toUser,String toSipAddress,String sdpData) throws Exception{
		_invite(toUser, toSipAddress,sdpData);
	}
	
	public void hangup() throws SipException{
		_bye(lastActiveTransaction.getDialog());
	}
	
	public void dispose() throws Exception {
		// delete listening point and free the port
		sipService.getSipStack(this.sipStackKey).deleteListeningPoint(listeningPoint);
		// delete sip provider
		this.sipProvider.removeSipListener(this.sipListener);
		sipService.getSipStack(this.sipStackKey).deleteSipProvider(this.sipProvider);
		sipService.removeSipStack(this.sipStackKey);
	}
	
	public String getContactIp() {
		return contactIp;
	}
	
	public void setSipListener(SipListener sipListener) throws TooManyListenersException {
		this.sipListener = sipListener;
		 // Add our application as a SIP listener.
        this.sipProvider.addSipListener(this.sipListener);        
	}
	
	public void sendOkResponse(RequestEvent requestEvent){
		try {
			requestEvent.getServerTransaction().sendResponse(_createResponse(Response.OK, requestEvent.getRequest()));
		} catch (SipException | InvalidArgumentException e) {
			throw new IllegalStateException("failed to send ok response",e);
		}
	}
	
	public void sendBye(Dialog dialog) throws SipException{
		_bye(dialog);
	}
	
	public void handleChallenge(Response response,ClientTransaction tid,VoidCallback success) throws Exception{
		switch(response.getStatusCode()){
			case Response.PROXY_AUTHENTICATION_REQUIRED:
			case Response.UNAUTHORIZED:
				if(numRegAttempts-- <=0)
					throw new SipException("reached number of attempts. Challenge auth failed for user "+sipAccountConfs.getUsername());
            
				AuthenticationHelper authenticationHelper = 
							((SipStackExt) sipService.getSipStack(this.sipStackKey))
            						 .getAuthenticationHelper(new AccountManagerImpl(
            								 					new UserCredentialsImpl(sipAccountConfs.getUsername(), 
            								 											sipAccountConfs.getPassword(), 
            								 											contactIp)), 
            								 					sipService.getHeaderFactory());

				ClientTransaction inviteTid = authenticationHelper.handleChallenge(response, tid, sipProvider, 5);
        
				log.info("Sending request for replying challenge "+tid.getRequest().toString());

				inviteTid.sendRequest();
				break;
			default:
				this.numRegAttempts = this.maxNumAttempts; //reset attempts number
				success.execute();
		}
	}
	

	public SipProvider getSipProvider() {
		return sipProvider;
	}

	

	/**
	 * PRIVATE METHODS
	 */
	
	private void _register(int registrationExpireTime) throws Exception {
        log.info("registering...");
        _sendRequest(Request.REGISTER, (CallIdHeader callIdHeader,CSeqHeader cSeqHeader,FromHeader fromHeader,ArrayList<ViaHeader> viaHeaders,MaxForwardsHeader maxForwardsHeader)->{
        	ExpiresHeader expH = sipService.getHeaderFactory().createExpiresHeader(registrationExpireTime);
        	
        	Request request = sipService.getMessageFactory().createRequest(
                     fromHeader.getAddress().getURI(),
                     Request.REGISTER,
                     callIdHeader,
                     cSeqHeader,
                     fromHeader,
                     this.sipService.getHeaderFactory().createToHeader(fromHeader.getAddress(), null),
                     viaHeaders,
                     maxForwardsHeader);
        	request.addHeader(expH);
        	return request;
        });
	}
	
	private void _invite(String toUser,String toSipAddress,String sdpData)  throws Exception{
		log.info("creating invite request...");
        _sendRequest(Request.INVITE, (CallIdHeader callIdHeader,CSeqHeader cSeqHeader,FromHeader fromHeader,ArrayList<ViaHeader> viaHeaders,MaxForwardsHeader maxForwardsHeader)->{
        	
        	String toHostAddress = SipUtils.isBlank(toSipAddress)?sipAccountConfs.getHost():toSipAddress;
        	 // create To Header
            SipURI toSipUri = sipService.getAddressFactory().createSipURI(toUser, toHostAddress);
            Address toAddress = sipService.getAddressFactory().createAddress(toSipUri);
            
        	
        	Request request = sipService.getMessageFactory().createRequest(
                     toSipUri,
                     Request.INVITE,
                     callIdHeader,
                     cSeqHeader,
                     fromHeader,
                     this.sipService.getHeaderFactory().createToHeader(toAddress, null),
                     viaHeaders,
                     maxForwardsHeader);
        	
        	 // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = sipService.getHeaderFactory()
                    .createContentTypeHeader("application", "sdp");
            byte[] contents = sdpData.getBytes();
            request.setContent(contents, contentTypeHeader);
        	
        	return request;
        });
	}
	
	private void _bye(Dialog dialog) throws SipException {
		Request byeRequest =dialog.createRequest(Request.BYE);
        // send the request out.
		log.info("sending bye request..");
		log.info(byeRequest.toString());
		//sending bye request
		ClientTransaction byeTransaction = sipProvider.getNewClientTransaction(byeRequest);
		dialog.sendRequest(byeTransaction);
	}
	
	private void _sendRequest(String reqName,RequestGen reqGen) throws Exception{
		Address fromAddress = sipService.getAddressFactory().createAddress(_getSipUserFormatString(sipAccountConfs.getUsername(), sipAccountConfs.getHost(), 
																								   sipAccountConfs.getPort(), sipAccountConfs.getTransport()));
		fromAddress.setDisplayName(sipAccountConfs.getUsername());
		Request request = reqGen.generateRequest(this.sipProvider.getNewCallId(), //call id header
							  this.sipService.getHeaderFactory().createCSeqHeader(1L, reqName), 
							  sipService.getHeaderFactory().createFromHeader(fromAddress, String.valueOf(System.currentTimeMillis())), 
							  _generateViaHeaders(), 
							  sipService.getHeaderFactory().createMaxForwardsHeader(MAX_FORWARDS));
		request.addHeader(_generateContactHeader());
		request.addHeader(_createUserAgentHeader());
		log.info("request is "+request.toString());
		lastActiveTransaction= this.sipProvider.getNewClientTransaction(request);
        // Send the request statefully, through the client transaction.
        lastActiveTransaction.sendRequest();
        log.info(reqName+" Request sent");
	}

	private Response _createResponse(int responseType,Request request){
		try {
			return sipService.getMessageFactory().createResponse(responseType, request);
		} catch (ParseException e) {
			log.error("failed to create response",e);
			throw new IllegalStateException("failed to create response",e);
		}
	}
	
	private ArrayList<ViaHeader> _generateViaHeaders() throws ParseException, InvalidArgumentException{
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		String viaHost = isStunEnabled?contactIp:this.listeningPoint.getIPAddress();
		int viaPort = isStunEnabled?this.contactPort:this.listeningPoint.getPort();
		ViaHeader viaHeader = sipService.getHeaderFactory().createViaHeader(viaHost, viaPort, sipAccountConfs.getTransport(), null);
		viaHeader.setRPort();
	    viaHeaders.add(viaHeader);
	    return viaHeaders;
	}
	
	private ContactHeader _generateContactHeader() throws ParseException{
		String contactHost = isStunEnabled?contactIp:this.listeningPoint.getIPAddress();
		int contactPort = isStunEnabled?this.contactPort:this.listeningPoint.getPort();
		Address contactAddress = sipService.getAddressFactory().createAddress(_getContactString(sipAccountConfs.getUsername(),contactHost,contactPort,this.listeningPoint.getTransport()));
		return sipService.getHeaderFactory().createContactHeader(contactAddress);
	}
	
	private UserAgentHeader _createUserAgentHeader() throws ParseException{
	      return sipService.getHeaderFactory().createUserAgentHeader(Arrays.asList(new String[]{"TIMSipClientv0.0.1"}));
	}
	
	private String _getSipUserFormatString(String username,String ipAddress, int port, String transport){
		return String.format("sip:%s@%s:%d;transport=%s",username,ipAddress,port,transport);
	}
	
	private String _getContactString(String username,String contactHost,int contactPort,String transport){
		return _getSipUserFormatString(username,contactHost, contactPort, transport);
	}
	
	private void _init(String localIp,int localPort,String transport) throws Exception{
		this.sipService = SipService.INSTANCE;
		log.info(String.format("creating listening point on %s:%d with transport %s", localIp,localPort,transport));
        this.listeningPoint = sipService.getSipStack(this.sipStackKey).createListeningPoint(localIp, localPort, transport);
        // Create the SIP provider.
        this.sipProvider = sipService.getSipStack(sipStackKey).createSipProvider(this.listeningPoint);
	}
	
	
	

	public static class SipClientBuilder {
		private String localIp;
		private int localPort;
		private SipAccountConfs sipAccountConfs;

		public SipClientBuilder listenOn(String ip, int port) {
			this.localIp = ip;
			this.localPort = port;
			return this;
		}

		public SipClientBuilder confs(SipAccountConfs confs) {
			this.sipAccountConfs = confs;
			return this;
		}
		
		public SipClient build() throws Exception{
			return new SipClient(this);
		}

	}
	
	@FunctionalInterface
	private interface RequestGen{
		Request generateRequest(CallIdHeader callIdHeader,CSeqHeader cSeqHeader,FromHeader fromHeader,ArrayList<ViaHeader> viaHeaders,MaxForwardsHeader maxForwardsHeader) throws Exception;
	}
	
	@FunctionalInterface
	public interface VoidCallback{
		void execute();
	}
}
