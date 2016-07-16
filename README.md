# Cordova Brother Label Printer Plugin

Uses the Brother Print SDK for Android...

More info can be found here, including a list of compatible printers: http://www.brother.com/product/dev/mobile/android/index.htm

Already bundled is the following version: v3.0.4 (5/18/2016) which is in the `src/android/libs` dir. By downloading this you agree to the Brother SDK License terms which are included in the README under the libs dir.

## Target mobile printers:
```
PocketJet PJ-722, PJ-723, PJ-762, PJ-763, PJ-763MFi, PJ-773
PocketJet PJ-622, PJ-623, PJ-662, PJ-663
PocketJet PJ-520, PJ-522, PJ-523, PJ-560, PJ-562, PJ-563
MPrint MW-145MFi, MW-260MFi
MPrint MW-140BT, MW-145BT, MW-260
RJ-4030Ai, RJ-4030, RJ-4040
TD-2020, TD-2120N, TD-2130N, TD-4000, TD-4100N
QL-710W, QL-720NW
PT-E550W, PT-P750W
RJ-3050, RJ-3150
PT-E800W, PT-D800W, PT-E850TKW
PT-P900W, PT-P950NW
```

__Tested models:__ `QL-720NW`

__NOTE:__ Currently, you will need to adjust the `modelName` variable in `src/android/BrotherPrinter.java`. It is the first variable in the `BrotherPrinter` class. This could be extended to be configured through config.xml or via a JS call, but it's currently hard-coded. Feel free to send a pull request to make the configuration more extensible... 


## Supported interfaces (by this plugin):
```
Wi-Fi (Infrastructure mode)
USB
```

_The SDK also has Bluetooth support, but this is not integrated currently. Pull requests are welcomed..._

## Usage

See here for JS interfaces to the plugin: `www/printer.js`

There are three available methods... 

* findNetworkPrinters
* printViaSDK
* sendUSBConfig

__findNetworkPrinters__ must be called before printViaSDK. It takes no parameters and returns two parameters, one is a boolean (whether a printer was found or not), and the other is a list of found printers. To print, all that's needed are the IP and MAC of the target printer.

Currently, the last printer that is found will be the one targetted due to the way we're looping over the `netPrinters` array. This plugin could be extended to allow the user to select which printer they want to connect with... If this is desired, let me know, and I'll address when I get a chance, or better yet, send a pull request. The best way would either be to pass the printer IP/MAC to the printViaSDK method, or perhaps you could just pass an index to select the desired printer from the `netPrinters` list.

__printViaSDK__ takes one parameter, which is a base64 encoded bitmap image. The result should be a status code that is passed directly from the SDK. The status codes are documnted in the Brother SDK Appendix in section 4.2.2.5.Error Code. If everything works, the response should be "ERROR_NONE".

__sendUSBConfig__ calls the Brother SDK's `printFile` method. The expected input is a string containing raw print commands, which is written to a temporary file in the app cache directory, and is then sent to the `printFile` method and deleted afterwards. You will need a device that supports USB-OTG and a USB-OTG cable. On first run the app will request USB permissions, and it should be saved after that for subsequent prints. You can use this method ot print files directly, or you can use it to send raw commands in PCL (Printer Control Language) to the printer... For example, to configure the network settings of the printer, etc... You will need to reach out to Brother for documentation of the PCL commands. You can probably find them by searching for "[Brother Printer Command Reference](https://duckduckgo.com/?q=Brother+Printer+Command+Reference)" and appending your model number.
