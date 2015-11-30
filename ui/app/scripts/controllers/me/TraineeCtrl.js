'use strict';

/*global app: false */

/**
 * The trainee controller.
 *
 */
app.controller('TraineeCtrl', ['$http','$scope','$templateCache','$rootScope', 'AlertFactory', function($http, $scope, $templateCache, $rootScope, AlertFactory) {

  $scope.update = function() {
    $scope.temptrainee = $rootScope.trainee
    if($scope.formData.firstname) $scope.temptrainee.firstname = $scope.formData.firstname
    if($scope.formData.lastname) $scope.temptrainee.lastname = $scope.formData.lastname
    if($scope.formData.mobile) $scope.temptrainee.mobile = $scope.formData.mobile
    if($scope.formData.phone) $scope.temptrainee.phone = $scope.formData.phone
    if($scope.formData.zip) $scope.temptrainee.address.zip = $scope.formData.zip
    if($scope.formData.street) $scope.temptrainee.address.street = $scope.formData.street
    if($scope.formData.city) $scope.temptrainee.address.city = $scope.formData.city
    if($scope.formData.email) $scope.temptrainee.email = $scope.formData.email
    $http({
      method: "PUT",
      url: "/trainees/me",
      data: $scope.temptrainee,
      headers: { 'Content-Type': 'application/json; charset=UTF-8'},
      cache: $templateCache}).
    then(function(response) {
      $scope.status = response.status
      $scope.data = response.data
      AlertFactory.addAlert(response.data.message, "success")
    }, function(response) {
      $scope.data = response.data
      $scope.status = response.status
      AlertFactory.addAlert(response.data.message, "danger")
    });
  };

}]);




