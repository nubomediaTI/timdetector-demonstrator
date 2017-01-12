package com.tilab.ca.sip_endpoint;

import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tilab.ca.sip_endpoint.SipClient.SipClientBuilder;
import com.tilab.ca.sip_endpoint.listeners.interfaces.CallSuccessListener;
import com.tilab.ca.sip_endpoint.listeners.interfaces.ErrorListener;
import com.tilab.ca.sip_endpoint.listeners.interfaces.SipEventListener;
import com.tilab.ca.sip_endpoint.listeners.interfaces.SipOperationFailedListener;
import com.tilab.ca.sip_endpoint.utils.IpUtils;
import com.tilab.ca.sip_endpoint.utils.SipUtils;


public class SipEndpoint {
	
	private static final int DEFAULT_HOST_PORT=5060;
	private static final int DEFAULT_MAX_REGISTRATION_NUM_ATTEMPTS_= 8;
	private static final String DEFAULT_TRANSPORT="udp";

	private static final int DEFAULT_STUN_PORT = 3478;
	
	private static final Logger log = LoggerFactory.getLogger(SipEndpoint.class);
	
	private final SipAccountConfs sipAccountConfs;
	private final String transport;
	private final String localAddress;
	private final int localPort;
	
	
	private boolean enableAvpf=false;
	
	//kurento
    private RtpEndpoint rtpEndpoint;
    private MediaPipeline mediaPipeline;
    private String kmsUrl;
    
    //FIXME temporary to test
    private String kmsIp; 
    
    //listeners
    private ErrorListener errorListener =  null;
    private CallSuccessListener callSuccessListener = null;
    private SipEventListener callEndedListener = null;
    private SipOperationFailedListener callFailedListener = null;
    private SipEventListener registrationSuccessListener = null;
    private SipOperationFailedListener registrationFailedListener = null;
    
    private SipClient sipClient;
	
	
	
	private SipEndpointStatus sipEndpointStatus;
	private boolean registered = false;
	
	
	private SipEndpoint(Builder builder) throws Exception{
		if(!builder.areMandatoryFiledsFilled())
			throw new IllegalStateException("All mandatory fields have to be filled. Mandatory fields are (username,password,host,pipeline)");
		
		sipAccountConfs = new SipAccountConfs();
		sipAccountConfs.setHost(builder.host);
		sipAccountConfs.setPort(builder.port != null && builder.port!=0? builder.port:DEFAULT_HOST_PORT);
		sipAccountConfs.setUsername(builder.username);
		sipAccountConfs.setPassword(builder.password);
		sipAccountConfs.setMaxNumRegAttempts(builder.maxNumRegistrationAttempts>0?builder.maxNumRegistrationAttempts:DEFAULT_MAX_REGISTRATION_NUM_ATTEMPTS_);
		if(!SipUtils.isBlank(builder.stunServerHost)){
			sipAccountConfs.setStunServerHost(builder.stunServerHost);
			sipAccountConfs.setStunServerPort(builder.stunServerPort);
		}
		this.transport = SipUtils.isBlank(builder.transport)?DEFAULT_TRANSPORT:builder.transport;
		sipAccountConfs.setTransport(transport);
		
		this.localAddress = SipUtils.getLocalIp(builder.listenOnInterface);
		this.localPort = SipUtils.getFirstFreePort();
		log.info("local ip is "+localAddress+" and local port "+localPort);
		
		this.enableAvpf = builder.enableAvpf;
		this.kmsUrl = builder.kmsUrl;
		if(!SipUtils.isBlank(kmsUrl))
        	kmsIp = IpUtils.getKmsIpFromUrl(kmsUrl);
		
		//this.kurentoClient = builder.kurentoClient;
		this.mediaPipeline = builder.pipeline;
		this.sipClient = new SipClientBuilder().confs(sipAccountConfs).listenOn(localAddress, localPort)
											  .build();
		sipClient.setSipListener(new DefaultSipListener(this,sipClient));//sipClient.getSipProvider()
	}
	
	public void register() throws Exception{
		sipClient.register();
	}
	
	public void unregister() throws Exception{
		sipClient.unregister();
	}
	
	//public void setKmsIp(String kmsIp){
		//this.kmsIp = kmsIp;
	//}
	
	/**
	 * start a call procedure to the sip user passed as parameter
	 * @param toUser destination sip user username 
	 * @param toSipAddress host address. If not provided is used the same as the from user
	 * @throws Exception
	 */
	public void startSipCall(String toUser,String toSipAddress) throws Exception{    
			setSipEndpointStatus(SipEndpointStatus.IN_CALL);
            //create rtp endpoint every time a call is started since it doesn not support renegotiation
            this.rtpEndpoint = new RtpEndpoint.Builder(this.mediaPipeline).build();
             
            String sdpData = rtpEndpoint.generateOffer();     
            
            //log.info("got kmsIp: "+kmsIp);
            
            log.info("got kmsIp: "+kmsIp);
            
            //replacing the kurento local ip 
            //if(!SipUtils.isBlank(kmsIp)){
            if(!SipUtils.isBlank(kmsIp) && !IpUtils.isPrivateV4OrLoopbackAddress(kmsIp)){
           		log.info("replacing local ip with remote one..");
               	sdpData = sdpData.replaceAll("c=IN IP4 ([0-9]+\\.){3}[0-9]+","c=IN IP4 "+kmsIp);
            }else{
        		log.info("detected private address on kmsIp or kmsUri not provided. Skipping SDP offer KMS Ip replace..");
        	}
            
            if(!enableAvpf)
            	sdpData = sdpData.replace("RTP/AVPF","RTP/AVP");
            
            log.info("rtpEndpoint generated offer is " + sdpData);
            sipClient.call(toUser, toSipAddress, sdpData);
    }
	
	
	public void stopSipCall() throws Exception{
		sipClient.hangup();
        setSipEndpointStatus(SipEndpointStatus.WAITING);
	}
	
	
	public void dispose() throws Exception{
		this.sipClient.dispose();
		this.rtpEndpoint = null;
	}
	
