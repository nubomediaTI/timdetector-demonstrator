package com.tilab.ca.call_on_detect;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.message.Response;

import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.module.nubofacedetector.NuboFaceDetector;
import org.kurento.module.nubofacedetector.OnFaceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.JsonObject;
import com.tilab.ca.call_on_detect.exception.InternalErrorCodes;
import com.tilab.ca.jrpcwrap.JrpcEventListener;
import com.tilab.ca.jrpcwrap.JrpcMethod;
import com.tilab.ca.jrpcwrap.JsonKey;
import com.tilab.ca.jrpcwrap.exception.AppException;
import com.tilab.ca.sip_endpoint.SipEndpoint;
import com.tilab.ca.sip_endpoint.SipEndpointStatus;
import com.tilab.ca.sip_endpoint.listeners.interfaces.SipEventListener;
import com.tilab.ca.sip_endpoint.listeners.interfaces.SipOperationFailedListener;
import com.tilab.ca.sip_endpoint.utils.SipUtils;

public class CallOnDetectHandler implements JrpcEventListener {

    private static final Logger log = LoggerFactory.getLogger(CallOnDetectHandler.class);

    private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();
    
     private final ConcurrentHashMap<String, String> credentialsMap = new ConcurrentHashMap<>();

    //@Autowired
    //private StringRedisTemplate redisTemplate;

    @Value("${serverEvents.timer.milliseconds:4000}")
    private int serverEventsTimerMilliseconds;

    @Value("${sip.host}")
    private String host;

    @Value("${sip.port:5060}")
    private int port;

    @Value("${sip.stunServer:#{null}}")
    private String stunServer;

    @Value("${sip.listenOnInterface:#{null}}")
    private String listenOnInterface;
    
    
    private static final String COD_LOGIN_KEY_PREFIX = "cod.username.";

    @JrpcMethod(name = "register")
    public void register(Transaction transaction, @JsonKey(name = "username") String username,
            @JsonKey(name = "password") String password,
            @JsonKey(name = "sipHost", optional = true) String sipHost,
            @JsonKey(name = "sipPort", optional = true) Integer sipPort) throws Exception {
        if (SipUtils.anyBlank(username, password)) {
            throw new IllegalArgumentException("provide a username or password");
        }
        String userKey = getLoginKey(username, password);
        //if (!this.redisTemplate.hasKey(userKey)) {
        if (!this.credentialsMap.containsKey(userKey)) {
            //ValueOperations<String, String> ops = this.redisTemplate.opsForValue();
            UserSettings settings = new UserSettings();
            settings.setUsername(username);
            settings.setPassword(password);
            if (!SipUtils.isBlank(sipHost)) {
                settings.setSipHost(sipHost);
            }
            if (sipPort != null && sipPort > 0) {
                settings.setSipPort(sipPort);
            }
            this.credentialsMap.put(userKey, JsonUtils.toJson(settings));
            //ops.set(userKey, JsonUtils.toJson(settings));
        } else {
            throw new IllegalArgumentException("username already exists on database");
        }
    }

    private String getLoginKey(String username, String password) {
        return COD_LOGIN_KEY_PREFIX+username;
    }
    
    @JrpcMethod(name = "login")
    public void login(Transaction transaction, @JsonKey(name = "username") String username,
                        @JsonKey(name = "password") String password) throws Exception {
    	String loginKey = getLoginKey(username, password);
    	//if (!this.redisTemplate.hasKey(loginKey)) {
        if (!this.credentialsMap.containsKey(loginKey)) {
    		transaction.sendError(400, "Bad Request", "invalid username or password");
    		return;
    	}
    	//ValueOperations<String, String> ops = this.redisTemplate.opsForValue();
    	//UserSettings userSettings = JsonUtils.fromJson(ops.get(loginKey), UserSettings.class);
        UserSettings userSettings = JsonUtils.fromJson(this.credentialsMap.get(loginKey), UserSettings.class);
    	
    	if (!userSettings.getPassword().equals(password)) {
    		transaction.sendError(400, "Bad Request", "invalid username or password");
    		return;
    	}
        String sessionId = transaction.getSession().getSessionId();
        log.info("creating user session for session id "+sessionId);
        UserSession user = users.get(sessionId);
        
        if(user == null){
            user = new UserSession(sessionId);
            user.setUserSettings(userSettings);
            users.put(sessionId, user);
        }
        
        sendResponse(transaction, "settings", userSettings);
    }
    
