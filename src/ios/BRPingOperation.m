/*
 Contains:   Implements ping.

 Written by: DTS

 Copyright:  Copyright (c) 2010-2012 Apple Inc. All Rights Reserved.

 Disclaimer: IMPORTANT: This Apple software is supplied to you by Apple Inc.
 ("Apple") in consideration of your agreement to the following
 terms, and your use, installation, modification or
 redistribution of this Apple software constitutes acceptance of
 these terms.  If you do not agree with these terms, please do
 not use, install, modify or redistribute this Apple software.

 In consideration of your agreement to abide by the following
 terms, and subject to these terms, Apple grants you a personal,
 non-exclusive license, under Apple's copyrights in this
 original Apple software (the "Apple Software"), to use,
 reproduce, modify and redistribute the Apple Software, with or
 without modifications, in source and/or binary forms; provided
 that if you redistribute the Apple Software in its entirety and
 without modifications, you must retain this notice and the
 following text and disclaimers in all such redistributions of
 the Apple Software. Neither the name, trademarks, service marks
 or logos of Apple Inc. may be used to endorse or promote
 products derived from the Apple Software without specific prior
 written permission from Apple.  Except as expressly stated in
 this notice, no other rights or licenses, express or implied,
 are granted by Apple herein, including but not limited to any
 patent rights that may be infringed by your derivative works or
 by other works in which the Apple Software may be incorporated.

 The Apple Software is provided by Apple on an "AS IS" basis.
 APPLE MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 WITHOUT LIMITATION THE IMPLIED WARRANTIES OF NON-INFRINGEMENT,
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, REGARDING
 THE APPLE SOFTWARE OR ITS USE AND OPERATION ALONE OR IN
 COMBINATION WITH YOUR PRODUCTS.

 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT,
 INCIDENTAL OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) ARISING IN ANY WAY
 OUT OF THE USE, REPRODUCTION, MODIFICATION AND/OR DISTRIBUTION
 OF THE APPLE SOFTWARE, HOWEVER CAUSED AND WHETHER UNDER THEORY
 OF CONTRACT, TORT (INCLUDING NEGLIGENCE), STRICT LIABILITY OR
 OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 */

/***********************************************************************************

 BRPingOperation.h

 Modified by Brother on 5/22/14.
 Copyright (c) 2014 Brother.

 ***********************************************************************************/

#import "BRPingOperation.h"

#import "APPingModule.h"
#include <sys/socket.h>
#include <netdb.h>

// Returns a dotted decimal string for the specified address (a (struct sockaddr)
// within the address NSData).
static NSString * DisplayAddressForAddress(NSData * address) {
    int         err;
    NSString *  result;
    char        hostStr[NI_MAXHOST];

    result = nil;

    if (address != nil) {
        err = getnameinfo([address bytes], (socklen_t) [address length], hostStr, sizeof(hostStr), NULL, 0, NI_NUMERICHOST);
        if (err == 0) {
            result = [NSString stringWithCString:hostStr encoding:NSASCIIStringEncoding];
            assert(result != nil);
        }
    }

    return result;
}

@interface BRPingOperation () <APPingModuleDelegate>

@property (nonatomic, strong, readwrite) APPingModule *   pinger;
@property (nonatomic, strong, readwrite) NSTimer *      sendTimer;

@end

@implementation BRPingOperation {
}
@synthesize delegate;
@synthesize pinger    = _pinger;
@synthesize sendTimer = _sendTimer;

// Given an NSError, returns a short error string that we can print, handling
// some special cases along the way.
-(NSString *)shortErrorFromError:(NSError *)error {
    NSString *      result;
    NSNumber *      failureNum;
    int             failure;
    const char *    failureStr;

    assert(error != nil);

    result = nil;

    // Handle DNS errors as a special case.

    if ( [[error domain] isEqual:(NSString *)kCFErrorDomainCFNetwork] && ([error code] == kCFHostErrorUnknown) ) {
        failureNum = [[error userInfo] objectForKey:(id)kCFGetAddrInfoFailureKey];
        if ( [failureNum isKindOfClass:[NSNumber class]] ) {
            failure = [failureNum intValue];
            if (failure != 0) {
                failureStr = gai_strerror(failure);
                if (failureStr != NULL) {
                    result = [NSString stringWithUTF8String:failureStr];
                    assert(result != nil);
                }
            }
        }
    }

    // Otherwise try various properties of the error object.

    if (result == nil) {
        result = [error localizedFailureReason];
    }
    if (result == nil) {
        result = [error localizedDescription];
    }
    if (result == nil) {
        result = [error description];
    }
    assert(result != nil);
    return result;
}

-(void)stopRunningPing {
//    self.pinger = nil;
//    CFRunLoopStop(CFRunLoopGetCurrent());
    [self.pinger stop];
    self.pinger = nil;

    [self.sendTimer invalidate];
    self.sendTimer = nil;
}

// The Objective-C 'main' for this program.  It creates a PingModule object
// and runs the runloop sending pings and printing the results.
-(void)runWithHostName:(NSString *)hostName {
    _shouldStop = NO;
    assert(self.pinger == nil);

    self.pinger = [APPingModule pingModuleWithHostName:hostName];
    assert(self.pinger != nil);

    self.pinger.delegate = self;
    [self.pinger start];

    NSLog(@"Pinger is nil ? [%@]",self.pinger);
    do {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate distantFuture]];
    } while (self.pinger != nil);
}

-(void)saveString:(NSString *)stringToSave {
    [self.delegate outputToScreenWithLog:stringToSave];

}

// Called to send a ping, both directly (as soon as the PingModule object starts up)
// and via a timer (to continue sending pings periodically).
-(void)sendPing {
    NSLog(@"sendPing");
    if (_shouldStop) {
        [self stopRunningPing];
    } else {
        assert(self.pinger != nil);
        [self.pinger sendPingWithData:nil];
    }
}

