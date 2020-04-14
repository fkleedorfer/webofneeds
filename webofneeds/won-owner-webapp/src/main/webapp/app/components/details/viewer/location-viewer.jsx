import React from "react";

import "~/style/_location-viewer.scss";
import ico_filter_map from "~/images/won-icons/ico-filter_map.svg";
import ico16_arrow_up from "~/images/won-icons/ico16_arrow_up.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { get } from "../../../utils.js";
import WonAtomMap from "../../atom-map.jsx";

import PropTypes from "prop-types";

export default class WonLocationViewer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      locationExpanded: false,
    };
  }

  toggleLocation() {
    this.setState({ locationExpanded: !this.state.locationExpanded });
  }

  render() {
    const icon = this.props.detail.icon && (
      <svg className="lv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="lv__header__label">{this.props.detail.label}</span>
    );

    const address = get(this.props.content, "address");
    const addressElement = address ? (
      <div
        className="lv__content__text clickable"
        onClick={this.toggleLocation.bind(this)}
      >
        {address}
        <svg className="lv__content__text__carret">
          <use xlinkHref={ico_filter_map} href={ico_filter_map} />
        </svg>
        <svg className="lv__content__text__carret">
          {this.state.locationExpanded ? (
            <use xlinkHref={ico16_arrow_up} href={ico16_arrow_up} />
          ) : (
            <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
          )}
        </svg>
      </div>
    ) : (
      undefined
    );

    const map =
      this.props.content && this.state.locationExpanded ? (
        <WonAtomMap locations={[this.props.content]} />
      ) : (
        undefined
      );

    return (
      <won-location-viewer class={this.props.className}>
        <div className="lv__header">
          {icon}
          {label}
        </div>
        <div className="lv__content">
          {addressElement}
          {map}
        </div>
      </won-location-viewer>
    );
  }
}
WonLocationViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
