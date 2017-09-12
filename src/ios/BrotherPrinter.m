#import "BrotherPrinter.h"

const NSString *BPContextCallbackIdKey = @"CallbackId";
const NSString *BPContextImageKey = @"image";

@implementation BrotherPrinter {
    NSMutableArray *_brotherDeviceList;
    BRPtouchNetworkManager *_networkManager;
    BRPtouchBluetoothManager *_bluetoothManager;
    BRPtouchPrinter *_ptp;
    void (^_callback)(NSArray *, NSError *);

    NSArray* printerList;
    NSArray* supportedPrinterList;
}
@synthesize operationQueue = _operationQueue;

-(void)pluginInitialize {
    [super pluginInitialize];
    _operationQueue = [NSOperationQueue mainQueue]; // [[NSOperationQueue alloc] init];
    printerList = @[
        @"Brother RJ-4040",
        @"Brother RJ-3150",
        @"Brother RJ-3150Ai",
        @"Brother RJ-3050",
        @"Brother RJ-3050Ai",
        @"Brother QL-710W",
        @"Brother QL-720NW",
        @"Brother QL-810W",
        @"Brother QL-820NWB",
        @"Brother PT-E550W",
        @"Brother PT-P750W",
        @"Brother PT-D800W",
        @"Brother PT-E800W",
        @"Brother PT-E850TKW",
        @"Brother PT-P900W",
        @"Brother PT-P950NW",
        @"Brother TD-2120N",
        @"Brother TD-2130N",
        @"Brother PJ-673",
        @"Brother PJ-763",
        @"Brother PJ-773",
        @"Brother MW-145MF",
        @"Brother MW-260MF",
        @"Brother RJ-4030Ai",
        @"Brother RJ-2050",
        @"Brother RJ-2140",
        @"Brother RJ-2150"];

    supportedPrinterList = @[
        @"Brother QL-710W",
        @"Brother QL-720NW",
        @"Brother QL-810W",
        @"Brother QL-820NWB"];

}

- (NSArray *)suffixedPrinterList:(NSArray *)list {
    NSMutableArray *result = [NSMutableArray array];
    NSUInteger count = list.count;
    for (NSUInteger i = 0; i < count; i++) {
        [result addObject:[list[i] componentsSeparatedByString:@" "][1]];
    }

    return result;
}

-(void)convertDeviceList:(NSArray *)deviceList asType:(NSString*)type withCompletion:(void (^)(NSArray *, NSError *))completion {
    __block NSMutableArray *resultList = [NSMutableArray array];
    [deviceList enumerateObjectsUsingBlock:^(BRPtouchDeviceInfo *deviceInfo, NSUInteger idx, BOOL *stop) {
        NSMutableDictionary* dict = [NSMutableDictionary dictionary];
        if (deviceInfo.strIPAddress && ![@"" isEqualToString:deviceInfo.strIPAddress]) {
            dict[@"ipAddress"] = deviceInfo.strIPAddress;
        }

        if (deviceInfo.strMACAddress && ![@"" isEqualToString:deviceInfo.strMACAddress]) {
            dict[@"macAddress"] = deviceInfo.strMACAddress;
        }

        if(deviceInfo.strPrinterName && ![@"" isEqualToString:deviceInfo.strPrinterName]) {
            dict[@"name"] = deviceInfo.strPrinterName;
        }

        if (deviceInfo.strModelName && ![@"" isEqualToString:deviceInfo.strModelName]) {
            dict[@"modelName"] = deviceInfo.strModelName;
        }

        if (deviceInfo.strSerialNumber && ![@"" isEqualToString:deviceInfo.strSerialNumber]) {
            dict[@"serialNumber"] = deviceInfo.strSerialNumber;
        }

        dict[@"port"] = type;

        [resultList addObject:[dict copy]];
    }];

    completion(resultList, nil);
}

