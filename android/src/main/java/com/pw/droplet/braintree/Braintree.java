package com.pw.droplet.braintree;

import java.util.Map;
import java.util.HashMap;
import android.util.Log;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import android.content.Intent;
import android.content.Context;
import com.braintreepayments.api.ThreeDSecure;

import com.braintreepayments.api.models.PaymentMethodNonce;

import com.braintreepayments.api.BraintreeFragment;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;

import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.GooglePayment;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class Braintree extends ReactContextBaseJavaModule   {
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
    }

    @Override
    public String getName() {
        return "Braintree";
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }


    @ReactMethod
    public void setup(final String token, final Callback successCallback, final Callback errorCallback) {
        try{
            this.mBraintreeFragment = BraintreeFragment.newInstance((AppCompatActivity) getCurrentActivity(),  token);
        }catch(InvalidArgumentException e){
            Log.e("PAYMENT_REQUEST", "I got an error", e);
            errorCallback.invoke(e.getMessage());
        }
        if(this.mBraintreeFragment instanceof BraintreeFragment){
            this.mBraintreeFragment.addListener(new BraintreeCancelListener() {
                @Override
                public void onCancel(int requestCode) {
                    nonceErrorCallback("USER_CANCELLATION");
                }
            });
            this.mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
                @Override
                public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {

                    if (paymentMethodNonce instanceof CardNonce) {
                        CardNonce cardNonce = (CardNonce) paymentMethodNonce;
                        if (!cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible()) {
                            nonceErrorCallback("3DSECURE_NOT_ABLE_TO_SHIFT_LIABILITY");
                        } else if (!cardNonce.getThreeDSecureInfo().isLiabilityShifted()) {
                            nonceErrorCallback("3DSECURE_LIABILITY_NOT_SHIFTED");
                        } else {
                            nonceCallback(paymentMethodNonce.getNonce());
                        }
                    }
                    else {
                        nonceCallback(paymentMethodNonce.getNonce());
                    }
                }
            });

            this.mBraintreeFragment.addListener(new BraintreeErrorListener() {
                @Override
                public void onError(Exception error) {
                    Log.e("PAYMENT_REQUEST", "I got an error", error);
                    if (error instanceof ErrorWithResponse) {
                        ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
                        BraintreeError cardErrors = errorWithResponse.errorFor("creditCard");
                        if (cardErrors != null) {
                            Gson gson = new Gson();
                            final Map<String, String> errors = new HashMap<>();
                            BraintreeError numberError = cardErrors.errorFor("number");
                            BraintreeError cvvError = cardErrors.errorFor("cvv");
                            BraintreeError expirationDateError = cardErrors.errorFor("expirationDate");
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

    @ReactMethod
    public void getCardNonce(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback)  {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;

        CardBuilder cardBuilder = new CardBuilder()
                .validate(false);

        if (parameters.hasKey("number"))
            cardBuilder.cardNumber(parameters.getString("number"));

        if (parameters.hasKey("cvv"))
            cardBuilder.cvv(parameters.getString("cvv"));

        // In order to keep compatibility with iOS implementation, do not accept expirationMonth and exporationYear,
        // accept rather expirationDate (which is combination of expirationMonth/expirationYear)
        if (parameters.hasKey("expirationDate"))
            cardBuilder.expirationDate(parameters.getString("expirationDate"));

        if (parameters.hasKey("cardholderName"))
            cardBuilder.cardholderName(parameters.getString("cardholderName"));

        if (parameters.hasKey("firstname"))
            cardBuilder.firstName(parameters.getString("firstname"));

        if (parameters.hasKey("lastname"))
            cardBuilder.lastName(parameters.getString("lastname"));

        if (parameters.hasKey("countryCode"))
            cardBuilder.countryCode(parameters.getString("countryCode"));

        if (parameters.hasKey("countryCodeAlpha2"))
            cardBuilder.countryCode(parameters.getString("countryCodeAlpha2"));

        if (parameters.hasKey("locality"))
            cardBuilder.locality(parameters.getString("locality"));

        if (parameters.hasKey("postalCode"))
            cardBuilder.postalCode(parameters.getString("postalCode"));

        if (parameters.hasKey("region"))
            cardBuilder.region(parameters.getString("region"));

        if (parameters.hasKey("streetAddress"))
            cardBuilder.streetAddress(parameters.getString("streetAddress"));

        if (parameters.hasKey("extendedAddress"))
            cardBuilder.extendedAddress(parameters.getString("extendedAddress"));

        ThreeDSecure.performVerification(this.mBraintreeFragment, cardBuilder, parameters.getString("amount"));

    }


    public void nonceCallback(String nonce) {
        this.successCallback.invoke(nonce);
    }

    public void nonceErrorCallback(String error) {
        this.errorCallback.invoke(error);
    }

    @ReactMethod
    public void paypalRequest(final Callback successCallback, final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
        PayPalRequest request = new PayPalRequest()
                .currencyCode("USD")
                .intent(PayPalRequest.INTENT_AUTHORIZE);
        PayPal.requestBillingAgreement(this.mBraintreeFragment, request);
    }

    @ReactMethod
    public void venmoRequest(final Callback successCallback, final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
        Venmo.authorizeAccount(this.mBraintreeFragment, false);
    }

    @ReactMethod
    public void showGooglePayViewController(final ReadableMap options, final Callback successCallback, final Callback errorCallback) {
        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice(options.getString("totalPrice"))
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode(options.getString("currencyCode"))
                        .build())
                .billingAddressRequired(options.getBoolean("requireAddress"))
                .googleMerchantId(options.getString("googleMerchantId"));
        GooglePayment.requestPayment(mBraintreeFragment, googlePaymentRequest);
    }

    @ReactMethod
    public void getDeviceData(final ReadableMap options, final Callback successCallback) {
        this.deviceDataCallback = successCallback;
        this.collectDeviceData = true;
        String env = options.getString("environment");


        if (options.hasKey("merchantId")) {
            String merchantId = options.getString("merchantId");
            DataCollector.collectDeviceData(this.mBraintreeFragment, merchantId, new BraintreeResponseListener<String>() {
                @Override
                public void onResponse(String deviceData) {
                    successCallback.invoke(null, deviceData);
                }
            });

        } else {
            String type = options.getString("dataCollector");

            Log.d("Data Collector type", type);

            if (type.equals("paypal")) {
                DataCollector.collectPayPalDeviceData(this.mBraintreeFragment, new BraintreeResponseListener<String>() {
                    @Override
                    public void onResponse(String deviceData) {
                        Log.d("Device Data Response", deviceData);
                        successCallback.invoke(null, deviceData);
                    }
                });
            }
        }


    }

    public void onNewIntent(Intent intent){}
}