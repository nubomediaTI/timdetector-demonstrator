'use strict';

angular.module('cod.registration', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/registration', {
    templateUrl: 'registration/registration.html',
    controller: 'RegistrationCtrl'
  });
}])

.controller('RegistrationCtrl', ['$scope','$log','wsService','$location',
                                  function($scope,$log,wsService,$location) {

    var registerRespHandler = wsService.registerResponseHandler(1,function(result){
      $log.debug("user registered successfully!");
      $log.debug(result);
      //delete handler
      wsService.deleteResponseHandler(1,registerRespHandler);
      $scope.$emit("notification",{message:"user registered successfully",type:"info"});
      $location.path("/login");
    },function(error){
      $log.error(error);
      $scope.$emit("notification",{message:"failed register. Please try again",type:"error"});
    });

    $scope.register = function(user){
      wsService.sendRequest(1,"register",$scope.settings);
    };
}]);