-(void)pairedDevicesWithCompletion:(void (^)(NSArray *result, NSError *error))completion {
    [self checkBluetoothManager];
    NSArray* pairedDevices = [_bluetoothManager pairedDevices];

    NSLog(@"Paired Devices: %@", pairedDevices);

    [self convertDeviceList:pairedDevices asType:@"BLUETOOTH" withCompletion:completion];
}


-(void)networkPrintersWithCompletion:(void (^)(NSArray *result, NSError *error))completion {
    [self checkNetworkManager];

    _callback = completion;

    [_networkManager setPrinterNames: supportedPrinterList];
    [_networkManager startSearch: 5.0];
}

#pragma mark - BRPtouchNetworkDelegate
-(void)didFinishSearch:(id)sender {
    NSArray* deviceList = [_networkManager getPrinterNetInfo];
    [self convertDeviceList:deviceList asType:@"NET" withCompletion:^(NSArray* resultList, NSError* error) {
        NSLog(@"resultList: %@", resultList);
        if (_callback) {
            _callback(resultList, nil);
            _callback = nil;
        }
    }];
}

#pragma mark - Plugin Commands
-(void)findNetworkPrinters:(CDVInvokedUrlCommand*)command {
    [self.commandDelegate runInBackground:^{
        [self networkPrintersWithCompletion:^(NSArray* networkPrinters, NSError *error) {
            if (error) {
                [self.commandDelegate
                     sendPluginResult:[self errorResult:@"findNetworkPrinters" withCode:1 withMessage:[NSString stringWithFormat:@"unable to find network printers: %@", [error localizedDescription]]]
                           callbackId:command.callbackId];
                return;
            }

            [self.commandDelegate
                 sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:networkPrinters]
                       callbackId:command.callbackId];

        }];
    }];
}

-(void)checkNetworkManager {
    if (!_networkManager) {
        _networkManager = [[BRPtouchNetworkManager alloc] init];
        _networkManager.delegate = self;
        _networkManager.isEnableIPv6Search = YES;
    }
}

-(void)checkBluetoothManager {
    if (!_bluetoothManager) {
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_accessoryDidConnect:) name:BRDeviceDidConnectNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_accessoryDidDisconnect:) name:BRDeviceDidDisconnectNotification object:nil];

        _bluetoothManager = [BRPtouchBluetoothManager sharedManager];
        [_bluetoothManager registerForBRDeviceNotifications];
    }
}

-(void)_accessoryDidDisconnect:(NSNotification *)notification
{
    BRPtouchDeviceInfo *disconnectedAccessory = [[notification userInfo] objectForKey:BRDeviceKey];
    NSLog(@"DisconnectedDevice:[%@]",[disconnectedAccessory description]);
}

-(void)_accessoryDidConnect:(NSNotification *)notification
{
    BRPtouchDeviceInfo *connectedAccessory = [[notification userInfo] objectForKey:BRDeviceKey];
    NSLog(@"ConnectedDevice:[%@]",[connectedAccessory description]);
}

-(void)pairBluetoothPrinters:(CDVInvokedUrlCommand*)command {
    [self checkBluetoothManager];

    NSPredicate *pred = [NSPredicate predicateWithBlock:^ BOOL (NSString *evaluatedObject, NSDictionary * bindings) {
        if (!evaluatedObject || ![evaluatedObject isKindOfClass: [NSString class]]) {
            return NO;
        }

        __block BOOL keep = NO;
        [[self suffixedPrinterList:supportedPrinterList] enumerateObjectsUsingBlock:^(NSString *printerName, NSUInteger index, BOOL *stop) {
            keep = keep || [evaluatedObject hasPrefix:printerName];
        }];

        return keep;
    }];

    [_bluetoothManager brShowBluetoothAccessoryPickerWithNameFilter:pred];
    [self.commandDelegate
        sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK]
              callbackId:command.callbackId];
}

