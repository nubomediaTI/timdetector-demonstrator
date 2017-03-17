package com.tilab.ca.sip_endpoint;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSipListener implements SipListener {
	
	private static final int MAX_NUM_INVITE_ATTEMPTS = 3;

	private SipEndpoint sipEndpoint;
	private int numInviteAttempts = 0;
	
	private SipClient sipClient;

	private static final Logger log = LoggerFactory.getLogger(DefaultSipListener.class);

	public DefaultSipListener(SipEndpoint sipEndpoint,SipClient sipClient) { //SipProvider sipProvider
		super();
		this.sipEndpoint = sipEndpoint;
		this.sipClient = sipClient;
	}
	
	public SipEndpoint getSipEndpoint() {
		return sipEndpoint;
	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent arg0) {
		log.info("received process dialog terminated!");
	}

	@Override
	public void processIOException(IOExceptionEvent ioExceptionEvent) {
		log.error("received io exception event", ioExceptionEvent);

	}

	@Override
	public void processRequest(RequestEvent requestEvent) {
		log.info("******************received request event **********");
		log.info("METHOD is "+requestEvent.getRequest().getMethod().toString());
		log.info("REQUEST is "+requestEvent.getRequest().toString());
		log.info("********END**********");
		
		switch(requestEvent.getRequest().getMethod()){
			case Request.BYE: processBye(requestEvent);
		}
	}
	
	
	private void processBye(RequestEvent requestEvent){
		log.info(" -----------> received BYE event!! $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
		sipEndpoint.setSipEndpointStatus(SipEndpointStatus.WAITING);
		sipEndpoint.getCallEndedListener().invoke();
		sipClient.sendOkResponse(requestEvent);
	}

	@Override
	public void processResponse(ResponseEvent responseEvent) {
		// Get the response.
		Response response = responseEvent.getResponse();
		// Display the response message
		log.info("*************************************************************************************");
		log.info("Received response: " + response.toString());
		log.info("*************************************************************************************");
		try {
			ClientTransaction tid = responseEvent.getClientTransaction();
			CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
			Dialog dialog = tid.getDialog();
			log.info("transaction state is " + tid.getState());
			log.info("Dialog = " + dialog);
			log.info("cseq is " + cseq);
			log.info("Request method related to this response is "+cseq.getMethod());
			if(dialog!=null)
				log.info("Dialog State is " +dialog.getState());

			switch (cseq.getMethod()) {
			case Request.REGISTER:
				handleRegistration(response, tid);
				break;
			case Request.INVITE:
				handleInvite(response, tid);
				break;
			case Request.CANCEL:
				if (dialog.getState() == DialogState.CONFIRMED) {
					// oops cancel went in too late. Need to hang up the dialog.
					log.info("Sending BYE -- cancel went in too late !!");
					sipClient.sendBye(dialog);
				}
				break;
			}
		} catch (Exception ex) {
			if (sipEndpoint.getErrorListener() != null)
				sipEndpoint.getErrorListener().onError(ex);
			else
				log.error("failed to process response", ex);
		}

	}

	@Override
	public void processTimeout(TimeoutEvent timeoutEvent) {
		log.error("received timeout event to request "+timeoutEvent.getClientTransaction().getRequest().getMethod());
		switch(timeoutEvent.getClientTransaction().getRequest().getMethod()){
			case Request.REGISTER:
				sipEndpoint.setSipEndpointStatus(SipEndpointStatus.IN_ERROR);
				sipEndpoint.getRegistrationFailedListener().onFailure(Response.REQUEST_TIMEOUT);
				break;
			case Request.INVITE:
				sipEndpoint.setSipEndpointStatus(SipEndpointStatus.WAITING);
				sipEndpoint.getCallFailedListener().onFailure(Response.REQUEST_TIMEOUT);
				break;
		}
	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
		log.info("received transaction terminated event!");
		log.info(transactionTerminatedEvent.getClientTransaction().getRequest().getMethod().toString());
	}
	
	
	
	
	private void handleRegistration(Response response,ClientTransaction tid) throws Exception{
		sipClient.handleChallenge(response, tid,()-> {
			switch(response.getStatusCode()){
				case Response.OK:
					log.info("registration success!!!!");
					sipEndpoint.setRegistered(true);
					if (sipEndpoint.getRegistrationSuccessListener() != null) {
						log.debug("calling registration success listener ..");
						sipEndpoint.getRegistrationSuccessListener().invoke();
					}
					sipEndpoint.setSipEndpointStatus(SipEndpointStatus.WAITING);
					break;
				default:
					log.info("received response code "+response.getStatusCode()+" recognized as error. Calling registration failed listener..");
					sipEndpoint.getRegistrationFailedListener().onFailure(response.getStatusCode());
					sipEndpoint.setSipEndpointStatus(SipEndpointStatus.IN_ERROR);
			}
		});
	}
	
	private void handleInvite(Response response,ClientTransaction tid) throws Exception{
		sipClient.handleChallenge(response, tid,
				()->{
					switch(response.getStatusCode()){
					case Response.OK:
						handleInviteOK(response,tid);
						break;
					case Response.TRYING:
						log.info("Trying..");
						break;
					case Response.RINGING:
						log.info("Ringing..");
						break;
					case Response.SESSION_PROGRESS:
						log.info("session progress..");
						break;
					default:
						log.warn("received response code "+response.getStatusCode()+" recognized as error. Calling call failed listener..");
						//status backs to waiting until the count reaches the maximum number of attempts. Afterwards it change to error status (the channel is unavailable)
						if(numInviteAttempts>=MAX_NUM_INVITE_ATTEMPTS) 
							sipEndpoint.setSipEndpointStatus(SipEndpointStatus.IN_ERROR);
						else
							sipEndpoint.setSipEndpointStatus(SipEndpointStatus.WAITING); 
						sipEndpoint.getCallFailedListener().onFailure(response.getStatusCode());
						numInviteAttempts++;
					}
				});
	}
	
	private void handleInviteOK(Response response,ClientTransaction tid){
		Dialog dialog = tid.getDialog();
		log.debug("Dialog after 200 OK  " + dialog);
		log.debug("Dialog State after 200 OK  " + dialog.getState());
		try {
			log.debug("Sending ACK");
			dialog.sendAck(dialog.createAck(((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getSeqNumber()));
			String sdpAnswer = new String(response.getRawContent());
			log.trace("response content sdpAnswer is: " + sdpAnswer);
			sipEndpoint.getRtpEndpoint().processAnswer(sdpAnswer);
			sipEndpoint.setSipEndpointStatus(SipEndpointStatus.IN_CALL);
			if (sipEndpoint.getCallSuccessListener() != null)
				sipEndpoint.getCallSuccessListener().onCallSuccess(sipEndpoint.getRtpEndpoint());

		} catch (Exception e) {
			sipEndpoint.setSipEndpointStatus(SipEndpointStatus.IN_ERROR);
			if(sipEndpoint.getErrorListener()!=null)
				sipEndpoint.getErrorListener().onError(e);
		} 
	}
}
