//
//  BRBluetoothPrintOperation.m
//  SDK_Sample_Ver2
//
//  Created by Kusumoto Naoki on 2015/08/18.
//  Copyright (c) 2015年 Kusumoto Naoki. All rights reserved.
//

#import "BRUserDefaults.h"
#import "BRBluetoothPrintOperation.h"

@interface BRBluetoothPrintOperation () {
}
@property(nonatomic, assign) BOOL isExecutingForBT;
@property(nonatomic, assign) BOOL isFinishedForBT;

@property(nonatomic, weak) BRPtouchPrinter    *ptp;
@property(nonatomic, strong) BRPtouchPrintInfo  *printInfo;
@property(nonatomic, assign) CGImageRef         imgRef;
@property(nonatomic, assign) int                numberOfPaper;
@property(nonatomic, strong) NSString           *serialNumber;

@end


@implementation BRBluetoothPrintOperation

-(id)initWithOperation:(BRPtouchPrinter *)targetPtp
             printInfo:(BRPtouchPrintInfo *)targetPrintInfo
                imgRef:(CGImageRef)targetImgRef
         numberOfPaper:(int)targetNumberOfPaper
          serialNumber:(NSString *)targetSerialNumber {
    self = [super init];
    if (self) {
        self.ptp            = targetPtp;
        self.printInfo      = targetPrintInfo;
        self.imgRef         = targetImgRef;
        self.numberOfPaper  = targetNumberOfPaper;
        self.serialNumber   = targetSerialNumber;
    }

    return self;
}

+(BOOL)automaticallyNotifiesObserversForKey:(NSString*)key {
    if (
        [key isEqualToString:@"communicationResultForBT"]   ||
        [key isEqualToString:@"isExecutingForBT"]           ||
        [key isEqualToString:@"isFinishedForBT"]) {
        return YES;
    }
    return [super automaticallyNotifiesObserversForKey:key];
}

-(void)main {
    self.isExecutingForBT = YES;

    [self.ptp setupForBluetoothDeviceWithSerialNumber:self.serialNumber];

    if ([self.ptp isPrinterReady]) {
        self.communicationResultForBT = [self.ptp startCommunication];
        if (self.communicationResultForBT) {
            [self.ptp setPrintInfo:self.printInfo];

            int printResult = [self.ptp printImage:self.imgRef copy:self.numberOfPaper];
            if (printResult == 0) {
                PTSTATUSINFO resultstatus;
                [self.ptp getPTStatus:&resultstatus];
                self.resultStatus = resultstatus;
            }
        }

        [self.ptp endCommunication];

    } else {
        self.communicationResultForBT = NO;
    }

    self.isExecutingForBT = NO;
    self.isFinishedForBT = YES;
}

@end