    @JrpcMethod(name = "settings")
    public void settings(Transaction transaction,
                        @JsonKey(name = "destUser") String destUser,
                        @JsonKey(name = "kmsIp", optional = true) String kmsIp,
                        @JsonKey(name = "sipHost", optional = true) String sipHost,
                        @JsonKey(name = "sipPort", optional = true) Integer sipPort,
                        @JsonKey(name = "rtspUrl", optional = true) String rtspUrl) throws Exception {
    	
        String sessionId = transaction.getSession().getSessionId();
        log.info("saving setting for session id "+sessionId);
        UserSession user = users.get(sessionId);
        //to substitute with new control now commented once developed new version
        if(user == null){
            user = new UserSession(sessionId);
            user.setUserSettings(new UserSettings());
            users.put(sessionId, user);
        }
        log.info("received kmsIp in settings: "+kmsIp);
        
        UserSettings settings = user.getUserSettings();
        settings.setRtspUrl(rtspUrl);
        settings.setDestUser(destUser);
        if(!SipUtils.isBlank(sipHost))
        	settings.setSipHost(sipHost);
        if(sipPort!=null && sipPort>0)
        	settings.setSipPort(sipPort);
        settings.setKmsIp(kmsIp);
        
        /*
        ValueOperations<String, String> ops = this.redisTemplate.opsForValue();
        ops.set(userKey, JsonUtils.toJson(settings));
        */
        String userKey = getLoginKey(user.getUserSettings().getUsername(), user.getUserSettings().getPassword());
        this.credentialsMap.put(userKey, JsonUtils.toJson(settings));
        
        log.info("saved setting for session id "+sessionId);
    }
    
    
    @JrpcMethod(name = "startWithWebRtcAsSource")
    public void startWithWebRtcAsSource(Transaction transaction, @JsonKey(name = "sdpOffer") String sdpOffer) throws Exception {
        String sessionId = transaction.getSession().getSessionId();
        UserSession userSession = users.get(sessionId);
        if (userSession == null) {
            throw new AppException(InternalErrorCodes.USER_NOT_AUTHENTICATED, "no active user session found");
        }
        WebRtcEndpoint webRtcEndpoint = userSession.getWebRtcEndpoint();
        //Ice Candidate
        webRtcEndpoint.addIceCandidateFoundListener((event) -> {
            JsonObject response = new JsonObject();
            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
            try {
                transaction.getSession().sendNotification("iceCandidate", response);
            } catch (IOException ex) {
                log.error("failed to send ice candidate", ex);
            }
        });

        //register sipEndpoint
        registerSipEndpoint(userSession, transaction, () -> {
            log.info("user " + userSession.getUserSettings().getUsername() + " registered successfully!");
            //set nubo face detector
            NuboFaceDetector face = addNuboFaceDetector(webRtcEndpoint, webRtcEndpoint.getMediaPipeline(), userSession, transaction.getSession());
            face.connect(webRtcEndpoint);
            sendNotification(transaction, NotificationKeys.SIP_REGISTRATION.getKey(), "success");
        }, (error) -> {
            log.error("failed registration for user " + userSession.getUserSettings().getUsername(), error);
            sendNotification(transaction, NotificationKeys.SIP_REGISTRATION.getKey(), "failed");
        });
        //send sdpAnswer
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
        // Sending response back to client
        log.info("sending sdpAnswer " + sdpAnswer);
        sendResponse(transaction, "sdpAnswer", sdpAnswer);
        webRtcEndpoint.gatherCandidates();
    }

    @JrpcMethod(name = "startWithRtspPlayerAsSource")
    public void startWithRtspPlayerAsSource(Transaction transaction, @JsonKey(name = "sdpOffer") String sdpOffer) throws Exception {
        String sessionId = transaction.getSession().getSessionId();
        UserSession userSession = users.get(sessionId);
        if (userSession == null) {
            throw new IllegalStateException("settings for user has to be defined before to call startWithWebRtcAsSource method");
        }
        WebRtcEndpoint webRtcEndpoint = userSession.getWebRtcEndpoint();
        PlayerEndpoint playerEndpoint = userSession.getPlayerEndpoint();

        //register sipEndpoint
        registerSipEndpoint(userSession, transaction, () -> {
            log.info("user " + userSession.getUserSettings().getUsername() + " registered successfully!");
            //set nubo face detector
            NuboFaceDetector nuboFaceDetector = addNuboFaceDetector(playerEndpoint, userSession.getMediaPipeline(), userSession, transaction.getSession());
            nuboFaceDetector.connect(webRtcEndpoint);
        }, (error) -> {
            log.error("failed registration for user " + userSession.getUserSettings().getUsername() + " with error code " + error);
            sendNotification(transaction, "sipRegistration", "failed");
        });
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
        // Sending response back to client
        log.info("sending sdpAnswer " + sdpAnswer);
        sendResponse(transaction, "sdpAnswer", sdpAnswer);
        webRtcEndpoint.gatherCandidates();
    }

