import angular from "angular";
import { attach } from "../../../utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <div class="pv__header">
          <svg class="pv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="pv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="pv__content">
          <div class="pv__content__detail" ng-if="self.content.get('title')">
            <div class="pv__content__detail__label">
              Title
            </div>
            <div class="pv__content__detail__value">
              {{self.content.get('title')}}
            </div>
          </div>
          <div class="pv__content__detail" ng-if="self.content.get('name')">
            <div class="pv__content__detail__label">
              Name
            </div>
            <div class="pv__content__detail__value">
              {{self.content.get('name')}}
            </div>
          </div>
          <div class="pv__content__detail" ng-if="self.content.get('position')">
            <div class="pv__content__detail__label">
              Position
            </div>
            <div class="pv__content__detail__value">
              {{self.content.get('position')}}
            </div>
          </div>
          <div class="pv__content__detail" ng-if="self.content.get('company')">
            <div class="pv__content__detail__label">
              Company
            </div>
            <div class="pv__content__detail__value">
              {{self.content.get('company')}}
            </div>
          </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      //TODO: debug; deleteme
      window.person4dbg = this;

      this.$scope.$watch("self.content", (newContent, prevContent) =>
        this.updatedContent(newContent, prevContent)
      );
      this.$scope.$watch("self.details", (newDetails, prevDetails) =>
        this.updatedDetails(newDetails, prevDetails)
      );
    }

    updatedDetails(newDetails, prevDetails) {
      if (newDetails && newDetails != prevDetails) {
        this.details = newDetails;
      }
    }
    updatedContent(newContent, prevContent) {
      if (newContent && newContent != prevContent) {
        this.content = newContent;
      }
    }
  }

  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      content: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.personViewerModule", [])
  .directive("wonPersonViewer", genComponentConf).name;
