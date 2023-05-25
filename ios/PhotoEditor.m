#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(PhotoEditor, RCTEventEmitter)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}
RCT_EXTERN_METHOD(open:(NSDictionary *)options
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