    private void sendResponse(Transaction transaction, String respKey, Object respValue) {
        JsonObject response = new JsonObject();
        String val = respValue instanceof String ? (String) respValue : JsonUtils.toJson(respValue);
        response.addProperty(respKey, val);
        try {
            transaction.sendResponse(response);
        } catch (IOException e) {
            log.error("failed to send response", e);
        }
    }

    private void sendNotification(Transaction transaction, String notKey, Object notVal) {
        sendNotification(transaction.getSession(), notKey, notVal);
    }

    private void sendNotification(Session session, String notKey, Object notVal) {
        try {
            session.sendNotification(notKey, notVal);
        } catch (IOException ex) {
            log.error("failed to send notification", ex);
        }
    }

    private NuboFaceDetector addNuboFaceDetector(MediaElement endpointToConnect, MediaPipeline mediaPipeline, final UserSession userSession, final Session session) {
        log.info("**adding nubo face detector**");
        NuboFaceDetector face = new NuboFaceDetector.Builder(mediaPipeline).build();
        face.showFaces(1);
        face.activateServerEvents(1, serverEventsTimerMilliseconds);
        face.addOnFaceListener((OnFaceEvent event) -> {
            log.info("**face recognized!**");
            CallEventWaitTimer ct = userSession.getCallEventWaitTimer();
            log.info(userSession.getSipEndpoint().getSipEndpointStatus().toString());
            if (userSession.getSipEndpoint().getSipEndpointStatus() == SipEndpointStatus.WAITING) {
                if (ct == null || ct.hasExpired()) {
                    if (ct == null) {
                        ct = new CallEventWaitTimer(CallEventWaitTimer.T_1_MINUTE);
                        userSession.setCallEventWaitTimer(ct);
                    } else {
                        ct.reset();
                    }
                    log.info("sending notification faceDetected to client..");
                    sendNotification(session, NotificationKeys.FACE_DETECTED.getKey(), "");
                }
            }
        });
        endpointToConnect.connect(face);
        userSession.setFaceDetector(face);
        return face;
    }

    private void registerSipEndpoint(UserSession user, Transaction transaction, SipEventListener rsh, SipOperationFailedListener sof) throws Exception {
        SipEndpoint sipEndpoint = new SipEndpoint.Builder()
                .mediaPipeline(user.getMediaPipeline())
                .password(user.getUserSettings().getPassword())
                .username(user.getUserSettings().getUsername())
                .kmsUrl(user.getKmsUrl())
                .host(SipUtils.isBlank(user.getUserSettings().getSipHost()) ? host : user.getUserSettings().getSipHost())
                .port(port)
                .withStunServer(stunServer)
                .listenOnInterface(listenOnInterface)
                .build();

        //sipEndpoint.setKmsIp(user.getUserSettings().getKmsIp());
        sipEndpoint.addRegistrationListeners(rsh, sof);

        sipEndpoint.addErrorListener((Exception e) -> {
            log.error("error on sip endpoint!", e);
            sof.onFailure(-1);
        });

        user.setSipEndpoint(sipEndpoint);
        sipEndpoint.register();
    }

