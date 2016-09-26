"use strict";

angular.module('cod').service("kurentoService",['$log',function($log){
  var kurentoService = {};

  kurentoService.createWebRTCPeer = function(options,onSuccess,onError){
      function onOffer(error,offerSdp){
        if(error)
          onError(error);
          else
          onSuccess(offerSdp);
      };

      if(options.remoteVideo){
          return new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function(error) {
              if (error) {
                onError(error);
              }else{
                this.generateOffer(onOffer);
              }
            });
      }

      return  new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
        function (error) {
          if(error)
            onError(error);
          else
            this.generateOffer(onOffer);
        });
  };

  return kurentoService;
}]);
