'user strict';
angular.module('cod.codVideo',['oitozero.ngSweetAlert','angular-svg-round-progressbar'])
.constant("GlobalStates",{
  NOT_STARTED:"notStarted",
  LOADING:'loading',
  WORKING:'working',
  ERROR: 'error'
})
.constant("AvailStates",{
  I_CAN_START:0,
  I_CAN_STOP:1,
  I_AM_STARTING:2
})
.constant("IDS",{
  START:2,
  ICE_CANDIDATE_LOCAL:3,
  STOP:4,
  START_AUDIO:5,
  ICE_CANDIDATE_LOCAL_AUDIO:6
})
.constant("METHODS",{
  START_RTSP_PLAYER: "startWithRtspPlayerAsSource",
  START_LOCAL_CAM: "startWithWebRtcAsSource",
  START_CALL: "startSipCall",
  ON_ICE_CANDIDATE:"onIceCandidate",
  ON_ICE_CANDIDATE_AUDIO:"onIceCandidateRec",
  STOP:"stop"
})
.constant("NOTIFICATIONS",{
  ICE_CANDIDATE:"iceCandidate",
  ICE_CANDIDATE_AUDIO:"iceCandidateRec",
  SIP_REGISTRATION:"sipRegistration",
  FACE_DETECTED:"faceDetected",
  CALL:"call"
})
.directive("codVideo",["wsService","AvailStates","$log","kurentoService",
                       "METHODS","IDS","NOTIFICATIONS","GlobalStates","soundService","$interval",
                       function(wsService,AvailStates,$log,kurentoService,METHODS,IDS,NOTIFICATIONS,GlobalStates,soundService,$interval){

  function setSpinner(mediaElement){
    mediaElement.poster = './images/transparent-1px.png';
    mediaElement.style.background = "center transparent url('./images/spinner.gif') no-repeat";
  }

  function removeSpinner(mediaElement){
    mediaElement.src = '';
    mediaElement.poster = './images/webrtc.png';
    mediaElement.style.background = '';
  }

  function onError(scope,message,error){
    $log.error(error);
    scope.$emit("notification",{message:message,type:"error"});
  }

  function notify(scope,message){
    scope.$emit("notification",{message:message,type:"info"});
  }

  return {
    restrict: 'AE',
    transclude: false,
    scope: {
      settings:"@settings"
    },
    templateUrl: "directives/cod-video.html",
    link: function(scope,elem,attr){
      var videoElement = document.getElementById('cameraVideo');
      var audioElement = document.getElementById('sipAudio');
      var currState = AvailStates.I_CAN_START;
      var settings = JSON.parse(scope.settings).value;
      var webRtcPeer;
      var webRtcPeerAudio;
      var currTimer;

      scope.callStarted = false;
      scope.globalState = GlobalStates.NOT_STARTED;

      scope.waitSecs = 0;

      function updateWaitTimer(){
         scope.waitSecs--;
         if(scope.waitSecs<0)
          $interval.cancel(currTimer);
      }

      function setWaitTimer(timerMillis){
        scope.waitSecs = timerMillis/1000;
        currTimer = $interval(updateWaitTimer, 1000);
      }

      scope.isDisabled = function(btnName){
        switch(btnName){
          case "start": return currState != AvailStates.I_CAN_START;
          case "stop": return currState != AvailStates.I_CAN_STOP;
        }
      }

      scope.$watch(function(){return currState},function(newValue,oldValue){
        if(newValue === AvailStates.I_AM_STARTING){
          setSpinner(videoElement);
        }
        if(oldValue && newValue ==  AvailStates.I_CAN_START){
          removeSpinner(videoElement);
        }
      });

      //method starts on button start click
      scope.start = function(){
        $log.debug("starting video..");
        currState = AvailStates.I_AM_STARTING;
        scope.globalState = GlobalStates.LOADING;
        //options needed by kurento utils WebRtcPeerRecvonly
        //setting ice candidate sender method and remote or local video depending if local or ip cam choosen on application settings phase
        $log.debug("rtspUrl is "+settings.rtspUrl);

        var options = {
          remoteVideo: settings.rtspUrl?videoElement:null,
          localVideo: settings.rtspUrl?null:videoElement,
          onicecandidate: function(candidate){
            $log.debug("Local candidate" + JSON.stringify(candidate));
            wsService.sendRequest(IDS.ICE_CANDIDATE_LOCAL,METHODS.ON_ICE_CANDIDATE,{candidate: candidate});
          }
        }

        //calling startWithRtspPlayerAsSource if remote cam selected, with startWithWebRtcAsSource otherwise
        webRtcPeer = kurentoService.createWebRTCPeer(options,function(offerSdp){
          $log.debug('Invoking SDP offer callback function');
          wsService.sendRequest(IDS.START,settings.rtspUrl?METHODS.START_RTSP_PLAYER:METHODS.START_LOCAL_CAM,{sdpOffer : offerSdp});
        }),function(error){
          onError(scope,"failed to start the video. Please try again later or check browser console log for more details",error);
          scope.globalState = GlobalStates.ERROR;
        };
      }

      //called to stop the video call
      scope.stop = function() {
        $log.debug("Stopping video call ...");
        currState = AvailStates.I_CAN_START;
        scope.globalState = GlobalStates.NOT_STARTED;
        if (webRtcPeer) {
          webRtcPeer.dispose();
          webRtcPeer = null;
          wsService.sendRequest(IDS.STOP,METHODS.STOP,{});
        }
        if(webRtcPeerAudio){
          webRtcPeerAudio.dispose();
          webRtcPeerAudio = null;
        }
        if(!soundService.call.callingTone.paused)
          soundService.call.callingTone.pause();

        scope.callStarted = false;

        if(currTimer){
          scope.waitSecs = 0;
        }

        $log.debug("Call On detect stopped");
      }
      /**
      handlers video registration
      **/
      wsService.registerResponseHandler(IDS.START,function(message){
        currState = AvailStates.I_CAN_STOP;
        $log.debug("video SDP answer received from server. Processing ...");

        webRtcPeer.processAnswer (message.sdpAnswer, function (error) {
          if (error){
            scope.globalState = GlobalStates.ERROR;
            return onError(scope,"failed to process video sdp Answer",error);
          }
          $log.debug("video sdp answer processed successfully");
        });
      },function(error){
        onError(scope,"failed to start video on remote server",error)
        scope.globalState = GlobalStates.ERROR;
      });

      wsService.registerNotificationHandler(NOTIFICATIONS.ICE_CANDIDATE,function(message){
        webRtcPeer.addIceCandidate(message.candidate, function (error) {
          if (error)
          onError(scope,"Error adding ice candidate",error);
        });
      },function(error){
        onError(scope,"failed to receive ice candidate",error);
      });

      wsService.registerNotificationHandler(NOTIFICATIONS.SIP_REGISTRATION,function(message){
        if(message == "failed"){
          onError(scope,"failed to register sip account","An internal Error has occurred. Please try again later");
          scope.stop();
        }else{
          scope.globalState = GlobalStates.WORKING;
        }
      });

      wsService.registerNotificationHandler(NOTIFICATIONS.FACE_DETECTED,function(message){
        soundService.call.callingTone.play();
        notify(scope,"calling "+settings.destUser);
        scope.callStarted = true;
        startAudioReceiver();
      });


      /*
      AUDIO RECEIVER SECTION
      */
      function startAudioReceiver(){
        $log.debug("starting audio receiver...");
        //test to try multiple call to sip in one video session
        if(webRtcPeerAudio){
          return wsService.sendRequest(IDS.START_AUDIO,METHODS.START_CALL,{});
        }
        //------------------------------------------------------
        var options = {
          remoteVideo: audioElement,
          mediaConstraints:{
            video: settings.videoRec,
            audio: true
          },
          onicecandidate: function(candidate){
            $log.debug("Local candidate audio " + JSON.stringify(candidate));
            wsService.sendRequest(IDS.ICE_CANDIDATE_LOCAL_AUDIO,METHODS.ON_ICE_CANDIDATE_AUDIO,{candidate: candidate});
          }
        }

        //calling start audio
        webRtcPeerAudio = kurentoService.createWebRTCPeer(options,function(offerSdp){
          $log.debug('Invoking SDP audio offer callback function');
          wsService.sendRequest(IDS.START_AUDIO,METHODS.START_CALL,{sdpOffer : offerSdp});
        },function(error){
          onError(scope,"failed to start audio. Please try again later or check browser console log for more details",error);
          scope.globalState = GlobalStates.ERROR;
        });
      }

      wsService.registerResponseHandler(IDS.START_AUDIO,function(message){
        $log.debug("audio SDP answer received from server. Processing ...");
        if(message.sdpAnswer){
          webRtcPeerAudio.processAnswer (message.sdpAnswer, function (error) {
            if (error) return onError(scope,"failed to process audio sdp Answer",error);
            $log.debug("audio sdp answer processed successfully");
          });
        }else{
          $log.debug("rtc endpoint already negotiated. No need to renegotiate.");
        }
      },function(error){
        onError(scope,"failed to start audio on remote server",error);
        scope.globalState = GlobalStates.ERROR;
      });

      wsService.registerNotificationHandler(NOTIFICATIONS.ICE_CANDIDATE_AUDIO,function(message){
        webRtcPeerAudio.addIceCandidate(message.candidate, function (error) {
          if (error)
          onError(scope,"Error adding audio ice candidate",error);
        });
      },function(error){
        onError(scope,"failed to receive audio ice candidate",error);
      });

      wsService.registerNotificationHandler(NOTIFICATIONS.CALL,function(message){
          switch(message.status){
            case "ANSWERED":
                soundService.call.callingTone.pause();
                break;
            case "ENDED":
                notify(scope,"call ended");
                scope.callStarted = false;
                soundService.call.hangupTone.play();
                setWaitTimer(message.timer);
                break;
            case "FAILED":
                soundService.call.callingTone.pause();
                scope.callStarted = false;
                handleCallFailed(message.details);
                setWaitTimer(message.timer);
                break;
          }
      });

      scope.$on('$routeChangeStart', function(next, current) {
          wsService.deleteAllResponseHandlers();
          wsService.deleteAllNotificationHandlers();
      });

      function handleCallFailed(details){
        switch(details){
          case "requestTimeout":
                onError(scope,"failed to call dest user due to request timeout","failed to call dest user due to request timeout");
                break;
          case "busy":
                onError(scope,"called user is busy. please try again later","failed to call dest user due to call refuse");
                break;
          default:
               onError(scope,"something went wrong during the call. Prease try again on timer expiring");
        }
      }
    }
  }
}]);