    @JrpcMethod(name = "startSipCall")
    public void startSipCall(Transaction transaction, @JsonKey(name = "sdpOffer", optional = true) String sdpOffer) throws Exception {
        UserSession userSession = users.get(transaction.getSession().getSessionId());
        if (userSession == null) {
            throw new IllegalStateException("cannot perform sip call before settings and detection on");
        }
        WebRtcEndpoint webRtcEndpointRec = userSession.getWebRtcEndpointReceiver();

        log.info("webrtcEndpoint rec num source connections is " + webRtcEndpointRec.getSourceConnections().size());

        SipEndpoint sipEndpoint = userSession.getSipEndpoint();

        if (!SipUtils.isBlank(sdpOffer)) {
            //Ice Candidate
            webRtcEndpointRec.addIceCandidateFoundListener((IceCandidateFoundEvent event) -> {
                JsonObject response = new JsonObject();
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                try {
                    transaction.getSession().sendNotification("iceCandidateRec", response);
                } catch (IOException ex) {
                    log.error("failed to send ice candidate", ex);
                }
            });
        } else {
            webRtcEndpointRec.disconnect(sipEndpoint.getRtpEndpoint());
        }

        sipEndpoint.startSipCall(userSession.getUserSettings().getDestUser(), userSession.getUserSettings().getSipHost());
        JsonObject resp = new JsonObject();

        sipEndpoint.addCallListeners((sdpEndpoint) -> {
            userSession.getWebRtcEndpoint().connect(sdpEndpoint);
            sdpEndpoint.connect(webRtcEndpointRec);
            log.info("call started successfully");
            resp.addProperty("status", CallStatuses.ANSWERED.toString());
            sendNotification(transaction, NotificationKeys.CALL.getKey(), resp);
        }, (failureResponseCode) -> {
            log.info("call failed. Failure response code is " + failureResponseCode);
            resp.addProperty("status", CallStatuses.FAILED.toString());
            switch (failureResponseCode) {
                case Response.REQUEST_TIMEOUT:
                    resp.addProperty("details", "requestTimeout");
                case Response.DECLINE:
                case Response.BUSY_HERE:
                    resp.addProperty("details", "busy");
            }
            userSession.getCallEventWaitTimer().reset();
            resp.addProperty("timer", userSession.getCallEventWaitTimer().getRemainingTime());
            sendNotification(transaction, NotificationKeys.CALL.getKey(), resp);
        });

        sipEndpoint.setCallEndedListener(() -> {
            log.info("call terminated. sending notification to client.");
            userSession.getCallEventWaitTimer().reset();
            resp.addProperty("status", CallStatuses.ENDED.toString());
            resp.addProperty("timer", userSession.getCallEventWaitTimer().getRemainingTime());
            sendNotification(transaction, NotificationKeys.CALL.getKey(), resp);
        });

        //if is the client webrtc endpoint has to be initialized process offer 
        // otherwise the offer is blank and just answer ok
        if (!SipUtils.isBlank(sdpOffer)) {
            String sdpAnswer = webRtcEndpointRec.processOffer(sdpOffer);
            // Sending response back to client
            //log.info("sending sdpAnswer "+sdpAnswer);
            JsonObject response = new JsonObject();
            response.addProperty("sdpAnswer", sdpAnswer);
            transaction.sendResponse(response);
            webRtcEndpointRec.gatherCandidates();
        }
    }

    @JrpcMethod(name = "onIceCandidate")
    public void onIceCandidate(Transaction transaction, @JsonKey(name = "candidate") JsonObject candidate) {
        UserSession user = users.get(transaction.getSession().getSessionId());
        if (user != null) {
            user.addCandidate(candidate, false);
        }
    }

    @JrpcMethod(name = "onIceCandidateRec")
    public void onIceCandidateRec(Transaction transaction, @JsonKey(name = "candidate") JsonObject candidate) {
        UserSession user = users.get(transaction.getSession().getSessionId());
        if (user != null) {
            user.addCandidate(candidate, true);
        }
    }

    @JrpcMethod(name = "stop")
    public void stop(Transaction transaction) throws Exception {
        UserSession user = users.get(transaction.getSession().getSessionId());
        if (user != null) {
            //if the user is in call the call has to be stopped
            if (user.getSipEndpoint() != null && user.getSipEndpoint().getSipEndpointStatus() == SipEndpointStatus.IN_CALL) {
                //close the call
                user.getSipEndpoint().stopSipCall();
            }
            releaseSipEndpoint(user);
            user.release();
            users.remove(user.getSessionId());
            UserSession nuser = new UserSession(user.getSessionId());
            nuser.setUserSettings(user.getUserSettings());
            users.put(user.getSessionId(), nuser);
        }
    }

    private void releaseSipEndpoint(UserSession user) {
        SipEndpoint sipEndpoint = user.getSipEndpoint();
        try {
            if (sipEndpoint.isRegistered()) {
                sipEndpoint.addRegistrationListeners(() -> {
                    log.info("unregister success!");
                    disposeSipEndpoint(sipEndpoint);
                }, (error) -> {
                    log.info("unregister error code " + error);
                    disposeSipEndpoint(sipEndpoint);
                });
                user.getSipEndpoint().unregister();
            } else {
                disposeSipEndpoint(sipEndpoint);
            }
        } catch (Exception e) {
            log.error("failed to dispose sipEndpoint", e);
        }
    }

    private void disposeSipEndpoint(SipEndpoint sipEndpoint) {
        try {
            sipEndpoint.dispose();
        } catch (Exception e) {
            log.error("failed to dispose sipEndpoint", e);
        }
    }

    @Override
    public void afterConnectionClosed(Session session, String status) {
        log.info("connection closed. Removing user from session..");
        try {
            UserSession user = users.get(session.getSessionId());
            if (user != null) {
                log.info("user was {}. releasing resources..", user.getUserSettings().getUsername());
                if (user.getSipEndpoint() != null) {
                    releaseSipEndpoint(user);
                    if (user.getSipEndpoint().getSipEndpointStatus() == SipEndpointStatus.IN_CALL) {
                        //close the call
                        user.getSipEndpoint().stopSipCall();
                    }
                }
                user.release();
                users.remove(user.getSessionId());
            }
        } catch (Exception e) {
            log.error("failed to release resources after connection closed", e);
        }
    }
}
