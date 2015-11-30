'use strict';

/*global app: false */

/**
 * The clazz controller.
 *
 */
app.controller('DashboardCtrl', ['$rootScope', '$scope', '$http', 'ClazzFactory', 'RegistrationFactory', function($rootScope, $scope, $http, ClazzFactory, RegistrationFactory) {

  RegistrationFactory.getRegistrationsCount();
  ClazzFactory.getClazzCount();


}]);
