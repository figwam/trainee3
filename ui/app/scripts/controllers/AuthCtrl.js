'use strict';


/*global app: false */

/**
 * The navigation controller.
 */
app.controller('AuthCtrl', ['$rootScope', '$scope', '$auth', '$location', '$anchorScroll', 'TraineeFactory', function ($rootScope, $scope, $auth, $location, $anchorScroll, TraineeFactory) {

  /**
   * Initializes the controller.
   */
  $scope.init = function () {
    $scope.code = null;
    $scope.response = null;


    /*
    TraineeFactory.get()
      .success(function (data) {
        $rootScope.trainee = data;
        console.log($rootScope.trainee.extId)
        $location.path("/me");
      })
      .error(function (error) {
        //console.log("1...."+JSON.stringify(error))
        if (!error.success)
        $location.path("/");
      });
  */

    TraineeFactory.get().then(
      function(response) {
        $rootScope.trainee = response.data;
        $location.path("/me")
      },
      function(response) {
        if(response.status == 401) {
          // do locally logout, cause some login inconsistency (Can happen if server is restarted!)
          // do logout on client side and redirect to root
          $auth.logout().then(function() {$location.path("/")});
        } else {
          // Also in other error cases do client side logout
          // TODO: improve! Distinguish between error cases
          $auth.logout().then(function() {$location.path("/")});
        }
    });
  };

  /**
   * Indicates if the trainee is authenticated or not.
   *
   * @returns {boolean} True if the trainee is authenticated, false otherwise.
   */
  $scope.isAuthenticated = function () {
    return $auth.isAuthenticated();
  };

}]);
