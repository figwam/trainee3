'use strict';

/*global app: false */

/**
 * The clazz controller.
 *
 */
app.controller('ClazzMeCtrl', ['$state', '$scope', '$rootScope', '$http', '$templateCache', 'AlertFactory', function($state, $scope, $rootScope, $http, $templateCache, AlertFactory) {

  $scope.clazzes = {};
  $scope.totalClazzes = 0;
  $scope.clazzesPerPage = 10;
  if ($state.current.name == 'me.clazzes') getResultsPage(1)
  $scope.pagination = {
    current: 1
  };

  $scope.myTotalClazzes = 0;
  $scope.myClazzesPerPage = 10;
  $scope.myClazzes = {};
  if ($state.current.name == 'me.myclazzes') getMyClazzesResultsPage(1)
  $scope.myClazzesPagination = {
    myClazzesCurrent: 1
  };

  $scope.myTotalClazzesHistory = 0;
  $scope.myClazzesHistoryPerPage = 10;
  $scope.myClazzesHistory = {};
  if ($state.current.name == 'me.myclazzes') getMyClazzesHistoryResultsPage(1)
  $scope.myClazzesHistoryPagination = {
    myClazzesHistory: 1
  };

  $scope.pageChanged = function(newPage) {
    getResultsPage(newPage);
  };


  function getResultsPage(pageNumber) {
    //play start paging from 0 --> (pageNumber-1)
    $http.get('/clazzes/trainees/me?p='+(pageNumber-1)+'&s=1&f='+($rootScope.clazzesSearchString == null ? '':$rootScope.clazzesSearchString))
      .then(function(result) {
        $scope.clazzes = result.data
        $scope.totalClazzes = result.data.total
      });
  }

  $scope.myClazzesPageChanged = function(newPage) {
    getMyClazzesResultsPage(newPage);
  };

  function getMyClazzesResultsPage(pageNumber) {
    //play start paging from 0 --> (pageNumber-1)
    $http.get('/trainees/me/clazzes?p='+(pageNumber-1)+'&s=1&f=&sf='+new Date().getTime())
      .then(function(result) {
        $scope.myClazzes = result.data
        $scope.myTotalClazzes = result.data.total
      });

  }

  $scope.myClazzesHistoryPageChanged = function(newPage) {
    getMyClazzesHistoryResultsPage(newPage);
  };

  function getMyClazzesHistoryResultsPage(pageNumber) {
    //play start paging from 0 --> (pageNumber-1)
    $http.get('/trainees/me/clazzes?p='+(pageNumber-1)+'&s=1&f=&ea='+new Date().getTime())
      .then(function(result) {
        $scope.myClazzesHistory = result.data
        $scope.myTotalClazzesHistory = result.data.total
      });

  }

  $scope.book = function(idClazz) {
    var body={"idClazz":idClazz};
    $http({
      method: "POST",
      url: "/trainees/me/registrations",
      data: body,
      headers: { 'Content-Type': 'application/json; charset=UTF-8'},
      cache: $templateCache}).
      then(function(response) {
        $scope.status = response.status;
        $scope.data = response.data;
        getResultsPage($scope.pagination.current)
        getMyClazzesResultsPage($scope.myClazzesPagination.myClazzesCurrent)
        getMyClazzesHistoryResultsPage($scope.myClazzesHistoryPagination.myClazzesHistory)
        AlertFactory.addAlert("Danke für Ihre Registrierung.","success")
      }, function(response) {
        $scope.data = response.data || "Request failed";
        $scope.status = response.status;
      });
  };

  $scope.bookDelete = function(idRegistration) {
    $http({
      method: "DELETE",
      url: "/trainees/me/registrations/"+idRegistration,
      cache: $templateCache}).
      then(function(response) {
        $scope.status = response.status;
        $scope.data = response.data;
        getResultsPage($scope.pagination.current)
        getMyClazzesResultsPage($scope.myClazzesPagination.myClazzesCurrent)
        getMyClazzesHistoryResultsPage($scope.myClazzesHistoryPagination.myClazzesHistory)
        AlertFactory.addAlert("Sie wurden vom Kurs abgemeldet.","danger")
      }, function(response) {
        $scope.data = response.data || "Request failed";
        $scope.status = response.status;
      });
  };


  $scope.submitSearch = function(idTrainee){
    $rootScope.clazzesSearchString = $scope.searchString
    getResultsPage(1)
    if ($state.current.name != 'me.clazzes') $state.go('me.clazzes')
  };
}]);



app.filter('searchFor', function(){
  return function(arr, searchString){
    if(!searchString){
      return arr;
    }
    var result = [];
    searchString = searchString.toLowerCase();
    angular.forEach(arr, function(item){
      if(item.searchMeta.toLowerCase().indexOf(searchString) !== -1){
        result.push(item);
      }
    });
    return result;
  };
});