-(void)findBluetoothPrinters:(CDVInvokedUrlCommand*)command {
    [self.commandDelegate runInBackground:^{
        [self checkBluetoothManager];
        [self pairedDevicesWithCompletion:^(NSArray* pairedDevices, NSError* error) {
            if (error != nil) {
                [self.commandDelegate
                    sendPluginResult:[self errorResult:@"findBluetoothPrinters" withCode:1 withMessage:[NSString stringWithFormat:@"unable to find network printers: %@", [error localizedDescription]]]
                          callbackId:command.callbackId];

                return;
            }

            [self.commandDelegate
                 sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:pairedDevices]
                       callbackId:command.callbackId];
        }];
     }];
}

-(void)findPrinters:(CDVInvokedUrlCommand*)command {
//    [self.commandDelegate runInBackground:^{
        [self checkNetworkManager];
        [self networkPrintersWithCompletion:^(NSArray* networkPrinters, NSError *error) {
            if (error) {
                [self.commandDelegate
                 sendPluginResult:[self errorResult:@"findPrinters" withCode:1 withMessage:[NSString stringWithFormat:@"unable to find network printers: %@", [error localizedDescription]]]
                 callbackId:command.callbackId];
                return;
            }

            [self pairedDevicesWithCompletion:^(NSArray* pairedDevices, NSError* error) {
                if (error != nil) {
                    [self.commandDelegate
                     sendPluginResult:[self errorResult:@"findPrinters" withCode:1 withMessage:[NSString stringWithFormat:@"unable to find bluetooth printers: %@", [error localizedDescription]]]
                     callbackId:command.callbackId];

                    return;
                }

                NSMutableArray* finalList = [[NSMutableArray alloc] initWithArray:networkPrinters];
                [finalList addObjectsFromArray:pairedDevices];

                [self.commandDelegate
                 sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:[finalList copy]]
                 callbackId:command.callbackId];
            }];

        }];

//    }];
    return;
}

-(void)setPrinter:(CDVInvokedUrlCommand*)command {
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    NSDictionary *obj = [command.arguments objectAtIndex:0];
    if (!obj) {
        [self.commandDelegate
            sendPluginResult:[self errorResult:@"setPrinter" withCode:1 withMessage:@"expected an object as the first argument."]
                  callbackId:command.callbackId];
        return;
    }

    NSString *ipAddress = obj[@"ipAddress"];
    NSString *modelName = obj[@"modelName"];
    NSString *serialNumber = obj[@"serialNumber"];
    if (!modelName) {
        [self.commandDelegate
            sendPluginResult:[self errorResult:@"setPrinter" withCode:2 withMessage:@"expected a \"modelName\" key in the given object."]
                  callbackId:command.callbackId];
        return;
    }
    [userDefaults
        setObject:modelName
           forKey:kSelectedDevice];

    NSString *port = obj[@"port"];
    if (!port) {
        [self.commandDelegate
            sendPluginResult:[self errorResult:@"setPrinter" withCode:2 withMessage:@"expected a \"port\" key in the given object."]
                  callbackId:command.callbackId];
        return;
    }

    if ([@"BLUETOOTH" isEqualToString:port]) {
        [userDefaults
            setObject:@"0"
               forKey:kIsWiFi];
        [userDefaults
            setObject:@"1"
               forKey:kIsBluetooth];
    }

    if ([@"NET" isEqualToString:port]) {
        [userDefaults
            setObject:@"1"
               forKey:kIsWiFi];
        [userDefaults
            setObject:@"0"
               forKey:kIsBluetooth];
    }

    [userDefaults
        setObject:ipAddress
           forKey:kIPAddress];

    [userDefaults
        setObject:serialNumber
           forKey:kSerialNumber];

    [userDefaults synchronize];

    // Send okay
    [self.commandDelegate
        sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK]
              callbackId:command.callbackId];
}

