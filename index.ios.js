// @flow

'use strict';

import { NativeModules, processColor } from 'react-native';
import { mapParameters } from './utils';

import type { CardParameters } from './types';

const RCTBraintree = NativeModules.Braintree;

var Braintree = {
  setupWithURLScheme(token, urlscheme) {
    return new Promise(function(resolve, reject) {
      RCTBraintree.setupWithURLScheme(token, urlscheme, function(success) {
        success == true ? resolve(true) : reject('Invalid Token');
      });
    });
  },

  setup(token) {
    return new Promise(function(resolve, reject) {
      RCTBraintree.setup(token, function(success) {
        success == true ? resolve(true) : reject('Invalid Token');
      });
    });
  },

  getNonceWithThreeDSecure(parameters = {}) {
    return new Promise(function(resolve, reject) {
      RCTBraintree.getNonceWithThreeDSecure(parameters, function(
        err,
        nonce
      ) {
        let jsonErr = null;

        try {
          jsonErr = JSON.parse(err);
        } catch (e) {
          //
        }

        nonce !== null
          ? resolve(nonce)
          : reject(
              jsonErr
                ? jsonErr['BTCustomerInputBraintreeValidationErrorsKey'] ||
                  jsonErr
                : err
            );
      });
    });
  },

  showPaymentViewController(config = {}) {
    var options = {
      tintColor: processColor(config.tintColor),
      bgColor: processColor(config.bgColor),
      barBgColor: processColor(config.barBgColor),
      barTintColor: processColor(config.barTintColor),
      callToActionText: config.callToActionText,
      title: config.title,
      description: config.description,
      amount: config.amount,
      threeDSecure: config.threeDSecure,
    };
    return new Promise(function(resolve, reject) {
      RCTBraintree.showPaymentViewController(options, function(err, nonce) {
        nonce != null ? resolve(nonce) : reject(err);
      });
    });
  },

  showPayPalViewController() {
    return new Promise(function(resolve, reject) {
      RCTBraintree.showPayPalViewController(function(err, nonce) {
        nonce != null ? resolve(nonce) : reject(err);
      });
    });
  },

  getCardNonceWithThreeDSecure(params) {
    const cardDetails = mapParameters(params);
    const parameters = {
      cardDetails,
      amount: params.amount
    }

    return new Promise(function(resolve, reject) {
      RCTBraintree.getCardNonceWithThreeDSecure(parameters, function(err, nonce) {
        try {
          const jsonErr = JSON.parse(err);

          if (jsonErr)
            return reject(jsonErr["BTCustomerInputBraintreeValidationErrorsKey"] || jsonErr);
        } catch (e) {
          //
        }

        if (err)
          return reject(err);

        return resolve(nonce);
      });
    });
  },

  getCardNonce(parameters: CardParameters = {}) {
    return new Promise(function(resolve, reject) {
      RCTBraintree.getCardNonce(mapParameters(parameters), function(
        err,
        nonce
      ) {
        let jsonErr = null;

        try {
          jsonErr = JSON.parse(err);
        } catch (e) {
          //
        }

        nonce !== null
          ? resolve(nonce)
          : reject(
              jsonErr
                ? jsonErr['BTCustomerInputBraintreeValidationErrorsKey'] ||
                  jsonErr
                : err
            );
      });
    });
  },

  getDeviceData(options = {}) {
    return new Promise(function(resolve, reject) {
      RCTBraintree.getDeviceData(options, function(err, deviceData) {
        deviceData != null ? resolve(deviceData) : reject(err);
      });
    });
  },

  showApplePayViewController(options = {}) {
    return new Promise(function(resolve, reject) {
      if (Platform.OS === 'ios') {
        RCTBraintree.showApplePayViewController(options, function(err, nonce) {
            nonce != null ? resolve(nonce) : reject(err);
          });
      } else {
        reject('showApplePayViewController is only available on ios devices');
      }
    });
  },
};

module.exports = Braintree;
