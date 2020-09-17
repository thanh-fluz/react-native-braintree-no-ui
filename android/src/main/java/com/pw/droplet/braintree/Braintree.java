package com.pw.droplet.braintree;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.GooglePayment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.ThreeDSecure;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class Braintree extends ReactContextBaseJavaModule {
  private static final int PAYMENT_REQUEST = 1706816330;
  private String token;
  private Callback deviceDataCallback;
  private Boolean collectDeviceData = false;
  private Callback successCallback;
  private Callback errorCallback;

  private Context mActivityContext;
  private BraintreeFragment mBraintreeFragment;

  private ReadableMap threeDSecureOptions;

  public Braintree(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addActivityEventListener(activityEventListener);
  }

  @Override
  public String getName() {
    return "Braintree";
  }

  public String getToken() { return this.token; }

  public void setToken(String token) { this.token = token; }

  @ReactMethod
  public void setup(final String token, final Callback successCallback,
                    final Callback errorCallback) {
    try {
      this.mBraintreeFragment = BraintreeFragment.newInstance(
          (FragmentActivity)getCurrentActivity(), token);
    } catch (InvalidArgumentException e) {
      Log.e("PAYMENT_REQUEST", "I got an error", e);
      errorCallback.invoke(e.getMessage());
    }
    if (this.mBraintreeFragment instanceof BraintreeFragment) {
      this.mBraintreeFragment.addListener(new BraintreeCancelListener() {
        @Override
        public void onCancel(int requestCode) {
          nonceErrorCallback("USER_CANCELLATION");
        }
      });
      this.mBraintreeFragment.addListener(
          new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(
                PaymentMethodNonce paymentMethodNonce) {

              if (paymentMethodNonce instanceof GooglePaymentCardNonce) {

                WritableMap payment = new WritableNativeMap();
                payment.putString("nonce", paymentMethodNonce.getNonce());
                WritableMap billingContact = new WritableNativeMap();

                billingContact.putString(
                    "streetAddress",
                    ((GooglePaymentCardNonce)paymentMethodNonce)
                        .getBillingAddress()
                        .getStreetAddress());
                billingContact.putString(
                    "city", ((GooglePaymentCardNonce)paymentMethodNonce)
                                .getBillingAddress()
                                .getLocality());
                billingContact.putString(
                    "state", ((GooglePaymentCardNonce)paymentMethodNonce)
                                 .getBillingAddress()
                                 .getRegion());
                billingContact.putString(
                    "country", ((GooglePaymentCardNonce)paymentMethodNonce)
                                   .getBillingAddress()
                                   .getCountryCodeAlpha2());
                billingContact.putString(
                    "postalCode", ((GooglePaymentCardNonce)paymentMethodNonce)
                                      .getBillingAddress()
                                      .getPostalCode());
                billingContact.putString(
                    "countryCode", ((GooglePaymentCardNonce)paymentMethodNonce)
                                       .getBillingAddress()
                                       .getCountryCodeAlpha2());
                payment.putMap("billingContact", billingContact);
                objCallback(payment);

              } else {
                nonceCallback(paymentMethodNonce.getNonce());
              }
            }
          });

      this.mBraintreeFragment.addListener(new BraintreeErrorListener() {
        @Override
        public void onError(Exception error) {
          Log.e("PAYMENT_REQUEST", "I got an error", error);
          if (error instanceof ErrorWithResponse) {
            ErrorWithResponse errorWithResponse = (ErrorWithResponse)error;
            BraintreeError cardErrors =
                errorWithResponse.errorFor("creditCard");
            if (cardErrors != null) {
              Gson gson = new Gson();
              final Map<String, String> errors = new HashMap<>();
              BraintreeError numberError = cardErrors.errorFor("number");
              BraintreeError cvvError = cardErrors.errorFor("cvv");
              BraintreeError expirationDateError =
                  cardErrors.errorFor("expirationDate");
              BraintreeError postalCode = cardErrors.errorFor("postalCode");

              if (numberError != null) {
                errors.put("card_number", numberError.getMessage());
              }

              if (cvvError != null) {
                errors.put("cvv", cvvError.getMessage());
              }

              if (expirationDateError != null) {
                errors.put("expiration_date", expirationDateError.getMessage());
              }

              // TODO add more fields
              if (postalCode != null) {
                errors.put("postal_code", postalCode.getMessage());
              }

              nonceErrorCallback(gson.toJson(errors));
            } else {
              nonceErrorCallback(errorWithResponse.getErrorResponse());
            }
          }
        }
      });
      this.setToken(token);
      successCallback.invoke(this.getToken());
    }
  }

  public void nonceCallback(String nonce) {
    this.successCallback.invoke(nonce);
  }

  public void objCallback(WritableMap obj) { this.successCallback.invoke(obj); }

  public void nonceErrorCallback(String error) {
    this.errorCallback.invoke(error);
  }

  private final ActivityEventListener activityEventListener =
      new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode,
                                     int resultCode, Intent data) {
          if (requestCode == PAYMENT_REQUEST) {
            if (data != null) {
              switch (resultCode) {
              case Activity.RESULT_OK:
                GooglePayment.tokenize(mBraintreeFragment,
                                       PaymentData.getFromIntent(data));
                break;
              case Activity.RESULT_CANCELED:
                nonceErrorCallback("USER_CANCELLATION");
                break;
              }
            } else {
              nonceErrorCallback("NO_DATA_AVAILABLE");
            }
          }
        }
      };

  @ReactMethod
  public void paypalRequest(final Callback successCallback,
                            final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    PayPalRequest request = new PayPalRequest().currencyCode("USD").intent(
        PayPalRequest.INTENT_AUTHORIZE);
    PayPal.requestBillingAgreement(this.mBraintreeFragment, request);
  }

  @ReactMethod
  public void venmoRequest(final Callback successCallback,
                           final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    Venmo.authorizeAccount(this.mBraintreeFragment, false);
  }

  @ReactMethod
  public void showGooglePayViewController(final ReadableMap options,
                                          final Callback successCallback,
                                          final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    GooglePaymentRequest googlePaymentRequest =
        new GooglePaymentRequest()
            .transactionInfo(
                TransactionInfo.newBuilder()
                    .setTotalPrice(options.getString("totalPrice"))
                    .setTotalPriceStatus(
                        WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setCurrencyCode(options.getString("currencyCode"))
                    .build())
            .billingAddressRequired(options.getBoolean("requireAddress"));

    if (options.hasKey("googleMerchantId")) {
      googlePaymentRequest.googleMerchantId(
          options.getString("googleMerchantId"));
    }
    GooglePayment.requestPayment(this.mBraintreeFragment, googlePaymentRequest);
  }

  @ReactMethod
  public void getDeviceData(final ReadableMap options,
                            final Callback successCallback,
                            final Callback errorCallback) {
    if (this.mBraintreeFragment instanceof BraintreeFragment) {
      this.deviceDataCallback = successCallback;
      this.collectDeviceData = true;
      String type = options.getString("dataCollector");
      if (type.equals("paypal")) {
        DataCollector.collectPayPalDeviceData(
            this.mBraintreeFragment, new BraintreeResponseListener<String>() {
              @Override
              public void onResponse(String deviceData) {
                if (deviceData != null) {
                  successCallback.invoke(deviceData);
                } else {
                  errorCallback.invoke("BT:: DEVICE_DATA is empty.");
                }
              }
            });
      } else {
        DataCollector.collectDeviceData(
            this.mBraintreeFragment, new BraintreeResponseListener<String>() {
              @Override
              public void onResponse(String deviceData) {
                if (deviceData != null) {
                  successCallback.invoke(deviceData);
                } else {
                  errorCallback.invoke("BT:: DEVICE_DATA is empty.");
                }
              }
            });
      }
    } else {
      errorCallback.invoke("BT:: DATA_COLLECTOR not init.");
    }
  }

  public void onNewIntent(Intent intent) {}
}