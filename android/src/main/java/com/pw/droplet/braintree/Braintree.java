package com.pw.droplet.braintree;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.DataCollectorRequest;
import com.braintreepayments.api.GooglePayCardNonce;
import com.braintreepayments.api.GooglePayClient;
import com.braintreepayments.api.GooglePayListener;
import com.braintreepayments.api.GooglePayRequest;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.UserCanceledException;
import com.braintreepayments.api.VenmoAccountNonce;
import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoPaymentMethodUsage;
import com.braintreepayments.api.VenmoRequest;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;


public class Braintree extends ReactContextBaseJavaModule
  implements ActivityEventListener, LifecycleEventListener {

  private final Context mContext;
  private FragmentActivity mCurrentActivity;
  private DataCollector mDataCollector;
  private Promise mPromise;
  private String mDeviceData;
  private String mToken;
  private BraintreeClient mBraintreeClient;
  private PayPalClient mPayPalClient;
  private GooglePayClient mGooglePayClient;
  private VenmoClient mVenmoClient;

  @Override
  public String getName() {
    return "Braintree";
  }

  public Braintree(ReactApplicationContext reactContext) {
    super(reactContext);

    mContext = reactContext;

    reactContext.addLifecycleEventListener(this);
    reactContext.addActivityEventListener(this);
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
    switch (requestCode) {
      case BraintreeRequestCodes.GOOGLE_PAY:
        if (mGooglePayClient != null) {
          mGooglePayClient.onActivityResult(
            resultCode,
            intent,
            this::handleGooglePayResult
          );
        }
        break;
      case BraintreeRequestCodes.VENMO:
        if (mVenmoClient != null) {
          mVenmoClient.onActivityResult(
            mContext,
            resultCode,
            intent,
            this::handleVenmoResult
          );
        }
        break;
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    if (mCurrentActivity != null) {
      mCurrentActivity.setIntent(intent);
    }
  }

  @Override
  public void onHostResume() {
    if (mBraintreeClient != null && mCurrentActivity != null) {
      BrowserSwitchResult browserSwitchResult =
        mBraintreeClient.deliverBrowserSwitchResult(mCurrentActivity);
      if (browserSwitchResult != null) {
        switch (browserSwitchResult.getRequestCode()) {
          case BraintreeRequestCodes.PAYPAL:
            if (mPayPalClient != null) {
              mPayPalClient.onBrowserSwitchResult(
                browserSwitchResult,
                this::handlePayPalResult
              );
            }
            break;
          case BraintreeRequestCodes.VENMO:
            if (mVenmoClient != null) {
              mVenmoClient.onBrowserSwitchResult(
                browserSwitchResult,
                this::handleVenmoResult
              );
            }
            break;
        }
      }
    }
  }

  @ReactMethod
  public void setup(final String token) {
    if (mBraintreeClient == null || !token.equals(mToken)) {
      mCurrentActivity = (FragmentActivity) getCurrentActivity();
      mBraintreeClient = new BraintreeClient(mContext, token);

      mDataCollector = new DataCollector(mBraintreeClient);
      mToken = token;
    }
  }

  @ReactMethod
  public void paypalRequest(
    final ReadableMap parameters,
    final Promise promise
  ) {
    mPromise = promise;

    String description = parameters.hasKey("description") ?
      parameters.getString("description") :
      "FLUZ APP";
    String localeCode = parameters.hasKey("localeCode") ?
      parameters.getString("localeCode") :
      "US";

    if (mCurrentActivity != null) {
      mPayPalClient = new PayPalClient(mBraintreeClient);
      PayPalVaultRequest request = new PayPalVaultRequest(false);
      request.setLocaleCode(localeCode);
      request.setBillingAgreementDescription(description);

      mPayPalClient.tokenizePayPalAccount(mCurrentActivity, request);
    }

  }

  private void handlePayPalResult(
    @Nullable PayPalAccountNonce payPalAccountNonce,
    @Nullable Exception error
  ) {
    if (error != null) {
      System.out.println("Error making Paypal result: " + error);
      return;
    }
    if (payPalAccountNonce != null) {
      sendPaymentMethodNonceResult(payPalAccountNonce.getString());
    }
  }

  @ReactMethod
  public void showGooglePayViewController(final ReadableMap parameters) {
    try {
      String currency = "USD";
      if (mCurrentActivity != null) {
        mGooglePayClient = new GooglePayClient((FragmentActivity) mContext, mBraintreeClient);
        mGooglePayClient.setListener((GooglePayListener) mContext);

        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
          .setCurrencyCode(currency)
          .setTotalPrice(parameters.getString("totalPrice"))
          .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
          .build());
        googlePayRequest.setBillingAddressRequired(parameters.getBoolean("requireAddress"));

        mGooglePayClient.requestPayment(
          mCurrentActivity,
          googlePayRequest
        );
      }

    } catch(Exception error) {
      System.out.println("Error making google pay request: " + error);
    }
  }

  private void handleGooglePayResult(PaymentMethodNonce nonce, Exception error) {
    if (error != null) {
      System.out.println("Error making google pay request: " + error);
      return;
    }
    if (nonce != null) {
      if (mPromise != null) {
        GooglePayCardNonce paymentMethodNonce = (GooglePayCardNonce) nonce;
        WritableMap result = Arguments.createMap();
        result.putString("nonce", paymentMethodNonce.getString());

        WritableMap billingContact = Arguments.createMap();

        billingContact.putString(
          "streetAddress",
          ((GooglePayCardNonce) paymentMethodNonce)
            .getBillingAddress()
            .getStreetAddress());
        billingContact.putString(
          "city", ((GooglePayCardNonce) paymentMethodNonce)
            .getBillingAddress()
            .getLocality());
        billingContact.putString(
          "state", ((GooglePayCardNonce) paymentMethodNonce)
            .getBillingAddress()
            .getRegion());
        billingContact.putString(
          "country", ((GooglePayCardNonce) paymentMethodNonce)
            .getBillingAddress()
            .getCountryCodeAlpha2());
        billingContact.putString(
          "postalCode", ((GooglePayCardNonce) paymentMethodNonce)
            .getBillingAddress()
            .getPostalCode());
        billingContact.putString(
          "countryCode", ((GooglePayCardNonce) paymentMethodNonce)
            .getBillingAddress()
            .getCountryCodeAlpha2());
        result.putMap("billingContact", billingContact);
        mPromise.resolve(result);
      }
    }
  }

  @ReactMethod
  public void venmoRequest(final Promise promise) {
    mPromise = promise;

    if (mCurrentActivity != null) {
      mVenmoClient = new VenmoClient((FragmentActivity) mContext, mBraintreeClient);
      VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
      request.setShouldVault(true);
      request.setFallbackToWeb(true);
      mVenmoClient.tokenizeVenmoAccount(
        mCurrentActivity,
        request
      );
    }
  }

  private void handleVenmoResult(VenmoAccountNonce nonce, Exception error) {
    if (error != null) {
      System.out.println("Error making Venmo result: " + error);
      return;
    }
    if (nonce != null) {
      sendPaymentMethodNonceResult(nonce.getString());
    }
  }

  @ReactMethod
  public void getDeviceData(final Promise promise) {
    DataCollectorRequest request = new DataCollectorRequest(false);
    mDataCollector.collectDeviceData(
      mContext,
      request,
      (result, e) -> promise.resolve(result)
    );
  }



  private void sendPaymentMethodNonceResult(String nonce) {
    if (mPromise != null) {
      mPromise.resolve(nonce);
    }
  }

  @Override
  public void onHostPause() {
  }

  @Override
  public void onHostDestroy() {
  }
}
