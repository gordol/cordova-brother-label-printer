//
//  BRPtouchPrinter.h
//  BRPtouchPrinterKit
//
//  Created by BIL on 12/02/03.
//  Copyright (c) 2012 Brother Industries, Ltd. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>
#import <CoreGraphics/CoreGraphics.h>
#import <UIKit/UIKit.h>

#include "BRPtouchPrintInfo.h"
#include "BRPtouchPrinterData.h"

//  Cut Mode
#define FLAG_M_AUTOCUT  0x40
#define FLAG_M_MIRROR   0x80

//  拡張モード設定フラグ
#define FLAG_K_DRAFT    0x01
#define FLAG_K_HALFCUT  0x04
#define FLAG_K_NOCHAIN  0x08
#define FLAG_K_SPTAPE   0x10
#define FLAG_K_AFTERCUT 0x20
#define FLAG_K_HGPRINT  0x40
#define FLAG_K_COPY     0x80

//  Error Code
#define ERROR_NONE_          0
#define ERROR_TIMEOUT		-3
#define ERROR_BADPAPERRES	-4
#define ERROR_IMAGELARGE	-6
#define ERROR_CREATESTREAM	-7
#define ERROR_OPENSTREAM	-8
#define ERROR_FILENOTEXIST  -9
#define ERROR_PAGERANGEERROR  -10
#define ERROR_NOT_SAME_MODEL_ -11
#define ERROR_BROTHER_PRINTER_NOT_FOUND_ -12
#define ERROR_PAPER_EMPTY_ -13
#define ERROR_BATTERY_EMPTY_ -14
#define ERROR_COMMUNICATION_ERROR_ -15
#define ERROR_OVERHEAT_ -16
#define ERROR_PAPER_JAM_ -17
#define ERROR_HIGH_VOLTAGE_ADAPTER_ -18
#define ERROR_CHANGE_CASSETTE_ -19
#define ERROR_FEED_OR_CASSETTE_EMPTY_ -20
#define ERROR_SYSTEM_ERROR_ -21
#define ERROR_NO_CASSETTE_ -22
#define ERROR_WRONG_CASSENDTE_DIRECT_ -23
#define ERROR_CREATE_SOCKET_FAILED_ -24
#define ERROR_CONNECT_SOCKET_FAILED_ -25
#define ERROR_GET_OUTPUT_STREAM_FAILED_ -26
#define ERROR_GET_INPUT_STREAM_FAILED_ -27
#define ERROR_CLOSE_SOCKET_FAILED_ -28
#define ERROR_OUT_OF_MEMORY_ -29
#define ERROR_SET_OVER_MARGIN_ -30
#define ERROR_NO_SD_CARD_ -31
#define ERROR_FILE_NOT_SUPPORTED_ -32
#define ERROR_EVALUATION_TIMEUP_ -33
#define ERROR_WRONG_CUSTOM_INFO_ -34
#define ERROR_NO_ADDRESS_ -35
#define ERROR_NOT_MATCH_ADDRESS_ -36
#define ERROR_FILE_NOT_FOUND_ -37
#define ERROR_TEMPLATE_FILE_NOT_MATCH_MODEL_ -38
#define ERROR_TEMPLATE_NOT_TRANS_MODEL_ -39
#define ERROR_COVER_OPEN_ -40
#define ERROR_WRONG_LABEL_ -41
#define ERROR_PORT_NOT_SUPPORTED_ -42
#define ERROR_WRONG_TEMPLATE_KEY_ -43
#define ERROR_BUSY_ -44
#define ERROR_TEMPLATE_NOT_PRINT_MODEL_ -45
#define ERROR_CANCEL_ -46
#define ERROR_PRINTER_SETTING_NOT_SUPPORTED_ -47
#define ERROR_INVALID_PARAMETER_ -48
#define ERROR_INTERNAL_ERROR_ -49
#define ERROR_TEMPLATE_NOT_CONTROL_MODEL_ -50
#define ERROR_TEMPLATE_NOT_EXIST_ -51
#define ERROR_BADENCRYPT_ -52 // This does not occur in iOS
#define ERROR_BUFFER_FULL_ -53
#define ERROR_TUBE_EMPTY_ -54
#define ERROR_TUBE_RIBON_EMPTY_ -55
#define ERROR_UPDATE_FRIM_NOT_SUPPORTED_ -56 // This does not occur in iOS
#define ERROR_OS_VERSION_NOT_SUPPORTED_ -57 // This does not occur in iOS

