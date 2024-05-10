"use strict";

import { NativeModules, processColor, Platform } from "react-native";
import { mapParameters } from "./utils";

const Braintree = NativeModules.Braintree;

module.exports = {
  setupWithURLScheme(token, urlscheme) {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "ios") {
        Braintree.setupWithURLScheme(token, urlscheme, function (success) {
          success == true ? resolve(true) : reject("Invalid Token");
        });
      } else {
        reject("setupWithURLScheme is only available on ios devices");
      }
    });
  },
  setup(token) {
    return Braintree.setup(token);
  },
  getDeviceData(options = {}) {
    return Platform.OS === "ios"
      ? new Promise(function (resolve, reject) {
          Braintree.getDeviceData(options, function (err, deviceData) {
            deviceData != null ? resolve(deviceData) : reject(err);
          });
        })
      : Braintree.getDeviceData();
  },
  showPayPalViewController(options = {}) {
    return Platform.OS === "ios"
      ? new Promise(function (resolve, reject) {
          Braintree.showPayPalViewController(function (err, nonce) {
            nonce != null ? resolve(nonce) : reject(err);
          });
        })
      : Braintree.paypalRequest(options);
  },
  showApplePayViewController(options = {}) {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "ios") {
        Braintree.showApplePayViewController(options, function (err, nonce) {
          nonce != null ? resolve(nonce) : reject(err);
        });
      } else {
        reject("showApplePayViewController is only available on ios devices");
      }
    });
  },
  showVenmoViewController() {
    return Platform.OS === "ios"
      ? new Promise(function (resolve, reject) {
          Braintree.showVenmoViewController(function (err, nonce) {
            nonce != null ? resolve(nonce) : reject(err);
          });
        })
      : Braintree.venmoRequest();
  },
  showGooglePayViewController(options = {}) {
    return Platform.OS === "ios"
      ? new Promise(function (resolve, reject) {
          reject(
            "showGooglePayViewController is only available on ios devices"
          );
        })
      : Braintree.showGooglePayViewController(options);
  },
};