-(NSString *)currentDate {
//    NSLog(@"Date>>>[%@]",[[NSDate date] descriptionWithLocale:[NSLocale currentLocale]]);
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
//    [formatter setDateFormat:@"yyyy-MM-dd 'at' HH:mm:ss"];
    [formatter setDateFormat:@"HH:mm:ss"];

    NSString *currentDate = [formatter stringFromDate:[NSDate date]];

    return currentDate;
}

// A PingModule delegate callback method.  We respond to the startup by sending a
// ping immediately and starting a timer to continue sending them every second.
-(void)pingModule:(APPingModule *)pinger didStartWithAddress:(NSData *)address {
#pragma unused(pinger)
    assert(pinger == self.pinger);
    assert(address != nil);

    NSString *pingingAddress = [NSString stringWithFormat:@"%@ pinging:[%@]\n",[self currentDate],DisplayAddressForAddress(address)];
    NSLog(@"%@", pingingAddress);
    [self saveString:pingingAddress];


    // Send the first ping straight away.

    [self sendPing];

    // And start a timer to send the subsequent pings.

    assert(self.sendTimer == nil);
    self.sendTimer = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(sendPing) userInfo:nil repeats:YES];
}

// A PingModule delegate callback method.  We shut down our timer and the
// PingModule object itself, which causes the runloop code to exit.
-(void)pingModule:(APPingModule *)pinger didFailWithError:(NSError *)error {
#pragma unused(pinger)
    assert(pinger == self.pinger);
#pragma unused(error)
    NSString *info = [NSString stringWithFormat:@"%@\t\tfailed: %@\n",[self currentDate],[self shortErrorFromError:error]];
    NSLog(@"%@", info);
    [self saveString:info];

    [self.sendTimer invalidate];
    self.sendTimer = nil;

    // No need to call -stop.  The pinger will stop itself in this case.
    // We do however want to nil out pinger so that the runloop stops.

    self.pinger = nil;
}

// A PingModule delegate callback method.  We just log the send.
-(void)pingModule:(APPingModule *)pinger didSendPacket:(NSData *)packet {
#pragma unused(pinger)
    assert(pinger == self.pinger);
#pragma unused(packet)
    NSString *info = [NSString stringWithFormat:@"%@\t#%u\tsent\n",[self currentDate],(unsigned int) OSSwapBigToHostInt16(((const ICMPHeader *) [packet bytes])->sequenceNumber)];
    NSLog(@"%@",  info);
    [self saveString:info];
}

// A PingModule delegate callback method.  We just log the failure.
-(void)pingModule:(APPingModule *)pinger didFailToSendPacket:(NSData *)packet error:(NSError *)error {
#pragma unused(pinger)
    assert(pinger == self.pinger);
#pragma unused(packet)
#pragma unused(error)
    NSString *info = [NSString stringWithFormat:@"%@\t#%u\tsend failed: %@\n",[self currentDate],(unsigned int) OSSwapBigToHostInt16(((const ICMPHeader *) [packet bytes])->sequenceNumber), [self shortErrorFromError:error]];
    NSLog(@"%@",  info);
    [self saveString:info];
//    NSLog(@"#%u send failed: %@", (unsigned int) OSSwapBigToHostInt16(((const ICMPHeader *) [packet bytes])->sequenceNumber), [self shortErrorFromError:error]);
}

// A PingModule delegate callback method.  We just log the reception of a ping response.
-(void)pingModule:(APPingModule *)pinger didReceivePingResponsePacket:(NSData *)packet {
#pragma unused(pinger)
    assert(pinger == self.pinger);
#pragma unused(packet)
    NSString *info = [NSString stringWithFormat:@"%@\t#%u\treceived\n",[self currentDate], (unsigned int) OSSwapBigToHostInt16([APPingModule icmpInPacket:packet]->sequenceNumber)];
    NSLog(@"%@",  info);
    [self saveString:info];
//    NSLog(@"#%u received", (unsigned int) OSSwapBigToHostInt16([PingModule icmpInPacket:packet]->sequenceNumber) );
}

// A PingModule delegate callback method.  We just log the receive.
-(void)pingModule:(APPingModule *)pinger didReceiveUnexpectedPacket:(NSData *)packet {
    const ICMPHeader *  icmpPtr;

#pragma unused(pinger)
    assert(pinger == self.pinger);
#pragma unused(packet)

    icmpPtr = [APPingModule icmpInPacket:packet];
    NSString *info;
    if (icmpPtr != NULL) {
        info = [NSString stringWithFormat:@"%@\t#%u\tunexpected ICMP type=%u, code=%u, identifier=%u\n", [self currentDate],(unsigned int) OSSwapBigToHostInt16(icmpPtr->sequenceNumber), (unsigned int) icmpPtr->type, (unsigned int) icmpPtr->code, (unsigned int) OSSwapBigToHostInt16(icmpPtr->identifier)];
        NSLog(@"%@",  info);
//        NSLog(@"#%u unexpected ICMP type=%u, code=%u, identifier=%u", (unsigned int) OSSwapBigToHostInt16(icmpPtr->sequenceNumber), (unsigned int) icmpPtr->type, (unsigned int) icmpPtr->code, (unsigned int) OSSwapBigToHostInt16(icmpPtr->identifier) );
    } else {

        info = [NSString stringWithFormat:@"%@\t\tunexpected packet size=%zu\n", [self currentDate],(size_t) [packet length]];
        NSLog(@"%@",  info);
//        NSLog(@"unexpected packet size=%zu", (size_t) [packet length]);
    }
    [self saveString:info];
}


@end