//  Message value
#define MESSAGE_START_COMMUNICATION_ 1
#define MESSAGE_START_CONNECT_ 2
#define MESSAGE_END_CONNECTED_ 3
//#define MESSAGE_START_GET_OUTPUT_STREAM 4 // Not Available
//#define MESSAGE_END_GET_OUTPUT_STREAM 5 // Not Available
//#define MESSAGE_START_GET_INPUT_STREAM 6 // Not Available
//#define MESSAGE_END_GET_INPUT_STREAM 7 // Not Available
#define MESSAGE_START_SEND_STATUS_REQUEST_ 8
#define MESSAGE_END_SEND_STATUS_REQUEST_ 9
#define MESSAGE_START_READ_PRINTER_STATUS_ 10
#define MESSAGE_END_READ_PRINTER_STATUS_ 11
#define MESSAGE_START_CREATE_DATA_ 12
#define MESSAGE_END_CREATE_DATA_ 13
#define MESSAGE_START_SEND_DATA_ 14
#define MESSAGE_END_SEND_DATA_ 15
#define MESSAGE_START_SEND_TEMPLATE_ 16
#define MESSAGE_END_SEND_TEMPLATE_ 17
#define MESSAGE_START_SOCKET_CLOSE_ 18
#define MESSAGE_END_SOCKET_CLOSE_ 19
//#define MESSAGE_END_COMMUNICATION 20 // Not Available
#define MESSAGE_PRINT_COMPLETE_ 21
#define MESSAGE_PRINT_ERROR_ 22
#define MESSAGE_PAPER_EMPTY_ 23
#define MESSAGE_START_COOLING_ 24
#define MESSAGE_END_COOLING_ 25
//#define MESSAGE_PREPARATION 26 // Not Available
#define MESSAGE_WAIT_PEEL_ 27
#define MESSAGE_START_UPDATE_BLUETOOTH_SETTING_ 28
#define MESSAGE_END_UPDATE_BLUETOOTH_SETTING_ 29
#define MESSAGE_START_GET_BLUETOOTH_SETTING_ 30
#define MESSAGE_END_GET_BLUETOOTH_SETTING_ 31
#define MESSAGE_START_GET_TEMPLATE_LIST_ 32
#define MESSAGE_END_GET_TEMPLATE_LIST_ 33
#define MESSAGE_START_REMOVE_TEMPLATE_LIST_ 34
#define MESSAGE_END_REMOVE_TEMPLATE_LIST_ 35
#define MESSAGE_CANCEL_ 36

//  Return value
#define RET_FALSE       0
#define RET_TRUE        1

//
typedef struct _PTSTATUSINFO {
	Byte	byHead;						// Head mark
	Byte	bySize;						// Size
	Byte	byBrotherCode;				// Brother code
	Byte	bySeriesCode;				// Serial code
	Byte	byModelCode;				// Model code
	Byte	byNationCode;				// Nation code
	Byte	byFiller;					// information about cover
	Byte	byFiller2;					// Not used
	Byte	byErrorInf;					// Error information 1
	Byte	byErrorInf2;				// Error information 2
	Byte	byMediaWidth;				// Media width
	Byte	byMediaType;				// Media type
	Byte	byColorNum;					// The number of colors
	Byte	byFont;						// Font
	Byte	byJapanesFont;				// Japanese font
	Byte	byMode;						// Mode
	Byte	byDensity;					// Density
	Byte	byMediaLength;				// Media Length
	Byte	byStatusType;				// Status Type
	Byte	byPhaseType;				// Phase type
	Byte	byPhaseNoHi;				// Upper bytes of phase number
	Byte	byPhaseNoLow;				// Lower bytes of phase number
	Byte	byNoticeNo;					// Notice number
	Byte	byExtByteNum;				// Total bytes of extended part
    Byte	byLabelColor;				// Color of label
	Byte	byFontColor;				// Color of font
	Byte	byHardWareSetting[4];		// Settings of hardware
    Byte	byNoUse[2];                 // Not Use
} PTSTATUSINFO, *LPPTSTATUSINFO;

typedef enum {
    CONNECTION_TYPE_WLAN,
    CONNECTION_TYPE_BLUETOOTH,
    CONNECTION_ERROR
} CONNECTION_TYPE;

extern NSString *BRWLanConnectBytesWrittenNotification;
extern NSString *BRBluetoothSessionBytesWrittenNotification;
extern NSString *BRPtouchPrinterKitMessageNotification;

extern NSString *const BRBytesWrittenKey;
extern NSString *const BRBytesToWriteKey;
extern NSString *const BRMessageKey;


@interface BRPtouchPrinter : NSObject <NSNetServiceBrowserDelegate,NSNetServiceDelegate>

- (id)initWithPrinterName:(NSString*)strPrinterName;
- (id)initWithPrinterName:(NSString*)strPrinterName interface:(CONNECTION_TYPE)type;
- (BOOL)setPrinterName:(NSString*)strPrinterName;
- (void)setPrintInfo:(BRPtouchPrintInfo*)printInfo;
- (BOOL)setCustomPaperFile:(NSString*)strFilePath; // Not Available
//- (BOOL)setEncryptKey:(NSString*)strKey keyEx:(NSString*)strKeyEx; // Not Available

- (BOOL)isPrinterReady;
- (int)getPTStatus:(PTSTATUSINFO*)status;
- (NSString *)getFirmVersion;

- (BOOL)sendTemplateFile:(NSArray*)sendFileArray;
- (BOOL)sendFirmwareFile:(NSArray*)sendFileArray;

- (int)sendTemplate:(NSString *)sendtemplateFilePath connectionType:(CONNECTION_TYPE) type;

- (void)setIPAddress:(NSString*)strIP;
- (void)setupForBluetoothDeviceWithSerialNumber:(NSString*)serialNumber;

/**
 * Deprecated.
 * Use startCommunication.
 **/
- (int)startPrint __attribute__((deprecated));
- (BOOL)startCommunication;

/**
 * Deprecated.
 * Use endCommunication.
 **/
- (void)endPrint __attribute__((deprecated));
- (void)endCommunication;

- (int)sendFile:(NSString*)filePath;
- (int)sendData:(NSData*)data;
- (int)sendFileEx:(NSString*)filePath;
- (int)sendDataEx:(NSData*)data;

- (int)printPDFAtPath:(NSString *)pdfPath pages:(NSUInteger [])indexes length:(NSUInteger)length copy:(int)nCopy;
- (int)printImage:(CGImageRef)imageRef copy:(int)nCopy;

- (int)cancelPrinting;
- (void)setInterface:(CONNECTION_TYPE)strInterface;

- (int) setAutoConnectBluetooth:(BOOL)flag;
- (int) isAutoConnectBluetooth;

@end