-(NSInteger)integerValueFromDefaults:(NSUserDefaults *)userDefaults forKey:(NSString *)key withFallback:(NSInteger)fallback {
    if ([userDefaults objectForKey:key] == nil) {
        return fallback;
    }

    return [userDefaults integerForKey:key];
}

-(NSString *)stringValueFromDefaults:(NSUserDefaults *)userDefaults forKey:(NSString *)key withFallback:(NSString *)fallback {
    NSString *str = [userDefaults stringForKey:key];
    if (str == nil) {
        return fallback;
    }

    return str;
}

-(double)doubleValueFromDefaults:(NSUserDefaults *)userDefaults forKey:(NSString *)key withFallback:(double)fallback {
    if ([userDefaults objectForKey:key] == nil) {
        return fallback;
    }

    return [userDefaults doubleForKey:key];
}

-(void)printViaSDK:(CDVInvokedUrlCommand*)command {
    NSString* base64Data = [command.arguments objectAtIndex:0];
    if (base64Data == nil) {
        [self.commandDelegate
            sendPluginResult:[self errorResult:@"printViaSDK" withCode:1 withMessage:@"expected a string as the first argument."]
                  callbackId:command.callbackId];
        return;
    }

    NSData *imageData              = [[NSData alloc] initWithBase64EncodedString:base64Data options:NSDataBase64DecodingIgnoreUnknownCharacters];
    UIImage *image                 = [[UIImage alloc] initWithData:imageData];

    NSUserDefaults *userDefaults   = [NSUserDefaults standardUserDefaults];

    NSString *selectedDevice       = [userDefaults stringForKey:kSelectedDevice];

    NSString *ipAddress            = [userDefaults stringForKey:kIPAddress];
    NSString *serialNumber         = [userDefaults stringForKey:kSerialNumber];

    // Set the Print Info
    // PrintInfo
    BRPtouchPrintInfo *printInfo   = [[BRPtouchPrintInfo alloc] init];

    NSString *numPaper             = [self stringValueFromDefaults:userDefaults forKey:kPrintNumberOfPaperKey withFallback:@"1"]; // Item 1

    printInfo.strPaperName         = [self stringValueFromDefaults:userDefaults forKey:kPrintNumberOfPaperKey withFallback:@"62mm"]; // Item 2
    printInfo.nOrientation         = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintOrientationKey withFallback:Landscape]; // Item 3
    printInfo.nPrintMode           = (int)[self integerValueFromDefaults:userDefaults forKey:kScalingModeKey withFallback:Fit]; // Item 4
    printInfo.scaleValue           = [self doubleValueFromDefaults:userDefaults forKey:kScalingFactorKey withFallback:1.0]; // Item 5

    printInfo.nHalftone            = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintHalftoneKey withFallback:Dither]; // Item 6
    printInfo.nHorizontalAlign     = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintHorizintalAlignKey withFallback:Left]; // Item 7
    printInfo.nVerticalAlign       = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintVerticalAlignKey withFallback:Top]; // Item 8
    printInfo.nPaperAlign          = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintPaperAlignKey withFallback:PaperLeft]; // Item 9

    printInfo.nExtFlag            |= (int)[self integerValueFromDefaults:userDefaults forKey:kPrintCodeKey withFallback:CodeOff]; // Item 10
    printInfo.nExtFlag            |= (int)[self integerValueFromDefaults:userDefaults forKey:kPrintCarbonKey withFallback:CarbonOff]; // Item 11
    printInfo.nExtFlag            |= (int)[self integerValueFromDefaults:userDefaults forKey:kPrintDashKey withFallback:DashOff]; // Item 12
    printInfo.nExtFlag            |= (int)[self integerValueFromDefaults:userDefaults forKey:kPrintFeedModeKey withFallback:FixPage]; // Item 13

    printInfo.nRollPrinterCase     = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintCurlModeKey withFallback:CurlModeOff]; // Item 14
    printInfo.nSpeed               = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintSpeedKey withFallback:Fast]; // Item 15
    printInfo.bBidirection         = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintBidirectionKey withFallback:BidirectionOff]; // Item 16

    printInfo.nCustomFeed          = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintFeedMarginKey withFallback:0]; // Item 17
    printInfo.nCustomLength        = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintCustomLengthKey withFallback:0]; // Item 18
    printInfo.nCustomWidth         = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintCustomWidthKey withFallback:0]; // Item 19

    printInfo.nAutoCutFlag        |= (int)[self integerValueFromDefaults:userDefaults forKey:kPrintAutoCutKey withFallback:AutoCutOn]; // Item 20
    printInfo.bEndcut              = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintCutAtEndKey withFallback:CutAtEndOn]; // Item 21
    printInfo.bHalfCut             = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintHalfCutKey withFallback:HalfCutOff]; // Item 22
    printInfo.bSpecialTape         = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintSpecialTapeKey withFallback:SpecialTapeOff]; // Item 23
    printInfo.bRotate180           = (int)[self integerValueFromDefaults:userDefaults forKey:kRotateKey withFallback:RotateOff]; // Item 24
    printInfo.bPeel                = (int)[self integerValueFromDefaults:userDefaults forKey:kPeelKey withFallback:PeelOff]; // Item 25

    NSString *customPaper          = [self stringValueFromDefaults:userDefaults forKey:kPrintCustomPaperKey withFallback:@""]; // Item 26
    NSString *customPaperFilePath  = nil;

    printInfo.bCutMark             = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintCutMarkKey withFallback:CutMarkOff]; // Item 27
    printInfo.nLabelMargine        = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintLabelMargineKey withFallback:0]; // Item 28

    if ([selectedDevice rangeOfString:@"RJ-"].location != NSNotFound ||
        [selectedDevice rangeOfString:@"TD-"].location != NSNotFound) {
        printInfo.nDensity         = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintDensityMax5Key withFallback:DensityMax5Level1]; // Item 29
    }
    else if([selectedDevice rangeOfString:@"PJ-"].location != NSNotFound){
        printInfo.nDensity         = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintDensityMax10Key withFallback:DensityMax10Level5]; // Item 30
    }
    else {
        // Error
        printInfo.nDensity         = (int)[self integerValueFromDefaults:userDefaults forKey:@"density" withFallback:DensityMax5Level0];
    }

    printInfo.nTopMargin           = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintTopMarginKey withFallback:0]; // Item 31
    printInfo.nLeftMargin          = (int)[self integerValueFromDefaults:userDefaults forKey:kPrintLeftMarginKey withFallback:0]; // Item 32

    NSInteger isWifi      = [userDefaults integerForKey:kIsWiFi];
    NSInteger isBluetooth = [userDefaults integerForKey:kIsBluetooth];

    if (isBluetooth == 1) {
        [self checkBluetoothManager];
        __block NSString *finalDeviceName = nil;
        [[self suffixedPrinterList:supportedPrinterList] enumerateObjectsUsingBlock:^(NSString *printerName, NSUInteger index, BOOL *stop) {
            if([selectedDevice hasPrefix:printerName]) {
                finalDeviceName = [NSString stringWithFormat:@"Brother %@", printerName];
            }
        }];

        _ptp = [[BRPtouchPrinter alloc] initWithPrinterName:finalDeviceName interface:CONNECTION_TYPE_BLUETOOTH];

    } else if (isWifi == 1) {
        _ptp = [[BRPtouchPrinter alloc] initWithPrinterName:selectedDevice interface:CONNECTION_TYPE_WLAN];
    } else {
        _ptp = nil;
    }

    if (!_ptp) {
        // oh noes!
        [self.commandDelegate
         sendPluginResult:[self errorResult:@"printViaSDK" withCode:2 withMessage:@"no printer could be determined."]
                  callbackId:command.callbackId];
        return;
    }

    NSDictionary* context = @{
                              BPContextCallbackIdKey:command.callbackId,
                              BPContextImageKey:image
                              };
    NSOperation *operation = nil;
    if (isBluetooth == 1) {
        [self checkBluetoothManager];
        BRBluetoothPrintOperation *bluetoothPrintOperation = [[BRBluetoothPrintOperation alloc]
                           initWithOperation:_ptp
                                   printInfo:printInfo
                                      imgRef:[image CGImage]
                               numberOfPaper:[numPaper intValue]
                                serialNumber:serialNumber
                                    withDict:context];

        [bluetoothPrintOperation addObserver:self
                     forKeyPath:@"isFinishedForBT"
                        options:NSKeyValueObservingOptionNew
                        context:nil];

        [bluetoothPrintOperation addObserver:self
                    forKeyPath:@"communicationResultForBT"
                       options:NSKeyValueObservingOptionNew
                                     context:nil];

        operation = bluetoothPrintOperation;

    } else if (isWifi == 1) {
        BRWLANPrintOperation *wlanPrintOperation = [[BRWLANPrintOperation alloc]
                        initWithOperation:_ptp
                                 printInfo:printInfo
                                    imgRef:[image CGImage]
                             numberOfPaper:[numPaper intValue]
                                 ipAddress:ipAddress
                                  withDict:context];

        [wlanPrintOperation addObserver:self
                    forKeyPath:@"isFinishedForWLAN"
                       options:NSKeyValueObservingOptionNew
                                context:nil];

        [wlanPrintOperation addObserver:self
                    forKeyPath:@"communicationResultForWLAN"
                       options:NSKeyValueObservingOptionNew
                                context:nil];

        operation = wlanPrintOperation;

    } else {

    }

    if (!operation) {
        return;
    }

    [_operationQueue addOperation:operation];
}


