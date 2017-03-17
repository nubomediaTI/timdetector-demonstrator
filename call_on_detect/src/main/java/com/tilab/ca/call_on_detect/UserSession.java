package com.tilab.ca.call_on_detect;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
//import com.tilab.ca.call_on_detect.sip.SipEndpoint;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.Properties;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.module.nubofacedetector.NuboFaceDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.tilab.ca.sip_endpoint.SipEndpoint;


public class UserSession {
    private final Logger log = LoggerFactory.getLogger(UserSession.class);

    private final WebRtcEndpoint webRtcEndpoint;
    private WebRtcEndpoint webRtcEndpointReceiver;
    private NuboFaceDetector faceDetector;
    private MediaPipeline mediaPipeline;
    private KurentoClient kurentoClient;
    private final String sessionId;
    private CallEventWaitTimer callEventWaitTimer;
    
    private String kmsUrl;
    
    private  PlayerEndpoint playerEndpoint;
    private boolean isPlayerEndpointSet = false;
    
    private UserSettings userSettings;
    
    private SipEndpoint sipEndpoint;

    public UserSession(String sessionId) {
		this.sessionId = sessionId;
		
		String kmsId = UUID.randomUUID().toString();
	    kmsUrl = KurentoClient.getKmsUrl(kmsId, new Properties());
	    
		// One KurentoClient instance per session
		kurentoClient = KurentoClient.create(kmsUrl);
	
		log.info("Created kurentoClient (session {})", sessionId);
		
		//set kurentoclient id through reflection
		try {
			//log.info("***********************************");
			//log.info("getting kurento client fields...");
			//Arrays.asList(kurentoClient.getClass().getFields()).forEach(f -> log.info(f.getName()));
			//log.info("getting kurento declared fields...");
			//Arrays.asList(kurentoClient.getClass().getDeclaredFields()).forEach(f -> log.info(f.getName()));
			log.info("setting kurento client id through reflection..");
			Field kurentoClientIdField = kurentoClient.getClass().getDeclaredField("id");
			kurentoClientIdField.setAccessible(true);
			kurentoClientIdField.set(kurentoClient, kmsId);
		} catch (Exception e) {
			log.error("failed to set kmsId through reflection",e);
		} 
	
		mediaPipeline = getKurentoClient().createMediaPipeline();
		log.info("Created Media Pipeline {} (session {})", getMediaPipeline().getId(), sessionId);
	
		webRtcEndpoint = new WebRtcEndpoint.Builder(getMediaPipeline()).build();
    }

    public WebRtcEndpoint getWebRtcEndpoint() {
	return webRtcEndpoint;
    }

    public WebRtcEndpoint getWebRtcEndpointReceiver() {
        if(webRtcEndpointReceiver == null)
            webRtcEndpointReceiver = new WebRtcEndpoint.Builder(getMediaPipeline()).build();
        return webRtcEndpointReceiver;
    }
    
    

    public PlayerEndpoint getPlayerEndpoint() {
        if(this.playerEndpoint == null){
            this.playerEndpoint = new PlayerEndpoint.Builder(getMediaPipeline(),getUserSettings().getRtspUrl()).build();
            playerEndpoint.play();
            isPlayerEndpointSet = true;
        }
        return this.playerEndpoint;
    }
    
    public void dismissPlayerEndpoint(PlayerEndpoint playerEndpoint) {
        this.playerEndpoint.stop();
        this.playerEndpoint = null;
    }

    public boolean isPlayerEndpointSet() {
        return isPlayerEndpointSet;
    }

    public final MediaPipeline getMediaPipeline() {
	return mediaPipeline;
    }

    public final KurentoClient getKurentoClient() {
    	return kurentoClient;
    }

    public void addCandidate(IceCandidate candidate) {
	getWebRtcEndpoint().addIceCandidate(candidate);
    }
    
    public void addCandidate(JsonObject jsonCandidate,boolean isWebRtcReceiver) {
	IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
	jsonCandidate.get("sdpMid").getAsString(), jsonCandidate.get("sdpMLineIndex").getAsInt());
        if(!isWebRtcReceiver)
            getWebRtcEndpoint().addIceCandidate(candidate);
        else
            getWebRtcEndpointReceiver().addIceCandidate(candidate);
    }
       

    public void release() {
        log.info("stopping playerendpoint if active (session {})", sessionId);
        if(isPlayerEndpointSet)
            dismissPlayerEndpoint(playerEndpoint);
        log.info("Releasing webrtc endpoint for session {}", sessionId);
        webRtcEndpoint.release();
        log.info("Releasing media pipeline {}(session {})", getMediaPipeline().getId(), sessionId);
        getMediaPipeline().release();
        mediaPipeline = null;
        log.info("Destroying kurentoClient (session {})", sessionId);
        try{
        	getKurentoClient().destroy();
        }catch(Exception e ){
        	log.error("failed to release kurento client",e);
        }
        kurentoClient = null;
    }
    
    
    public String getSessionId() {
	return sessionId;
    }	

    public CallEventWaitTimer getCallEventWaitTimer() {
        return callEventWaitTimer;
    }

    public void setCallEventWaitTimer(CallEventWaitTimer callEventWaitTimer) {
        this.callEventWaitTimer = callEventWaitTimer;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = userSettings;
    }

    public NuboFaceDetector getFaceDetector() {
        return faceDetector;
    }

    public void setFaceDetector(NuboFaceDetector faceDetector) {
        this.faceDetector = faceDetector;
    }

    public SipEndpoint getSipEndpoint() {
        return sipEndpoint;
    }

    public void setSipEndpoint(SipEndpoint sipEndpoint) {
        this.sipEndpoint = sipEndpoint;
    }

	public String getKmsUrl() {
		return kmsUrl;
	}

	public void setKmsUrl(String kmsUrl) {
		this.kmsUrl = kmsUrl;
	}
    
}
