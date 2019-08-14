import React from "react";

import PropTypes from "prop-types";
import WonAtomShareLink from "./atom-share-link.jsx";

import "~/style/_share-dropdown.scss";

export default class WonShareDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    //TODO: REACT ON CLICK OUTSIDE OF COMPONENT AND CLOSE THE DIALOG (maybe with hooks)
  }

  render() {
    const dropdownElement = this.state.contextMenuOpen && (
      <div className="sdd__sharemenu">
        <div className="sdd__sharemenu__content">
          <div className="topline">
            <svg
              className="sdd__icon__small__sharemenu clickable"
              onClick={() => this.setState({ contextMenuOpen: false })}
            >
              <use xlinkHref="#ico16_share" href="#ico16_share" />
            </svg>
          </div>
          <WonAtomShareLink
            atomUri={this.props.atomUri}
            ngRedux={this.props.ngRedux}
          />
        </div>
      </div>
    );

    return (
      <won-share-dropdown
        class={this.props.className ? this.props.className : undefined}
      >
        <svg
          className="sdd__icon__small clickable"
          onClick={() => this.setState({ contextMenuOpen: true })}
        >
          <use xlinkHref="#ico16_share" href="#ico16_share" />
        </svg>
        {dropdownElement}
      </won-share-dropdown>
    );
  }
}
WonShareDropdown.propTypes = {
  atomUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
  className: PropTypes.string,
};
