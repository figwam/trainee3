'use strict';

/*global app: false */

/**
 * The trainee factory.
 */
app.factory('TraineeFactory', function($http) {
  return {
    get: function() {
      return $http.get('/user');
    }
  };
});
