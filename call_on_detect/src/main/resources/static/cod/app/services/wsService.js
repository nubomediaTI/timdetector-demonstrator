"use strict";

angular.module('cod').service("wsService",['$log','$location','$rootScope',function($log,$location,$rootScope){
  var wsService={};
  var registeredHandlers=[];

  var responseHandlers = {};
  var notificationsHandlers = {};

  var lws = new WebSocket('wss://' + $location.host() + ":" + $location.port() + '/callOnDetect');

  wsService.registerResponseHandler = function(id,onSuccess,onError){
    if(!responseHandlers[id])
      responseHandlers[id] = [];
    var hid = new Date().getTime();
    var currHandler = {id:hid,onSuccess:onSuccess,onError:onError};
    responseHandlers[id].push(currHandler);
    return currHandler;
  }

  wsService.registerNotificationHandler = function(method,onSuccess,onError){
    if(!responseHandlers[method])
      responseHandlers[method] = [];
    var hid = new Date().getTime();
    var currHandler = {id:hid,onSuccess:onSuccess,onError:onError};
    responseHandlers[method].push(currHandler);
    return currHandler;
  }

  wsService.deleteResponseHandler = function(id,handler){
    responseHandlers[id].splice(responseHandlers[id].indexOf(handler),1);
  }

  wsService.deleteNotificationHandler = function(method,handler){
    responseHandlers[method].splice(responseHandlers[method].indexOf(handler),1);
  }

  wsService.registerHandler = function(handler){
    registeredHandlers.push(handler);
  }

  wsService.deleteHandler = function(handler){
    registeredHandlers.splice(registeredHandlers.indexOf(handler),1);
  }

  wsService.sendMessage = function (message) {
    sendMessage(message);
  }

  wsService.sendRequest = function(id,method,params){
     var message ={
 			id:id,
 			method:method,
 			params:params
 		 };
     sendMessage(message);
  }

  lws.onmessage = function(message){
    var parsedMessage = JSON.parse(message.data);
    $log.debug('Received message: ' + message.data);
    if(parsedMessage.id){
        $log.debug("received response");
        if(responseHandlers[parsedMessage.id]){
          $rootScope.$apply(function(){
            if(parsedMessage.error)
              responseHandlers[parsedMessage.id].forEach(function(handler){
                if(handler.onError)
                  handler.onError(parsedMessage.error);
              });
            else{
              responseHandlers[parsedMessage.id].forEach(function(handler){
                handler.onSuccess(parsedMessage.result);
              });
            }
          });
        }else{
          $log.debug("received a message with id "+parsedMessage.id+" not associated to any handler");
        }
    }else{
        $log.debug("received notification");
        if(responseHandlers[parsedMessage.method]){
          $rootScope.$apply(function(){
            if(parsedMessage.error)
              responseHandlers[parsedMessage.method].forEach(function(handler){
                if(handler.onError)
                  handler.onError(parsedMessage.error);
              });
            else{
              responseHandlers[parsedMessage.method].forEach(function(handler){
                handler.onSuccess(parsedMessage.params);
              });
            }
          });
        }else{
          $log.debug("received a notification message with method "+parsedMessage.method+" not associated to any handler");
        }
    }
    if(registeredHandlers){
        $rootScope.$apply(function(){
          registeredHandlers.forEach(function(handler){
            handler(parsedMessage);
          });
        });
    }
  };

  lws.onerror = function(event){
     $log.error("An error has occurred on websocket connection");
     onError("an error has occurred on connection. Please reload the page.");
  }

  lws.onclose = function(event){
    if(event.code == 1000){
      $log.debug("normal websocket closure");
    }else{
      $log.error("unexpected websocket closure recognized as error. Error code is "+event.code);
      onError("an error has occurred on connection. Please reload the page.");
    }
  }

  function onError(message){
    $rootScope.$broadcast("notification",{type:"error",message:message});
  }

  wsService.dispose = function() {
    lws.close();
    registeredHandlers = [];
    responseHandlers = {};
    notificationsHandlers = {};
  };

  function sendMessage(message){
    var jsonMessage = JSON.stringify(message);
    $log.debug('Sending message: ' + jsonMessage);
    try{
      lws.send(jsonMessage);
    }catch(err){
        $log.error(err);
        onError("an error has occurred on connection. Please reload the page.");
    }
  }

  return wsService;

}]);
