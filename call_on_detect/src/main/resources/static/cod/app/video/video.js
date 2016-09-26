'use strict';

angular.module('cod.video', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/video', {
    templateUrl: 'video/video.html',
    controller: 'VideoCtrl',
    resolve: {
      authorize: function(currUser,$q){
        if(!currUser.authenticated){
          throw "not logged";
        }
        return true;
      }
    }
  });
}])
.controller('VideoCtrl', ['$scope','$log','wsService','$location','currSettings',
                          function($scope,$log,wsService,$location,currSettings) {
      $log.debug(currSettings.value);
      $scope.currSettings = currSettings;
      $scope.firstAccess = true;
}]);