	public String getContactIp(){
		return sipClient.getContactIp();
	}

	public SipAccountConfs getSipAccountConfs() {
		return sipAccountConfs;
	}

	public ErrorListener getErrorListener() {
		return errorListener;
	}

	public void addErrorListener(ErrorListener errorListener) {
		this.errorListener = errorListener;
	}

	public CallSuccessListener getCallSuccessListener() {
		return callSuccessListener;
	}

	public void setCallSuccessListener(CallSuccessListener callSuccessListener) {
		this.callSuccessListener = callSuccessListener;
	}

	public SipEventListener getRegistrationSuccessListener() {
		return registrationSuccessListener;
	}

	public void setRegistrationSuccessListener(SipEventListener registrationSuccessListener) {
		this.registrationSuccessListener = registrationSuccessListener;
	}

	public SipOperationFailedListener getCallFailedListener() {
		return callFailedListener;
	}

	public void setCallFailedListener(SipOperationFailedListener callFailedListener) {
		this.callFailedListener = callFailedListener;
	}

	public SipEventListener getCallEndedListener() {
		return callEndedListener;
	}

	public void setCallEndedListener(SipEventListener callEndedListener) {
		this.callEndedListener = callEndedListener;
	}

	public SipOperationFailedListener getRegistrationFailedListener() {
		return registrationFailedListener;
	}

	public void setRegistrationFailedListener(SipOperationFailedListener registrationFailedListener) {
		this.registrationFailedListener = registrationFailedListener;
	}
	
	public void addRegistrationListeners(SipEventListener rsl, SipOperationFailedListener rfl){
		setRegistrationSuccessListener(rsl);
		setRegistrationFailedListener(rfl);
	}

	public void addCallListeners(CallSuccessListener csl, SipOperationFailedListener cfl){
		setCallSuccessListener(csl);
		setCallFailedListener(cfl);
	}

	public RtpEndpoint getRtpEndpoint() {
		return rtpEndpoint;
	}
	
	public SipEndpointStatus getSipEndpointStatus() {
		return sipEndpointStatus;
	}

	void setSipEndpointStatus(SipEndpointStatus sipEndpointStatus) {
		this.sipEndpointStatus = sipEndpointStatus;
	}
	
	public boolean isRegistered() {
		return registered;
	}

	void setRegistered(boolean registered) {
		this.registered = registered;
	}




	public static class Builder{
        private String host;    
        private Integer port;     
        private String transport;      
        
        private String username;
        private String password;
        
        //max number of times that the client will try to perform a registration (default 8)
        private int maxNumRegistrationAttempts;
        
        //if true enable RTP/AVPF capability otherwise use RTP/AVP in sdp offer through sip server
        private boolean enableAvpf=false;
        
        private String stunServerHost;
        private int stunServerPort;
        
        //kurento media pipeline
        private MediaPipeline pipeline;
        private String kmsUrl;
        private String listenOnInterface;
        
        public Builder(){}
        
        public Builder host(String host){
        	this.host = host;
        	return this;
        }
        
        public Builder port(Integer port){
        	this.port = port;
        	return this;
        }
        
        public Builder transport(String transport){
            this.transport = transport;
            return this;
        }
        
        public Builder password(String password){
            this.password = password;
            return this;
        }
        
        public Builder username(String username){
            this.username = username;
            return this;
        }
        
        public Builder withStunServer(String stunServerAddr){
        	if(!SipUtils.isBlank(stunServerAddr)){
	            String[] stunServerParts = stunServerAddr.split(":");
	        	this.stunServerHost = stunServerParts[0];
	        	if(stunServerAddr.length()==1){
	        		this.stunServerPort = DEFAULT_STUN_PORT;
	        	}else{
	        		this.stunServerPort = Integer.parseInt(stunServerParts[1]);
	        	}
        	}
            return this;
        }
        
        /**
         * set the max number of times that the client will try to perform a registration (default 8)
         * @param maxNumOra max number of times that the client will try to perform a registration 
         * @return
         */
        public Builder maxNumberOfRegistrationAttempt(int maxNumOra){
            this.maxNumRegistrationAttempts = maxNumOra;
            return this;
        }
        
        public Builder enableAVPF(){
            this.enableAvpf = true;
            return this;
        }
        
        public Builder kmsUrl(String kmsUrl){
            this.kmsUrl = kmsUrl;
            return this;
        }
        
        public Builder mediaPipeline(MediaPipeline pipeline){
            this.pipeline = pipeline;
            return this;
        }
        
        public Builder listenOnInterface(String ifname){
            if(!SipUtils.isBlank(ifname))
            	this.listenOnInterface = ifname;
            return this;
        }
        
        //&& kurentoClient!=null
        private boolean areMandatoryFiledsFilled(){
        	return !SipUtils.anyBlank(host,username,password) && pipeline != null;
        }
        
        public SipEndpoint build() throws Exception{
            return new SipEndpoint(this);
        }
    }

}
