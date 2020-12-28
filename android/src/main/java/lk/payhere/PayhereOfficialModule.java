package lk.payhere;

import android.app.Activity;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.Address;
import lk.payhere.androidsdk.model.Customer;
import lk.payhere.androidsdk.model.InitBaseRequest;
import lk.payhere.androidsdk.model.InitPreapprovalRequest;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
import lk.payhere.androidsdk.model.StatusResponse;

@SuppressWarnings("unused")
public class PayhereOfficialModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final static int PAYHERE_REQUEST = 11010;
    private final ReactApplicationContext reactContext;
    private Callback lastCallback = null;

    /* MARK: Definitions */

    private static final class PaymentObjectKey{
        public final static String sandbox = "sandbox";
        public final static String merchantId = "merchant_id";
        public final static String merchantSecret = "merchant_secret";
        public final static String notifyUrl = "notify_url";
        public final static String orderId = "order_id";
        public final static String items = "items";
        public final static String amount = "amount";
        public final static String currency = "currency";
        public final static String firstName = "first_name";
        public final static String lastName = "last_name";
        public final static String email = "email";
        public final static String phone = "phone";
        public final static String address = "address";
        public final static String city = "city";
        public final static String country = "country";
        public final static String deliveryAddress = "delivery_address";
        public final static String deliveryCity = "delivery_city";
        public final static String deliveryCountry = "delivery_country";
        public final static String customOne = "custom_1";
        public final static String customTwo = "custom_2";
        public final static String recurrence = "recurrence";
        public final static String duration = "duration";
        public final static String startupFee = "startup_fee";
        public final static String preapprove = "preapprove";

        public PaymentObjectKey(){}
    }

    private static final class ResultKey{
        public final static String success = "success";
        public final static String callbackType = "jscallback";
        public final static String data = "jsdata";

        private ResultKey(){}
    }

    private static final class ResultCallbackType{
        public final static String complete = "complete";
        public final static String dismiss = "dismiss";
        public final static String error = "error";

        private ResultCallbackType(){}
    }

    private static final class PayHereKeyExtractionException extends Exception{

        private String parameter;
        private String type;
        private Boolean keyExisted;

        /**
         * Exception for key that exists, but failed to cast to String
         * @param key Key of value extracted (e.g. 'merchant_id')
         */
        public PayHereKeyExtractionException(String key){
            this.parameter = key;
            this.type = "String";
            this.keyExisted = true;
        }

        /**
         * Exception for key that may or may not exist, but failed to cast to String
         * @param key Key of value extracted (e.g. 'merchant_id')
         * @param keyExists Whether the key existed at point of extraction
         */
        public PayHereKeyExtractionException(String key, Boolean keyExists){
            this.parameter = key;
            this.type = "String";
            this.keyExisted = keyExists;
        }

        /**
         * Exception for key that exists, but failed to cast to type
         * @param key Key of value extracted (e.g. 'merchant_id')
         * @param type The type of the parameter expected but didn't exist (e.g. 'Boolean')
         */
        public PayHereKeyExtractionException(String key, String type){
            this.parameter = key;
            this.type = type;
            this.keyExisted = true;
        }
        /**
         * Exception for key that may or may not, but failed to cast to type
         * @param key Key of value extracted (e.g. 'merchant_id')
         * @param type The type of the parameter expected but didn't exist (e.g. 'Boolean')
         * @param keyExists Whether the key existed at point of extraction
         */
        public PayHereKeyExtractionException(String key, String type, Boolean keyExists){
            this.parameter = key;
            this.type = type;
            this.keyExisted = keyExists;
        }

        @Override
        public String toString() {
            return "PayHereKeyExtractionException{" +
                    "parameter='" + parameter + '\'' +
                    ", type='" + type + '\'' +
                    ", keyExisted=" + keyExisted +
                    '}';
        }
    }

    /* END MARK: Definitions */

    public PayhereOfficialModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "PayhereOfficial";
    }

    @ReactMethod
    public void startPayment(ReadableMap payment, Callback callback) {

        String errorString;
        HashMap<String, Object> paymentObject = payment.toHashMap();

        this.lastCallback = callback;

        this.log(payment.toHashMap().toString());

        /*
         * PayHere Android SDK sends result to activity, not custom listener.
         *
         * Since we're a module, adding this React listener is the only
         * way to tap into the activity result.
         */
        reactContext.addActivityEventListener(this);

        if (payment.hasKey(PaymentObjectKey.preapprove) && payment.getBoolean(PaymentObjectKey.preapprove))
            errorString = this.createAndLaunchPreapprovalRequest(paymentObject, reactContext);
        else{
            boolean recurrenceCheck = payment.hasKey(PaymentObjectKey.recurrence) && payment.getString(PaymentObjectKey.recurrence) != null;
            boolean durationCheck = payment.hasKey(PaymentObjectKey.duration) && payment.getString(PaymentObjectKey.duration) != null;

            if (recurrenceCheck && durationCheck)
                errorString = this.createAndLaunchRecurringRequest(paymentObject, reactContext);
            else
                errorString = this.createAndLaunchOnetimeRequest(paymentObject, reactContext);
        }

        if (errorString != null){
            // Error occurred. Request is not launched.
            // Invoke callback with error details.

            this.sendError(errorString);
        }

    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == PAYHERE_REQUEST) {
            if (data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                if (resultCode == Activity.RESULT_OK) {
                    String msg;
                    if (response != null)
                        if (response.isSuccess()) {
                            msg = "Activity result:" + response.getData().toString();
                            String paymentNo = Long.toString(response.getData().getPaymentNo());
                            this.sendCompleted(paymentNo);
                        } else {
                            msg = "Result:" + response.toString();
                            this.sendError(response.getData().getMessage());
                        }
                    else {
                        msg = "Result: no response";
                        this.log(msg);
                        this.sendDismissed();
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    if (response != null){
                        switch(response.getStatus()){
                            case PHResponse.STATUS_ERROR_CANCELED:
                                this.sendDismissed();
                                break;

                            case PHResponse.STATUS_ERROR_PAYMENT:
                                if (response.getData().getMessage() == null)
                                    this.sendDismissed();
                                else
                                    this.sendError(response.getData().getMessage());
                                break;

                            case PHResponse.STATUS_ERROR_NETWORK:
                                this.sendError("Network Error");
                                break;

                            case PHResponse.STATUS_ERROR_VALIDATION:
                                this.sendError("Parameter Validation Error");
                                break;

                            case PHResponse.STATUS_ERROR_DATA:
                                this.sendError("Intent Data not Present");
                                break;

                            case PHResponse.STATUS_ERROR_UNKNOWN:
                            default:
                                this.sendError("Unknown error occurred");
                                break;
                        }
                    }
                    else
                        this.sendDismissed();
                }
            }
            else if (data == null){
                this.sendDismissed();
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // No implementation
    }

    private void log(String msg){
        Log.d("PayHere", msg);
    }


    /**
     * Send an error message back to JS interface
     * JS: onError
     * @param error Error msg to pass (accepts null)
     */
    private void sendError(String error){

        if (lastCallback == null){
            throw new RuntimeException("callback must not be null");
        }

        String finalError = error == null ? "Null error" : error;

        WritableMap map = Arguments.createMap();

        map.putBoolean(ResultKey.success, false);
        map.putString(ResultKey.callbackType, ResultCallbackType.error);
        map.putString(ResultKey.data, finalError);

        lastCallback.invoke(map);
    }

    /**
     * Send a dismissed message back to JS interface
     * JS: onDismissed
     */
    private void sendDismissed(){

        if (lastCallback == null){
            throw new RuntimeException("callback must not be null");
        }

        WritableMap map = Arguments.createMap();

        map.putBoolean(ResultKey.success, false);
        map.putString(ResultKey.callbackType, ResultCallbackType.dismiss);

        lastCallback.invoke(map);
    }

    /**
     * Send a dismissed message back to JS interface
     * JS: onCompleted
     * @param data A string to send back (usually PayHere paymentID)
     */
    private void sendCompleted(String data){

        if (lastCallback == null){
            throw new RuntimeException("callback must not be null");
        }

        WritableMap map = Arguments.createMap();

        map.putBoolean(ResultKey.success, true);
        map.putString(ResultKey.callbackType, ResultCallbackType.complete);
        map.putString(ResultKey.data, data);

        lastCallback.invoke(map);
    }

    /**
     * Extracts a String Key from a HashMap.
     * @param map Map to extract keys from
     * @param key Key of value to extract
     * @return String value of extracted key. Null is never returned: exception thrown instead.
     * @throws PayHereKeyExtractionException Key extraction error information
     */
    private String extract(HashMap<String, Object> map, String key) throws PayHereKeyExtractionException{
        if (map.containsKey(key)){
            Object raw = map.get(key);
            if (raw == null){
                throw new PayHereKeyExtractionException(key, "Object", true);
            }
            else{
                try {
                    return raw.toString();
                }
                catch(Exception e){
                    throw new PayHereKeyExtractionException(key, true);
                }
            }
        }
        else{
            throw new PayHereKeyExtractionException(key, false);
        }
    }

    /**
     * Extracts a String Key from a HashMap.
     * If the key doesnt exist, returns null.
     * @param map Map to extract keys from
     * @param key Key of value to extract
     * @return String value of extracted key. Null is returned if key or value doesn't exist.
     */
    private String extractOptional(HashMap<String, Object> map, String key){
        if (map.containsKey(key)){
            Object raw = map.get(key);
            if (raw == null){
                return null;
            }
            else{
                try {
                    return raw.toString();
                }
                catch(Exception e){
                    return null;
                }
            }
        }
        else{
            return null;
        }
    }

    /**
     * Extracts an PayHere Amount from HashMap.
     * @param map Map to extract keys from
     * @param key Key of value to extract
     * @return Double value of extracted key. Null is never returned: exception thrown instead.
     * @throws PayHereKeyExtractionException Key extraction error information
     */
    private Double extractAmount(HashMap<String, Object> map, String key) throws PayHereKeyExtractionException{
        if (map.containsKey(key)){
            Object raw = map.get(key);
            if (raw == null){
                throw new PayHereKeyExtractionException(key, "Object", true);
            }
            else{
                try {
                    String str = raw.toString();
                    return Double.valueOf(str);
                }
                catch(Exception e){
                    throw new PayHereKeyExtractionException(key, "Double", true);
                }
            }
        }
        else{
            throw new PayHereKeyExtractionException(key, false);
        }
    }

    /**
     * Extracts an PayHere Amount from HashMap.
     * If the key doesnt exist, returns null.
     * @param map Map to extract keys from
     * @param key Key of value to extract
     * @return Double value of extracted key. Null is returned if key or value doesn't exist.
     */
    private Double extractOptionalAmount(HashMap<String, Object> map, String key){
        if (map.containsKey(key)){
            Object raw = map.get(key);
            if (raw == null){
                return null;
            }
            else{
                try {
                    String str = raw.toString();
                    return Double.valueOf(str);
                }
                catch(Exception e){
                    return null;
                }
            }
        }
        else{
            return null;
        }
    }

    /**
     * Extracts a bool from HashMap.
     * @param map Map to extract keys from
     * @param key Key of value to extract
     * @return Boolean value of extracted key. Null is never returned: exception thrown instead.
     * @throws PayHereKeyExtractionException Key extraction error information
     */
    private Boolean extractBoolean(HashMap<String, Object> map, String key) throws PayHereKeyExtractionException{
        if (map.containsKey(key)){
            Object raw = map.get(key);
            if (raw == null){
                throw new PayHereKeyExtractionException(key, true);
            }
            else{
                try {
                    String str = raw.toString();
                    return Boolean.valueOf(str);
                }
                catch(Exception e){
                    throw new PayHereKeyExtractionException(key, true);
                }
            }
        }
        else{
            throw new PayHereKeyExtractionException(key, false);
        }
    }

    private String createAndLaunchOnetimeRequest(HashMap<String, Object> o, ReactApplicationContext reactContext){
        String error = null;

        try {

            Item item = new Item(
                null,
                this.extract(o, PaymentObjectKey.items),
                1,
                this.extractAmount(o, PaymentObjectKey.amount)
            );

            InitRequest req = new InitRequest();

            req.setMerchantId(          this.extract(o,         PaymentObjectKey.merchantId));
            req.setMerchantSecret(      this.extract(o,         PaymentObjectKey.merchantSecret));
            req.setNotifyUrl(           this.extract(o,         PaymentObjectKey.notifyUrl));
            req.setCurrency(            this.extract(o,         PaymentObjectKey.currency));
            req.setAmount(              this.extractAmount(o,   PaymentObjectKey.amount));
            req.setOrderId(             this.extract(o,         PaymentObjectKey.orderId));
            req.setItemsDescription(    this.extract(o,         PaymentObjectKey.items));

            String custom1 =            this.extractOptional(o, PaymentObjectKey.customOne);
            String custom2 =            this.extractOptional(o, PaymentObjectKey.customTwo);

            if (custom1 != null) {
                req.setCustom1(custom1);
            }

            if (custom2 != null) {
                req.setCustom2(custom2);
            }

            Customer customer = req.getCustomer();
            customer.setFirstName(      this.extract(o,         PaymentObjectKey.firstName));
            customer.setLastName(       this.extract(o,         PaymentObjectKey.lastName));
            customer.setEmail(          this.extract(o,         PaymentObjectKey.email));
            customer.setPhone(          this.extract(o,         PaymentObjectKey.phone));

            Address customerAddress = customer.getAddress();
            customerAddress.setAddress( this.extract(o,         PaymentObjectKey.address));
            customerAddress.setCity(    this.extract(o,         PaymentObjectKey.city));
            customerAddress.setCountry( this.extract(o,         PaymentObjectKey.country));

            Address customerDeliveryAddress = customer.getDeliveryAddress();
            String deliveryAddress =    this.extractOptional(o, PaymentObjectKey.deliveryAddress);
            String deliveryCity =       this.extractOptional(o, PaymentObjectKey.deliveryCity);
            String deliveryCountry =    this.extractOptional(o, PaymentObjectKey.deliveryCountry);

            if (deliveryAddress != null)
                customerDeliveryAddress.setAddress(deliveryAddress);

            if (deliveryCity != null)
                customerDeliveryAddress.setCity(deliveryCity);

            if (deliveryCountry != null)
                customerDeliveryAddress.setCountry(deliveryCountry);

            req.getItems().add(item);

            Boolean isSandbox = this.extractBoolean(o,          PaymentObjectKey.sandbox);
            this.launchRequest(req, reactContext, isSandbox);

        }
        catch(PayHereKeyExtractionException exc){
            error = exc.toString();
        }

        return error;
    }

    /**
     * @warning: UNTESTED, development happened during PayHere System Upgrade.
     */
    private String createAndLaunchRecurringRequest(HashMap<String, Object> o, ReactApplicationContext reactContext){
        String error = null;

        try {

            InitRequest req = new InitRequest();

            req.setMerchantId(          this.extract(o,         PaymentObjectKey.merchantId));
            req.setMerchantSecret(      this.extract(o,         PaymentObjectKey.merchantSecret));
            req.setNotifyUrl(           this.extract(o,         PaymentObjectKey.notifyUrl));
            req.setCurrency(            this.extract(o,         PaymentObjectKey.currency));
            req.setAmount(              this.extractAmount(o,   PaymentObjectKey.amount));
            req.setRecurrence(          this.extract(o,         PaymentObjectKey.recurrence));
            req.setDuration(            this.extract(o,         PaymentObjectKey.duration));
            req.setOrderId(             this.extract(o,         PaymentObjectKey.orderId));
            req.setItemsDescription(    this.extract(o,         PaymentObjectKey.items));

            Double startupFee =         this.extractOptionalAmount(o, PaymentObjectKey.startupFee);
            if (startupFee != null){
                req.setStartupFee(startupFee);
            }

            String custom1 =            this.extractOptional(o, PaymentObjectKey.customOne);
            String custom2 =            this.extractOptional(o, PaymentObjectKey.customTwo);

            if (custom1 != null) {
                req.setCustom1(custom1);
            }

            if (custom2 != null) {
                req.setCustom2(custom2);
            }

            Customer customer = req.getCustomer();
            customer.setFirstName(      this.extract(o,         PaymentObjectKey.firstName));
            customer.setLastName(       this.extract(o,         PaymentObjectKey.lastName));
            customer.setEmail(          this.extract(o,         PaymentObjectKey.email));
            customer.setPhone(          this.extract(o,         PaymentObjectKey.phone));

            Address customerAddress = customer.getAddress();
            customerAddress.setAddress( this.extract(o,         PaymentObjectKey.address));
            customerAddress.setCity(    this.extract(o,         PaymentObjectKey.city));
            customerAddress.setCountry( this.extract(o,         PaymentObjectKey.country));

            Address customerDeliveryAddress = customer.getDeliveryAddress();
            String deliveryAddress =    this.extractOptional(o, PaymentObjectKey.deliveryAddress);
            String deliveryCity =       this.extractOptional(o, PaymentObjectKey.deliveryCity);
            String deliveryCountry =    this.extractOptional(o, PaymentObjectKey.deliveryCountry);

            if (deliveryAddress != null)
                customerDeliveryAddress.setAddress(deliveryAddress);

            if (deliveryCity != null)
                customerDeliveryAddress.setCity(deliveryCity);

            if (deliveryCountry != null)
                customerDeliveryAddress.setCountry(deliveryCountry);

            Boolean isSandbox = this.extractBoolean(o,          PaymentObjectKey.sandbox);
            this.launchRequest(req, reactContext, isSandbox);

        }
        catch(PayHereKeyExtractionException exc){
            error = exc.toString();
        }

        return error;
    }

    /**
     * @warning: UNTESTED, development happened during PayHere System Upgrade.
     */
    private String createAndLaunchPreapprovalRequest(HashMap<String, Object> o, ReactApplicationContext reactContext){
        String error = null;

        try {

            InitPreapprovalRequest req = new InitPreapprovalRequest();

            req.setMerchantId(          this.extract(o,         PaymentObjectKey.merchantId));
            req.setMerchantSecret(      this.extract(o,         PaymentObjectKey.merchantSecret));
            req.setNotifyUrl(           this.extract(o,         PaymentObjectKey.notifyUrl));
            req.setCurrency(            this.extract(o,         PaymentObjectKey.currency));
            req.setOrderId(             this.extract(o,         PaymentObjectKey.orderId));
            req.setItemsDescription(    this.extract(o,         PaymentObjectKey.items));

            String custom1 =            this.extractOptional(o, PaymentObjectKey.customOne);
            String custom2 =            this.extractOptional(o, PaymentObjectKey.customTwo);

            if (custom1 != null) {
                req.setCustom1(custom1);
            }

            if (custom2 != null) {
                req.setCustom2(custom2);
            }

            Customer customer = req.getCustomer();
            customer.setFirstName(      this.extract(o,         PaymentObjectKey.firstName));
            customer.setLastName(       this.extract(o,         PaymentObjectKey.lastName));
            customer.setEmail(          this.extract(o,         PaymentObjectKey.email));
            customer.setPhone(          this.extract(o,         PaymentObjectKey.phone));

            Address customerAddress = customer.getAddress();
            customerAddress.setAddress( this.extract(o,         PaymentObjectKey.address));
            customerAddress.setCity(    this.extract(o,         PaymentObjectKey.city));
            customerAddress.setCountry( this.extract(o,         PaymentObjectKey.country));

            Address customerDeliveryAddress = customer.getDeliveryAddress();
            String deliveryAddress =    this.extractOptional(o, PaymentObjectKey.deliveryAddress);
            String deliveryCity =       this.extractOptional(o, PaymentObjectKey.deliveryCity);
            String deliveryCountry =    this.extractOptional(o, PaymentObjectKey.deliveryCountry);

            if (deliveryAddress != null)
                customerDeliveryAddress.setAddress(deliveryAddress);

            if (deliveryCity != null)
                customerDeliveryAddress.setCity(deliveryCity);

            if (deliveryCountry != null)
                customerDeliveryAddress.setCountry(deliveryCountry);

            Boolean isSandbox = this.extractBoolean(o,          PaymentObjectKey.sandbox);
            this.launchRequest(req, reactContext, isSandbox);

        }
        catch(PayHereKeyExtractionException exc){
            error = exc.toString();
        }

        return error;
    }

    private void launchRequest(InitRequest req, ReactApplicationContext reactContext, boolean isSandbox){
        Intent intent = new Intent(reactContext, PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

        if (isSandbox)
            PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
        else
            PHConfigs.setBaseUrl(PHConfigs.LIVE_URL);

        reactContext.startActivityForResult(intent, PAYHERE_REQUEST, Bundle.EMPTY);
    }

    private void launchRequest(InitPreapprovalRequest req, ReactApplicationContext reactContext, boolean isSandbox){
        Intent intent = new Intent(reactContext, PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

        if (isSandbox)
            PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
        else
            PHConfigs.setBaseUrl(PHConfigs.LIVE_URL);

        reactContext.startActivityForResult(intent, PAYHERE_REQUEST, Bundle.EMPTY);
    }
}
