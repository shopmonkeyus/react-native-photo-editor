#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(PhotoEditor, NSObject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}
RCT_EXTERN_METHOD(open:(NSDictionary *)options
                 callback:(RCTResponseSenderBlock)callback
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
