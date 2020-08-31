import { NativeModules } from 'react-native';

const PayHere = function(){
    var startPayment = function(paymentObject, onCompleted, onError, onDismissed) {
        NativeModules.PayhereOfficial.startPayment(paymentObject, (data) => {
            // console.log("PayHere.startPayment Result =", data);
            try{
                if (data.success){
                    onCompleted(data.jsdata);
                }
                else{
                    if (data.jscallback == 'error'){
                        onError(data.jsdata);
                    }
                    else if (data.jscallback == 'dismiss'){
                        onDismissed();
                    }
                    else{
                        onError('Unknown callback');
                    }
                }
            }
            catch(error){
                onError(error);
            }
        });
    }

    return {
        startPayment
    }
}();

export default PayHere;