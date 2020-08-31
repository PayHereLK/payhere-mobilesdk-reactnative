#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(PayhereOfficial, NSObject)
RCT_EXTERN_METHOD(
    startPayment:(NSDictionary *)payment 
    callback:(RCTResponseSenderBlock)callback)
@end
