import React, { useState, useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import WonLabelledHr from "../../components/labelled-hr.jsx";

import "~/style/_signup.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import { actionCreators } from "../../actions/actions";
import { Link } from "react-router-dom";
import WonGenericPage from "~/app/pages/genericPage";

const MINPW_LENGTH = 6;

export default function PageSignUp() {
  const dispatch = useDispatch();

  const accountState = useSelector(generalSelectors.getAccountState);
  const privateId = accountUtils.getPrivateId(accountState);
  const registerError = accountUtils.getRegisterError(accountState);
  const isAnonymous = accountUtils.isAnonymous(accountState);

  const [state, setState] = useState({
    email: "",
    validEmail: false,
    privateId: privateId,
    password: "",
    passwordAgain: "",
    rememberMe: false,
    acceptToS: false,
  });

  useEffect(
    () => {
      setState({
        ...state,
        privateId: privateId,
      });
    },
    [privateId]
  );

  function transfer() {
    dispatch(
      actionCreators.account__transfer({
        email: state.email,
        password: state.password,
        privateId: state.privateId,
        rememberMe: state.rememberMe,
      })
    );
  }

  function register() {
    dispatch(
      actionCreators.account__register({
        email: state.email,
        password: state.password,
        rememberMe: state.rememberMe,
      })
    );
  }

  function isValid() {
    if (!state.validEmail) {
      return false;
    }
    if (state.password.length < MINPW_LENGTH) {
      return false;
    }
    if (state.passwordAgain !== state.password) {
      return false;
    }
    if (!state.acceptToS) {
      return false;
    }

    return true;
  }

  /*function formKeyUp(event) {
    if (registerError) {
      dispatch(actionCreators.view__clearRegisterError());
    }
    if (event.keyCode == 13 && isValid()) {
      if (isAnonymous) {
        transfer();
      } else {
        register();
      }
    }
  }*/

  function changePassword(event) {
    setState({
      ...state,
      password: event.target.value,
    });
  }

  function changePasswordAgain(event) {
    setState({
      ...state,
      passwordAgain: event.target.value,
    });
  }

  function changeRememberMe(event) {
    setState({
      ...state,
      rememberMe: event.target.checked,
    });
  }

  function changeAcceptToS(event) {
    setState({
      ...state,
      acceptToS: event.target.checked,
    });
  }

  function changeEmail(event) {
    setState({
      ...state,
      email: event.target.value,
      validEmail: event.target.validity.valid,
    });
  }

  return (
    <WonGenericPage pageTitle="Sign Up">
      <main className="signup" id="signupSection">
        <div className="signup__content">
          <div className="signup__content__form" name="registerForm">
            <input
              id="registerEmail"
              name="email"
              placeholder="Email address"
              className={registerError ? "ng-invalid" : ""}
              required
              type="email"
              onChange={changeEmail}
              value={state.email}
            />

            {state.email.length > 0 &&
              !state.validEmail && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  <span className="signup__content__form__errormsg__label">
                    Not a valid E-Mail address
                  </span>
                </div>
              )}
            {registerError && (
              <div className="signup__content__form__errormsg">
                <svg className="signup__content__form__errormsg__icon">
                  <use
                    xlinkHref={ico16_indicator_warning}
                    href={ico16_indicator_warning}
                  />
                </svg>
                <span className="signup__content__form__errormsg__label">
                  {registerError}
                </span>
              </div>
            )}

            <input
              name="password"
              placeholder="Password"
              required
              type="password"
              onChange={changePassword}
              value={state.password}
            />

            {state.password.length > 0 &&
              state.password.length < MINPW_LENGTH && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  <span className="signup__content__form__errormsg__label">{`Password too short, must be at least ${MINPW_LENGTH} Characters`}</span>
                </div>
              )}

            <input
              name="password_repeat"
              placeholder="Repeat Password"
              required
              type="password"
              onChange={changePasswordAgain}
              value={state.passwordAgain}
            />

            {state.passwordAgain.length > 0 &&
              state.password !== state.passwordAgain && (
                <div className="signup__content__form__errormsg">
                  <svg className="signup__content__form__errormsg__icon">
                    <use
                      xlinkHref={ico16_indicator_warning}
                      href={ico16_indicator_warning}
                    />
                  </svg>
                  <span className="signup__content__form__errormsg__label">
                    Password is not equal
                  </span>
                </div>
              )}

            <div>
              <input
                id="rememberMe"
                type="checkbox"
                onChange={changeRememberMe}
                value={state.rememberMe}
              />
              <label htmlFor="rememberMe">remember me</label>
            </div>
            <div>
              <input
                id="acceptToS"
                type="checkbox"
                required
                value={state.acceptToS}
                onChange={changeAcceptToS}
              />
              <label htmlFor="acceptToS">
                I accept the{" "}
                <Link
                  className="clickable"
                  to="/about?aboutSection=aboutTermsOfService"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Terms Of Service
                </Link>
              </label>
            </div>
          </div>
          {isAnonymous && (
            <button
              className="won-button--filled secondary"
              disabled={!isValid()}
              onClick={transfer}
            >
              <span>Keep Postings</span>
            </button>
          )}
          {isAnonymous && <WonLabelledHr label="or" className="labelledHr" />}
          <button
            className="won-button--filled secondary"
            disabled={!isValid()}
            onClick={register}
          >
            <span>
              {isAnonymous
                ? "Start from Scratch"
                : "That’s all we need. Let’s go!"}
            </span>
          </button>
        </div>
      </main>
    </WonGenericPage>
  );
}
