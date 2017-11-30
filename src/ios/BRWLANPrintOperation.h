//
//  BRWLANPrintOperation.h
//  SDK_Sample_Ver2
//
//  Created by Kusumoto Naoki on 2015/08/18.
//  Copyright (c) 2015年 Kusumoto Naoki. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <BRPtouchPrinterKit/BRPtouchPrinterKit.h>

@interface BRWLANPrintOperation : NSOperation {
}
@property(nonatomic, assign) BOOL communicationResultForWLAN;
@property(nonatomic, assign) PTSTATUSINFO resultStatus;

-(id)initWithOperation:(BRPtouchPrinter *)targetPtp
              printInfo:(BRPtouchPrintInfo *)targetPrintInfo
                 imgRef:(CGImageRef)targetImgRef
          numberOfPaper:(int)targetNumberOfPaper
              ipAddress:(NSString *)targetIPAddress;
@end