#pragma mark - Observers

-(void)finishedForWLAN:(NSOperation *)operation withCallbackId:(NSString *)callbackId {
    [operation removeObserver:self forKeyPath:@"isFinishedForWLAN"];
    [operation removeObserver:self forKeyPath:@"communicationResultForWLAN"];

    BRWLANPrintOperation *wlanOperation = (BRWLANPrintOperation *) operation;
    PTSTATUSINFO resultStatus = wlanOperation.resultStatus;

    if (wlanOperation.errorCode != ERROR_NONE_) {
        CDVPluginResult *result = [self errorResult:@"WLAN" withCode:wlanOperation.errorCode withMessage:[self errorMessageFromStatusInfo:wlanOperation.errorCode]];

        [self.commandDelegate
            sendPluginResult:result
                  callbackId:callbackId];

        return;
    }

    [self.commandDelegate
        sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK]
              callbackId:callbackId];
}

-(void)finishedForBT:(NSOperation *)operation withCallbackId:(NSString *)callbackId {
    [operation removeObserver:self forKeyPath:@"isFinishedForBT"];
    [operation removeObserver:self forKeyPath:@"communicationResultForBT"];

    BRWLANPrintOperation *wlanOperation = (BRWLANPrintOperation *) operation;
    PTSTATUSINFO resultStatus = wlanOperation.resultStatus;

    if (wlanOperation.errorCode != ERROR_NONE_) {
        CDVPluginResult *result = [self errorResult:@"Bluetooth" withCode:wlanOperation.errorCode withMessage:[self errorMessageFromStatusInfo:wlanOperation.errorCode]];

        [self.commandDelegate
            sendPluginResult:result
                  callbackId:callbackId];

        return;
    }

    [self.commandDelegate
        sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK]
              callbackId:callbackId];
}

