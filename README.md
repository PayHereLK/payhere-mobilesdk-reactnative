# React Native SDK

Integrating PayHere with your React Native App is super easy with our PayHere React Native SDK. You just have to include the package in your project dependencies, call its methods to initiate a payment and fetch the payment status just after the payment. The beauty is, this SDK allows you to accept a payment within your app, without redirecting your app user to the web browser.

## Usage

### 1. Include PayHere Mobile SDK in your App

Open your React Native project's `package.json` file and add the PayHere React Native SDK dependency.
```json
{
  "dependencies": {
    "@payhere/payhere-mobilesdk-reactnative": "4.0.10"
  }
}
```

Then run the following commands in your React Native project directory.

```
npm install
react-native link @payhere/payhere-mobilesdk-reactnative
```

### 2. Android Pre-requisites

##### a. Add the PayHere Android SDK's Maven repository

Open up the (outermost) `build.gradle` file in your Android project and add the repository. 

```groovy
allprojects {
    repositories {
        mavenLocal()
        maven {
            url  'https://repo.repsy.io/mvn/payhere/payhere-mobilesdk-android/'
        }
    }
}

```
##### b. Allow Manifest attribute merge 

Open up the `AndroidManifest.xml` file in your Anrdoid project and make the following changes.

i. Declare the Android `tools` namespace in the `<manifest>` element.
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.domain.name"
    xmlns:tools="http://schemas.android.com/tools">
```

ii. Add the `replace` merge rule for the `android:allowBackup` attribute in the `<application>` element.
```xml
<application tools:replace="android:allowBackup">
```

### 3. iOS Pre-requisites

Open up the `Podfile` file in your iOS project and make the following changes.

1. Increase the platform version to iOS 11.
```ruby
platform :ios, '11.0'
```
2. Add the PayHere React Native SDK podspec.
```ruby
# Add this is if something similar doesn't already exist
use_react_native!(:path => config["reactNativePath"])

# Add these TWO lines just below 'use_react_native!'
pod 'payHereSDK', :git => 'https://github.com/PayHereDevs/payhere-mobilesdk-ios-rb.git'
pod 'payhere-mobilesdk-reactnative', :path => '../node_modules/@payhere/payhere-mobilesdk-reactnative'
```
3. Run the following command inside the 'ios' folder.
```sh
pod install
```

If you encounter linking errors related to "swiftWebKit", please refer to this [GitHub issue](https://github.com/PayHereLK/payhere-mobilesdk-reactnative/issues/9).

### 4. Whitelist Mobile App Package Name

a. Login to your PayHere Merchant Account and navigate to Settings  > Domains and Credentials.

b. Click the 'Add Domain/App' button.

c. Select 'App' from the first dropdown.

d. Add your React Native App package name (e.g. lk.bhasha.helakuru).

e. Take note of the hash value in the last field. This is your Merchant Secret for this specific mobile App.

f. Click 'Request to Approve'. 

> If you are using a PayHere Live Merchant Account, your App Package Name must be manually reviewed by our operations team. Please allow upto a day for this review process to complete.
>
> For more information, please contact [techsupport@payhere.lk](mailto:techsupport@payhere.lk)

### 5. Initiate a Payment Request to PayHere Payment Gateway ### 

##### a. One-time Payment Request

Creates a one-time payment request charged only once. To capture the payment details from your server, [read our docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-checkout#2-listening-to-payment-notification).

```ts
import { Alert } from 'react-native';
import PayHere from '@payhere/payhere-mobilesdk-reactnative';

const paymentObject = {
    "sandbox": true,                 // true if using Sandbox Merchant ID
    "merchant_id": "1211149",        // Replace your Merchant ID
    "notify_url": "http://sample.com/notify",
    "order_id": "ItemNo12345",
    "items": "Hello from React Native!",
    "amount": "50.00",
    "currency": "LKR",
    "first_name": "Saman",
    "last_name": "Perera",
    "email": "samanp@gmail.com",
    "phone": "0771234567",
    "address": "No.1, Galle Road",
    "city": "Colombo",
    "country": "Sri Lanka",
    "delivery_address": "No. 46, Galle road, Kalutara South",
    "delivery_city": "Kalutara",
    "delivery_country": "Sri Lanka",
    "custom_1": "",
    "custom_2": ""
};

PayHere.startPayment(
    paymentObject, 
    (paymentId) => {
        console.log("Payment Completed", paymentId);
    },
    (errorData) => {
        Alert.alert("PayHere Error", errorData);
    },
    () => {
        console.log("Payment Dismissed");
    }
);
```

##### b. Recurring Payment Request

Creates a subscription payment that is charged at a fixed frequency. To capture the payment details from your server, [read our docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-recurring#2-listening-to-payment-notification).

Read more about Recurring Payments [in our docs](https://support.payhere.lk/faq/recurring-billing).

```ts
import { Alert } from 'react-native';
import PayHere from '@payhere/payhere-mobilesdk-reactnative';

