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

