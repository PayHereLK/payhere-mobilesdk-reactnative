import { NativeModules } from 'react-native';

const JS_CALLBACK_IS_ERROR = 'error';
const JS_CALLBACK_IS_DISMISS = 'dismiss';


const PayHere = function(){
    var startPayment = function(
      paymentObject,
      onCompleted,
      onError,
      onDismissed
    ) {
        NativeModules.PayhereOfficial.startPayment(
          paymentObject,
          ({success, jsdata, jscallback}) => {

            try{
                if (success){
                    onCompleted(jsdata);
                }
                else{
                    if (jscallback == JS_CALLBACK_IS_ERROR){
                        onError(jsdata);
                    }
                    else if (jscallback == JS_CALLBACK_IS_DISMISS){
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
