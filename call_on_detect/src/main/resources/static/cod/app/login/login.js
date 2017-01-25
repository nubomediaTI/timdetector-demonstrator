'use strict';

angular.module('cod.login', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/login', {
    templateUrl: 'login/login.html',
    controller: 'LoginCtrl'
  });
}])

.controller('LoginCtrl', ['$scope','$log','wsService','$location','$rootScope',"currSettings","currUser",function($scope,$log,wsService,$location,$rootScope,currSettings,currUser) {
    $scope.isLoading = false;

    $scope.login = function(credentials){
      $scope.isLoading = true;
      var loginRespHandler = wsService.registerResponseHandler(1,function(result){
        $log.debug("login success!");
        $log.debug(result);
        $rootScope.$broadcast('user.logged',$scope.credentials.username);
        $scope.$emit("notification",{message:"login success!",type:"info"});
        currUser.authenticated = true;
        //delete handler
        wsService.deleteResponseHandler(1,loginRespHandler);
        $scope.isLoading = false;
        currSettings.value = JSON.parse(result.settings);
        if(currSettings.value && currSettings.value.destUser){
            $location.path("/video");
        }else{
          $location.path("/settings");
        }
      },function(error){
        $log.error(error);
        $scope.$emit("notification",{message:"login failed",type:"error"});
        $scope.isLoading = false;
        wsService.deleteResponseHandler(1,loginRespHandler);
      });
      wsService.sendRequest(1,"login",credentials);
    };
}]);
