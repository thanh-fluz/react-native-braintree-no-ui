# react-native-braintree-xplat

Follow both setup guide with native sdk first

https://developers.braintreepayments.com/guides/client-sdk/setup/android/v3

https://developers.braintreepayments.com/guides/client-sdk/setup/ios/v4

## Usage

### Setup

```js
BTClient.setup(token);
```


**PayPal, Venmo**

```js
BTClient.showPayPalViewController()
  .then(function(nonce) {
    //payment succeeded, pass nonce to server
  })
  .catch(function(err) {
    //error handling
  });
```

---

**Apple Pay**

```
BTClient.showApplePayViewController({
    merchantIdentifier: 'your.merchant.id',
    paymentSummaryItems: [
        {label: 'Subtotals', amount: subtotals},
        {label: 'Shipping', amount: shipping},
        {label: 'Totals', amount: totals},
    ]
    })
    .then((response) => {
        console.log(response);
        if (response.nonce) {
            // Do something with the nonce
        }
    });
```

#### WhiteList

If your app is built using iOS 9 as its Base SDK, then you must add URLs to a whitelist in your app's info.plist

```js
   <key>LSApplicationQueriesSchemes</key>
   <array>
     <string>com.paypal.ppclient.touch.v1</string>
     <string>com.paypal.ppclient.touch.v2</string>
     <string>com.venmo.touch.v2</string>
   </array>
```

#### For both platforms:

```js
if (Platform.OS === "ios") {
  BTClient.setupWithURLScheme(token, "your.bundle.id.payments");
} else {
  BTClient.setup(token);
}
```

### TODO

1. remove card methods and 3D-methods 
2. remove dropin-ui related reference

## Credits

Big thanks to [@alanhhwong](https://github.com/alanhhwong) and [@surialabs](https://github.com/surialabs) for the original ios & android modules.

Works made possible by [@kraffslol](https://github.com/kraffslol/react-native-braintree-xplat)
