//
//  BRPtouchNetwork.h
//  BRPtouchPrinterKit
//
//  Created by BIL on 12/02/23.
//  Copyright (c) 2012 Brother Industries, Ltd. All rights reserved.
//

#import <Foundation/Foundation.h>

#include "BRPtouchDeviceInfo.h"

//  Return value
#define RET_FALSE       0
#define RET_TRUE        1

@protocol BRPtouchNetworkDelegate;

@class PLNetworkModule;

@interface BRPtouchNetworkManager : NSObject <NSNetServiceBrowserDelegate,NSNetServiceDelegate>


@property(retain, nonatomic) NSMutableArray* registeredPrinterNames;
@property(assign, nonatomic) BOOL isEnableIPv6Search;


- (int)startSearch: (int)searchTime;
- (NSArray*)getPrinterNetInfo;

- (BOOL)setPrinterNames:(NSArray*)strPrinterNames;
- (BOOL)setPrinterName:(NSString*)strPrinterName;

- (id)initWithPrinterNames:(NSArray*)strPrinterNames;
- (id)initWithPrinterName:(NSString*)strPrinterName;

@property (nonatomic, assign) id <BRPtouchNetworkDelegate> delegate;

@end

@protocol BRPtouchNetworkDelegate <NSObject>

-(void) didFinishSearch:(id)sender;

@end
