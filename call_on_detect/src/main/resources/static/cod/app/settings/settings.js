'use strict';

angular.module('cod.settings', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/settings', {
    templateUrl: 'settings/settings.html',
    controller: 'SettingsCtrl',
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

.controller('SettingsCtrl', ['$scope','$log','wsService','$location','$rootScope','currSettings',function($scope,$log,wsService,$location,$rootScope,currSettings) {

    $scope.settings = currSettings.value;

    $scope.uploadSettings = function(){
      var settingsRespHandler = wsService.registerResponseHandler(1,function(result){
        $log.debug("settings updated successfully!");
        $log.debug(result);
        currSettings.value = $scope.settings;
        //delete handler
        wsService.deleteResponseHandler(1,settingsRespHandler);
        $scope.$emit("notification",{message:"settings saved successfully",type:"info"});
        $location.path("/video");
      },function(error){
        $log.error(error);
        $scope.$emit("notification",{message:"failed to save settings. Please try again",type:"error"});
      });
      wsService.sendRequest(1,"settings",$scope.settings);
    };

}]);
