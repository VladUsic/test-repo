package com.pw.droplet.braintree;

import java.util.Map;
import java.util.HashMap;

import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupListener;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.google.gson.Gson;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.ThreeDSecure;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.models.CardNonce;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReadableMap;

public class Braintree extends ReactContextBaseJavaModule {
  private static final String USER_CANCELLATION = "USER_CANCELLATION";
  private static final String AUTHENTICATION_UNSUCCESSFUL = "AUTHENTICATION_UNSUCCESSFUL";
  private static final String EXTRA_THREE_D_SECURE_LOOKUP = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP";
  private String token;

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
    try {
      this.successCallback = successCallback;
      this.errorCallback = errorCallback;

      mBraintreeFragment = BraintreeFragment.newInstance((AppCompatActivity)getCurrentActivity(), token);
      mBraintreeFragment.addListener(new BraintreeCancelListener() {
        @Override
        public void onCancel(int requestCode) {
          nonceErrorCallback(USER_CANCELLATION);
        }
      });

      mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
        @Override
        public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
          if (paymentMethodNonce instanceof CardNonce) {
            nonceCallback(paymentMethodNonce.getNonce());
          } else {
            nonceErrorCallback(AUTHENTICATION_UNSUCCESSFUL);
          }
        }
      });

      this.mBraintreeFragment.addListener(new BraintreeErrorListener() {
        @Override
        public void onError(Exception error) {
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
          } else {
            nonceErrorCallback(AUTHENTICATION_UNSUCCESSFUL);
          }
        }
      });
      this.setToken(token);
      successCallback.invoke(this.getToken());
    } catch (InvalidArgumentException e) {
      errorCallback.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void getCardNonce(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    CardBuilder cardBuilder = new CardBuilder()
      .validate(true);

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

    if (parameters.hasKey("firstName"))
      cardBuilder.firstName(parameters.getString("firstName"));

    if (parameters.hasKey("lastName"))
      cardBuilder.lastName(parameters.getString("lastName"));

    if (parameters.hasKey("company"))
      cardBuilder.company(parameters.getString("company"));

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

    Card.tokenize(this.mBraintreeFragment, cardBuilder);
  }

  @ReactMethod
  public void getCardNonceWithThreeDSecure(final ReadableMap parameters, final float orderTotal, final ReadableMap options, final Callback successCallback, final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    this.threeDSecureOptions = options.getMap("threeDSecure");

    CardBuilder cardBuilder = new CardBuilder()
      .validate(true);

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

    if (parameters.hasKey("firstName"))
      cardBuilder.firstName(parameters.getString("firstName"));

    if (parameters.hasKey("lastName"))
      cardBuilder.lastName(parameters.getString("lastName"));

    if (parameters.hasKey("company"))
      cardBuilder.company(parameters.getString("company"));

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

    ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();

    threeDSecureRequest.challengeRequested(true);
    threeDSecureRequest.amount(String.valueOf(orderTotal));
    threeDSecureRequest.versionRequested(ThreeDSecureRequest.VERSION_2);

    ThreeDSecure.performVerification(mBraintreeFragment, cardBuilder, threeDSecureRequest);
  }

  @ReactMethod
  public void getNonceWithThreeDSecure(final ReadableMap parameters, final Callback successCallback, final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    if (!parameters.hasKey("nonce")) {
      this.errorCallback.invoke("Parameter `nonce` is required");
    } else if (!parameters.hasKey("amount")) {
      this.errorCallback.invoke("Parameter `amount` is required");
    } else {
      ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest()
              .nonce(parameters.getString("nonce"))
              .amount(parameters.getString("amount"))
              .versionRequested(ThreeDSecureRequest.VERSION_2);

      ThreeDSecure.performVerification(mBraintreeFragment, threeDSecureRequest, new ThreeDSecureLookupListener() {
        @Override
        public void onLookupComplete(ThreeDSecureRequest request, ThreeDSecureLookup lookup) {
          ThreeDSecure.continuePerformVerification(mBraintreeFragment, request, lookup);
        }
      });
    }
  }

  public void nonceCallback(String nonce) {
    this.successCallback.invoke(nonce);
  }

  public void nonceErrorCallback(String error) {
    this.errorCallback.invoke(error);
  }

}