-(void)communicationResultForWLAN:(NSOperation *)operation withCallbackId:(NSString *)callbackId {
    BRWLANPrintOperation *wlanOperation = (BRWLANPrintOperation *) operation;
    BOOL result = wlanOperation.communicationResultForWLAN;
    if (!result) {
        [operation removeObserver:self forKeyPath:@"isFinishedForWLAN"];
        [operation removeObserver:self forKeyPath:@"communicationResultForWLAN"];
        CDVPluginResult *result = [self errorResult:@"WLAN" withCode:1 withMessage:@"unable to connect with device."];

        [self.commandDelegate
            sendPluginResult:result
                  callbackId:callbackId];
    }
}

-(void)communicationResultForBT:(NSOperation *)operation withCallbackId:(NSString *)callbackId {
    BRBluetoothPrintOperation *bluetoothOperation = (BRBluetoothPrintOperation *) operation;
    BOOL result = bluetoothOperation.communicationResultForBT;

    if (!result) {
        [operation removeObserver:self forKeyPath:@"isFinishedForBT"];
        [operation removeObserver:self forKeyPath:@"communicationResultForBT"];
        CDVPluginResult *result = [self errorResult:@"Bluetooth" withCode:1 withMessage:@"unable to connect with device."];

        [self.commandDelegate
            sendPluginResult:result
                  callbackId:callbackId];
    }
}

