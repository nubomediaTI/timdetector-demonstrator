"use strict";

angular.module('cod').service("soundService",['$log',function($log){

  var soundService = {call:{}};

  soundService.call.callingTone = new Audio("http://soundbible.com/mp3/Telephone%20Ring-SoundBible.com-770479245.mp3");
  soundService.call.callingTone.loop = true;

  soundService.callbusyTone = new Audio("http://soundbible.com/mp3/Busy Signal-SoundBible.com-1695161320.mp3");
  soundService.call.hangupTone = new Audio("http://soundbible.com/mp3/Answering Machine Beep-SoundBible.com-1804176620.mp3");

  return soundService;
}]);
