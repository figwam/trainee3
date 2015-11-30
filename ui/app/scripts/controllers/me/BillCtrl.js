'use strict';

/*global app: false */

/**
 * The bill controller.
 *
 */
app.controller('BillCtrl', ['$modal','ModalService', '$scope', '$rootScope', 'AlertFactory', '$http', '$templateCache', function($modal, ModalService, $scope, $rootScope, AlertFactory, $http, $templateCache) {

  $scope.retrieve = function() {
    $http({
      method: "GET",
      url: "/trainees/me/subscriptions/"+$rootScope.trainee.subscription.id+"/bills",
      cache: $templateCache}).
    then(function(response) {
      $scope.status = response.status;
      $scope.data = response.data;
      $rootScope.trainee.subscription.bills = response.data
    }, function(response) {
      $scope.data = response.data || "Request failed";
      $scope.status = response.status;
    });
  };

}]);




