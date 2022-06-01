import { Alert } from 'react-native';
import PayHere from '@payhere/payhere-mobilesdk-reactnative';

function getSampleObject(){
    const paymentObject = {
        "sandbox": false,                 // true if using Sandbox Merchant ID
        "merchant_id": "210251",        // Replace your Merchant ID
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

    return paymentObject;
}

function handleOnetime(){
    const obj = getSampleObject();
    PayHere.startPayment(
        obj, 
        (paymentId) => {
            Alert.alert("Payment Complete!", `Payment ID: ${paymentId}`);
        },
        (error) => {
            Alert.alert("Payment Error", `Payment Error: ${error}`);
        },
        () => {
            Alert.alert("Payment Dismissed", "");
        }
    )
}

export const Handler = {
    onetime: handleOnetime
};