//
//  BRPtouchDevice.h
//  BRSearchModule
//
//  Created by Sha Peng on 6/22/15.
//  Copyright (c) 2015 Brother. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface BRPtouchDeviceInfo : NSObject

@property	(copy,nonatomic)NSString*	strIPAddress;
@property	(copy,nonatomic)NSString*	strLocation;
@property	(copy,nonatomic)NSString*	strModelName;
@property	(copy,nonatomic)NSString*	strPrinterName;
@property	(copy,nonatomic)NSString*	strSerialNumber;
@property	(copy,nonatomic)NSString*	strNodeName;
@property	(copy,nonatomic)NSString*	strMACAddress;

- (NSString *)description;

@end