-(void)observeValueForKeyPath:(NSString *)keyPath
                     ofObject:(id)object
                       change:(NSDictionary *)change
                     context:(void *)context {

    NSOperation *operation = (NSOperation *)object;
    if ([@"isFinishedForWLAN" isEqualToString:keyPath]) {
        NSString *callbackId = ((BRWLANPrintOperation *)operation).dict[BPContextCallbackIdKey];
        [self finishedForWLAN:operation withCallbackId:callbackId];
    } else if ([@"isFinishedForBT" isEqualToString:keyPath]) {
        NSString *callbackId = ((BRBluetoothPrintOperation *)operation).dict[BPContextCallbackIdKey];
        [self finishedForBT:operation withCallbackId:callbackId];
    } else if ([@"communicationResultForWLAN" isEqualToString:keyPath]) {
        NSString *callbackId = ((BRWLANPrintOperation *)operation).dict[BPContextCallbackIdKey];
        [self communicationResultForWLAN:operation withCallbackId:callbackId];
    } else if ([@"communicationResultForBT" isEqualToString:keyPath]) {
        NSString *callbackId = ((BRBluetoothPrintOperation *)operation).dict[BPContextCallbackIdKey];
        [self communicationResultForBT:operation withCallbackId:callbackId];
    } else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
}

#pragma mark - Error Handler
-(CDVPluginResult *)errorResult:(NSString *)namespace withCode:(NSInteger)code withMessage:(NSString *)message {
    return [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{@"code":[NSNumber numberWithInteger:code], @"message":message, @"namespace":namespace}];
}