const paymentObject = {
    "sandbox": true,                 // true if using Sandbox Merchant ID
    "merchant_id": "1211149",        // Replace your Merchant ID
    "notify_url": "http://sample.com/notify",
    "order_id": "ItemNo12345",
    "items": "Hello from React Native!",
    "amount": "50.00",               // Recurring amount
    "recurrence": "1 Month",         // Recurring payment frequency
    "duration": "1 Year",            // Recurring payment duration
    "startup_fee": "10.00",          // Extra amount for first payment
    "currency": "LKR",
    "first_name": "Saman",
    "last_name": "Perera",
    "email": "samanp@gmail.com",
    "phone": "0771234567",
    "address": "No.1, Galle Road",
    "city": "Colombo",
    "country": "Sri Lanka",
    "delivery_address": "No. 46, Galle road, Kalutara South",
    "delivery_city": "Kalutara",
    "delivery_country": "Sri Lanka",
    "custom_1": "",
    "custom_2": ""
};

PayHere.startPayment(
    paymentObject, 
    (paymentId) => {
        console.log("Payment Completed", paymentId);
    },
    (errorData) => {
        Alert.alert("PayHere Error", errorData);
    },
    () => {
        console.log("Payment Dismissed");
    }
);
```

##### c. Preapproval Request

Tokenize customer card details for later usage with the [PayHere Charging API](https://support.payhere.lk/api-&-mobile-sdk/payhere-charging). To capture the payment details from your server, [read our docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-preapproval#2-listening-to-payment-notification).

Read more about Automated Charging [in our docs](https://support.payhere.lk/faq/automated-charging).  

```ts
import { Alert } from 'react-native';
import PayHere from '@payhere/payhere-mobilesdk-reactnative';

const paymentObject = {
    "sandbox": true,                 // true if using Sandbox Merchant ID
    "preapprove": true,              // Required
    "merchant_id": "1211149",        // Replace your Merchant ID
    "amount": "50.00",
    "notify_url": "http://sample.com/notify",
    "order_id": "ItemNo12345",
    "items": "Hello from React Native!",
    "currency": "LKR",
    "first_name": "Saman",
    "last_name": "Perera",
    "email": "samanp@gmail.com",
    "phone": "0771234567",
    "address": "No.1, Galle Road",
    "city": "Colombo",
    "country": "Sri Lanka",
    "amount": "50.00"
};

PayHere.startPayment(
    paymentObject, 
    (paymentId) => {
        console.log("Payment Completed", paymentId);
    },
    (errorData) => {
        Alert.alert("PayHere Error", errorData);
    },
    () => {
        console.log("Payment Dismissed");
    }
);
```

##### d. Hold-on-Card Request

Authorize (hold) charges on a customer's card for later use with the [PayHere Capture API](https://support.payhere.lk/api-&-mobile-sdk/payhere-capture). To capture the payment hold details from your server, [read out docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-authorize#2-listening-to-authorization-notification).

Read more about Hold-on-card [in our docs](https://support.payhere.lk/faq/hold-on-card).  

```js
import { Alert } from 'react-native';
import PayHere from '@payhere/payhere-mobilesdk-reactnative';

const paymentObject = {
    "sandbox": true,                // true if using Sandbox Merchant ID
    "authorize": true,              // Required
    "merchant_id": "1211149",       // Replace your Merchant ID
    "notify_url": "https://ent13zfovoz7d.x.pipedream.net/",
    "order_id": "ItemNo12345",
    "items": "Hello from React Native!",
    "currency": "LKR",
    "amount": "50.00",
    "first_name": "Saman",
    "last_name": "Perera",
    "email": "samanp@gmail.com",
    "phone": "0771234567",
    "address": "No.1, Galle Road",
    "city": "Colombo",
    "country": "Sri Lanka",
};

PayHere.startPayment(
    paymentObject, 
    (paymentId) => {
        console.log("Payment Authorized!");
    },
    (errorData) => {
        Alert.alert("PayHere Error", errorData);
    },
    () => {
        console.log("Payment Dismissed");
    }
);
```

### 6. Optionally, pass Item-wise Details

Starting with version `3.0.0` you can optionally pass the details of the line items in the order. These details will appear in the customer's invoice. Item-wise Details are supported in Onetime, Subscription and Authorization payment modes. It is not supported in Pre-approval payments.

Each item has four parameters. Their parameter names must be followed by the index of that item. For example:
```json
  "item_number_1": "ITM001",
  "item_name_1": "PayHere Sticker",
  "quantity_1": "2",
  "amount_1": "25.0",
```

An example Onetime payment request with 2 items is shown below. If you have specific questions, please raise them in the [Issues section](https://github.com/PayHereLK/payhere-mobilesdk-reactnative/issues).

```js
import { Alert } from 'react-native';
import PayHere from '@payhere/payhere-mobilesdk-reactnative';

