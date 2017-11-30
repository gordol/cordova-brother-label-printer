//
//  BRPtouchPrinterData.h
//  BRPtouchPrinterKit
//
//  Created by BIL on 12/06/26.
//  Copyright (c) 2012 Brother Industries, Ltd. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface BRPtouchPrinterData : NSObject
{
	NSArray*	aryPrinterList;
}

- (NSArray*)getPrinterList;

@end