-(NSString *)errorMessageFromStatusInfo:(NSInteger)statusInfo {
    switch (statusInfo) {
        case ERROR_NONE_:
            return @"none";

        case ERROR_TIMEOUT:
            return @"timeout";

        case ERROR_BADPAPERRES:
            return @"";

        case ERROR_IMAGELARGE:
            return @"image too large.";

        case ERROR_CREATESTREAM:
            return @"failed to create a stream.";

        case ERROR_OPENSTREAM:
            return @"failed to open a stream.";

        case ERROR_FILENOTEXIST:
            return @"specified file does not exist.";

        case ERROR_PAGERANGEERROR:
            return @"specified page range exceeds the maximum.";

        case ERROR_NOT_SAME_MODEL_:
            return @"different printer model.";

        case ERROR_BROTHER_PRINTER_NOT_FOUND_:
            return @"no brother printer can be found.";

        case ERROR_PAPER_EMPTY_:
            return @"no paper";

        case ERROR_BATTERY_EMPTY_:
            return @"insufficient battery level.";

        case ERROR_COMMUNICATION_ERROR_:
            return @"failed to retrieve printer status.";

        case ERROR_OVERHEAT_:
            return @"overheating error.";

        case ERROR_PAPER_JAM_:
            return @"paper jam.";

        case ERROR_HIGH_VOLTAGE_ADAPTER_:
            return @"high-voltage adapter.";

        case ERROR_CHANGE_CASSETTE_:
            return @"replace paper cassette while printing.";

        case ERROR_FEED_OR_CASSETTE_EMPTY_:
            return @"feeding error, or end of paper.";

        case ERROR_SYSTEM_ERROR_:
            return @"system error";

        case ERROR_NO_CASSETTE_:
            return @"no paper cassette during a printing operation.";

        case ERROR_WRONG_CASSENDTE_DIRECT_:
            return @"wrong cassendte direct";

        case ERROR_CREATE_SOCKET_FAILED_:
            return @"failed to create socket.";

        case ERROR_CONNECT_SOCKET_FAILED_:
            return @"failed to connect.";

        case ERROR_GET_OUTPUT_STREAM_FAILED_:
            return @"failed to retrieve output stream.";

        case ERROR_GET_INPUT_STREAM_FAILED_:
            return @"failed to retrieve input stream.";

        case ERROR_CLOSE_SOCKET_FAILED_:
            return @"failed to close socket.";

        case ERROR_OUT_OF_MEMORY_:
            return @"insufficient memory";

        case ERROR_SET_OVER_MARGIN_:
            return @"exceeded the specified margin.";

        case ERROR_NO_SD_CARD_:
            return @"no SD card.";

        case ERROR_FILE_NOT_SUPPORTED_:
            return @"unsupported filed.";

        case ERROR_EVALUATION_TIMEUP_:
            return @"trial period for PDF printing has ended.";

        case ERROR_WRONG_CUSTOM_INFO_:
            return @"incorrect custom paper settings.";

        case ERROR_NO_ADDRESS_:
            return @"IP address and MAC address are not specified.";

        case ERROR_NOT_MATCH_ADDRESS_:
            return @"IP address or MAC address don't match.";

        case ERROR_FILE_NOT_FOUND_:
            return @"filed does not exist.";

        case ERROR_TEMPLATE_FILE_NOT_MATCH_MODEL_:
            return @"model incompatible with template transfer.";

        case ERROR_TEMPLATE_NOT_TRANS_MODEL_:
            return @"model incompatible with template transfer.";

        case ERROR_COVER_OPEN_:
            return @"cover open.";

        case ERROR_WRONG_LABEL_:
            return @"incorrect roll type.";

        case ERROR_PORT_NOT_SUPPORTED_:
            return @"unsupported interface.";

        case ERROR_WRONG_TEMPLATE_KEY_:
            return @"a template with the specified key does not exist.";

        case ERROR_BUSY_:
            return @"busy";

        case ERROR_TEMPLATE_NOT_PRINT_MODEL_:
            return @"model incompatible with p-touch template printing.";

        case ERROR_CANCEL_:
            return @"cancelled";

        case ERROR_PRINTER_SETTING_NOT_SUPPORTED_:
            return @"device settings function not supported.";

        case ERROR_INVALID_PARAMETER_:
            return @"invalid parameter.";

        case ERROR_INTERNAL_ERROR_:
            return @"internal device error.";

        case ERROR_TEMPLATE_NOT_CONTROL_MODEL_:
            return @"model incompatible with template deleting and retrieving.";

        case ERROR_TEMPLATE_NOT_EXIST_:
            return @"template does not exist.";

        case ERROR_BUFFER_FULL_:
            return @"buffer full.";

        case ERROR_TUBE_EMPTY_:
            return @"tube empty.";

        case ERROR_TUBE_RIBON_EMPTY_:
            return @"tube ribbon empty.";

        case ERROR_BADENCRYPT_: // This does not occur in iOS
        case ERROR_UPDATE_FRIM_NOT_SUPPORTED_: // This does not occur in iOS
        case ERROR_OS_VERSION_NOT_SUPPORTED_: // This does not occur in iOS
        default:
            return @"unknown error.";
    }
}

@end