const paymentObject = {
    "sandbox": true,                 // true if using Sandbox Merchant ID
    "merchant_id": "1211149",        // Replace your Merchant ID
    "notify_url": "http://sample.com/notify",
    "order_id": "ItemNo12345",
    "items": "Hello from React Native!",

    "item_number_1": "001",          // ** Item 1 **
    "item_name_1": "Test Item #1",
    "amount_1": "15.00",
    "quantity_1": "2",
    "item_number_2": "002",          // ** Item 2 **
    "item_name_2": "Test Item #2",
    "amount_2": "20.00",
    "quantity_2": "1",
    
    "amount": 50.00,
    "currency": "LKR",
    "first_name": "Saman",
    "last_name": "Perera",
    "email": "samanp@gmail.com",
    "phone": "0771234567",
    "address": "No.1, Galle Road",
    "city": "Colombo",
    "country": "Sri Lanka",
    "delivery_address": "No. 46, Galle road, Kalutara South",
    "delivery_city": "Kalutara",
    "delivery_country": "Sri Lanka",
    "custom_1": "",
    "custom_2": ""
};

PayHere.startPayment(
    paymentObject, 
    (paymentId) => {
        console.log("Payment Completed", paymentId);
    },
    (errorData) => {
        Alert.alert("PayHere Error", errorData);
    },
    () => {
        console.log("Payment Dismissed");
    }
);
```

## FAQ

#### What versions of React Native are supported?

React Native versions above `0.60.0` are supported.

#### What versions of iOS are supported?

The iOS component for this SDK supports iOS Versions above 11.0.

#### What versions of Android are supported? #### 

The Android component for this SDK supports Android Versions above API Level 17.

#### What are the parameters for the `PayHere.startPayment` method?

```js
PayHere.startPayment(
    paymentObject, 
    onCompletedHandler,
    onErrorHandler,
    onDismissedHandler
);
```

- `paymentObject` - _Object_
The payment parameters as a Javascript Object.

- `onCompletedHandler` - _Function_
Called with the PayHere Payment ID (_String_) as a parameter, for succesful payments.

- `onErrorHandler` - _Function_
Called with the Error (_String_) as a parameter, when an error occurs.

- `onDismissedHandler` - _Function_
Called with no parameters, when the payment popup is closed before payments are processed.

#### How to get payment details such as payment method, status, card holder etc.?

You must setup a Server Endpoint that accepts the asynchronous PayHere Payment Notification `POST` request, and pass its URL to the `notify_url` parameter of the `paymentObject`. 

Each payment request type (one-time/recurring/pre-approval) sends a different payment notification. Study the following sections for more information.

- One-time Payment Details: [read docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-checkout#2-listening-to-payment-notification)
- Recurring Payment Details: [read docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-recurring#2-listening-to-payment-notification)
- Preapproval Details: [read docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-preapproval#2-listening-to-preapproval-notification)
- Authorization (Hold) Details: [read docs](https://support.payhere.lk/api-&-mobile-sdk/payhere-authorize#2-listening-to-authorization-notification)

#### I am getting an error saying, "Could not GET 'https://dl.bintray.com..."

PayHere React Native SDK versions prior to `1.0.11` (`1.0.8` and previous versions) depended on an older version of the PayHere Android SDK which is no longer available through the bintray.com Maven repository. Update to the latest version `1.0.11` and try again.

If you are still experiencing issues, make sure you have followed the new "2. Android Pre-requisites" section with updated instructions for SDK versions `1.0.11` and above.

#### I am getting Linker errors when building for iOS

This is a known issue that occurs due to the use of Swift files in the PayHere iOS SDK which is used internally by this SDK. Please refer to the following GitHub issue on the documented steps to resolve it.

[GitHub Issue](https://github.com/PayHereLK/payhere-mobilesdk-reactnative/issues/9)

#### I am getting a 'Multiple commands produce...' error when building for iOS

Please see this GitHub Issue for troubleshooting.

[GitHub Issue](https://github.com/PayHereLK/payhere-mobilesdk-reactnative/issues/14)

#### Can I pass details about each item in the Order?

Yes! Starting from version `3.0.0`, the PayHere React Native SDK supports Item-wise Parameters. Please read [Section 6](https://github.com/PayHereLK/payhere-mobilesdk-reactnative#6-optionally-pass-item-wise-details) above.

#### Does this SDK support Payment Authorization (Hold on Card) and Capture?

Yes and No. Starting from version `3.0.0` this SDK supports Authorization (also known as Hold on Card) requests. Authorizations generate an `authorization_token` which is sent as a POST request to your `notify_url`. 

From there you must use the [PayHere Capture API](https://support.payhere.lk/api-&-mobile-sdk/payhere-capture) to use the genereated token and perform the capture. You can read more about the PayHere Hold on Card Feature by [reading our docs](https://support.payhere.lk/faq/hold-on-card).

#### I have a different question. Where should I raise my issues?

1. You can raise issues directly at the [Issues section](https://github.com/PayHereLK/payhere-mobilesdk-reactnative/issues) for the SDK's GitHub page.
2. You can contact a PayHere Developer for technical support by mailing your issue and relevant code/screenshots to [techsupport@payhere.lk](mailto:techsupport@payhere.lk)
