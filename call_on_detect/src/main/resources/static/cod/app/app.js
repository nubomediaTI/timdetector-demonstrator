'use strict';

// Declare app level module which depends on views, and components
angular.module('cod', [
  'ngRoute',
  'cod.login',
  'cod.settings',
  'cod.video',
  'cod.version',
  'cod.codVideo'
]).
config(['$locationProvider', '$routeProvider', function($locationProvider, $routeProvider) {
  $locationProvider.hashPrefix('!');
  $routeProvider.otherwise({redirectTo: '/login'});
}])
.run(["$rootScope","$location",function($rootScope, $location){
  $rootScope.$on("$routeChangeError", function(evt, to, from, error) {
        $location.path("/login");
  });
}])
.value("currSettings",{})
.value("currUser",{"authenticated":false})
.controller("AppController",['$scope','$timeout','SweetAlert',function($scope,$timeout,SweetAlert){
   $scope.$on("notification",function(event,notification){
     if(notification.type == "error"){
        SweetAlert.swal("Error!", notification.message, "error");
     }else{
       $scope.notification = notification;
       $scope.toastClass = notification.type;
       $timeout(function(){
         $scope.notification = null;
       }, 5000);
     }
   });
}]);
