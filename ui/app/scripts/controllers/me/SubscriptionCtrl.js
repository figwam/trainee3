'use strict';

/*global app: false */

/**
 * The clazz controller.
 *
 */
app.controller('SubscriptionCtrl', ['$modal','ModalService', '$scope', '$rootScope', 'AlertFactory', '$http', '$templateCache', function($modal, ModalService, $scope, $rootScope, AlertFactory, $http, $templateCache) {

  $scope.retrieve = function(idSubscription) {
    $http({
      method: "GET",
      url: "/trainees/me/subscriptions/"+idSubscription,
      cache: $templateCache}).
    then(function(response) {
      $scope.status = response.status;
      $scope.data = response.data;
      $rootScope.trainee.subscription.canceledOn = response.data.canceledOn
      $rootScope.trainee.subscription.deletedOn = response.data.deletedOn
    }, function(response) {
      $scope.data = response.data || "Request failed";
      $scope.status = response.status;
    });
  };

  $scope.delete = function() {
    var modalDefaults = {
      templateUrl: '/views/me/deleteSubscriptionModal.html'
    };

    var modalOptions = {
      closeButtonText: 'Cancel',
      actionButtonText: 'Delete',
      headerText: 'Delete?',
      bodyText: 'Are you sure you want to delete this subs?'
    };

    ModalService.showModal(modalDefaults, modalOptions).then(function (result) {
      if (result === 'ok') {
        $http({
          method: "DELETE",
          url: "/trainees/me/subscriptions/"+$rootScope.trainee.subscription.id,
          cache: $templateCache}).
        then(function(response) {
          $scope.status = response.status
          $scope.data = response.data
          $scope.retrieve($rootScope.trainee.subscription.id)
          AlertFactory.addAlert(response.data.message, "success")
        }, function(response) {
          $scope.data = response.data
          $scope.status = response.status
          AlertFactory.addAlert(response.data.message, "danger")
        });
      }
    });

  };

}]);




