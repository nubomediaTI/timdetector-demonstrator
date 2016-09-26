angular.module('cod').controller("NavbarController",['$scope','$log',function($scope,$log){

  $scope.$on("user.logged",function(event,username){
      $log.debug("nav-bar: received user logged emission!");
      $scope.loggedUser = {
        username: username,
        avatarUrl:"https://cdn2.iconfinder.com/data/icons/faceavatars/PNG/D04.png"
      }
  });
}]);
