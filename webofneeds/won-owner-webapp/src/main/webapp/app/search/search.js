/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

angular.module('won.owner').controller('SearchCtrl', function ($scope, $location,$log,$routeParams, mapService, applicationStateService, applicationControlService) {

    $scope.results = applicationStateService.getSearchResults();
    $scope.columnNum = 2;
    $scope.$on(won.EVENT.WON_SEARCH_RECEIVED,function(ngEvent, event){
        event.data = linkedDataService.getNeed(event.matchUrl());
        $scope.results.push(event);
    })
    // TODO LOGIC
    $scope.relatedTags = ['Sony', 'Tv', 'Samsung', 'LCD'];
    $scope.search = {};
    $scope.search.title = $routeParams.term;
    $scope.search.type = $routeParams.type;

    //TODO LOGIC
    $scope.searching = {type:'others offer', title:'Frilly pink cat unicorn'};

    $scope.createNewPost = function () {
        //TODO put title from search
        $location.url('/create-need/1//' + $scope.searching.title);
    }
});
app.directive(('searchResult'), function searchResultFct(applicationControlService){
    var dtv = {
        restrict: 'E',
        scope : {
            results : '=',
            columnNum : '@'
        },
        templateUrl: "app/search/search-result.html",
        link: function(scope, elem, attr){
            scope.counter = 0;
            scope.preparedResults = [];
            var prepareResults = function(){
                var rowCount = 0;
                for(var i = 0;i<scope.results.length;i++){
                    if(i%scope.columnNum==0){
                        rowCount = rowCount+1;
                        var row = [];
                        row.push(scope.results[i]);
                        scope.preparedResults.push(row);
                    }else{
                        scope.preparedResults[scope.preparedResults.length-1].push(scope.results[i]);
                    }
                }

            }
            prepareResults();
        },
        controller: function($scope, applicationControlService){

            $scope.getPostType = function(result){
                return result[won.WON.searchResultPreview][won.WON.hasBasicNeedType]['@id'];

            }
        }
    }
    return dtv;
})

