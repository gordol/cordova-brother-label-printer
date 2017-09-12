// handleError is meant to translate errors returned from the Plugin side to the javascript
// side.
function handleError(callback) {
    return function(obj) {
        if (!obj) {
            var err = new Error('No Error recieved, possible error in transmission');
            callback(err);
            return;
        }

        var namespace = obj.namespace || 'unknown namespace';
        var code = obj.code || 'unknown code';
        var message = obj.message || 'unknown message';

        var err = new Error(message);
        err.code = code;
        err.namespace = namespace;
        callback(err);
    }
}

var BrotherPrinter = function () {}
BrotherPrinter.prototype = {
    findNetworkPrinters: function (onSuccess, onError) {
        cordova.exec(onSuccess, handleError(onError), 'BrotherPrinter', 'findNetworkPrinters', []);
    },

    pairBluetoothPrinters: function(onSuccess, onError) {
        cordova.exec(onSuccess, handleError(onError), 'BrotherPrinter', 'pairBluetoothPrinters', []);
    },

    findBluetoothPrinters: function (onSuccess, onError) {
        cordova.exec(onSuccess, handleError(onError), 'BrotherPrinter', 'findBluetoothPrinters', []);
    },

    findPrinters: function (onSuccess, onError) {
        cordova.exec(onSuccess, handleError(onError), 'BrotherPrinter', 'findPrinters', []);
    },

    setPrinter: function (printer, onSuccess, onError) {
        cordova.exec(onSuccess, handleError(onError), 'BrotherPrinter', 'setPrinter', [printer]);
    },

    printViaSDK: function (data, onSuccess, onError) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects a bitmap.');
            onError(new Error('No data passed into \'printViaSDK\'. Expecting supported image data.'));
            return;
        }
        cordova.exec(onSuccess, handleError(onError), 'BrotherPrinter', 'printViaSDK', [data]);
    },

    sendUSBConfig: function (data, onSuccess, onError) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects print payload string.');
            onError(new Error('No data passed into \'sendUSBConfig\'. Expecting print payload string.'));
            return;
        }
        cordova.exec(onSuccess, handleError(onError), 'BrotherPrinter', 'sendUSBConfig', [data]);
    }
}
var plugin = new BrotherPrinter()
module.exports = plugin
