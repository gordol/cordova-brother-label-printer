var BrotherPrinter = function () {}
BrotherPrinter.prototype = {
    findNetworkPrinters: function (callback, scope) {
        var callbackFn = function () {
            var args = typeof arguments[0] == 'boolean' ? arguments : arguments[0]
            callback.apply(scope || window, args)
        }
        cordova.exec(callbackFn, null, 'BrotherPrinter', 'findNetworkPrinters', [])
    },
    setSessionPrinter: function (data, callback) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects a string representing the serial number.')
            return
        }
        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'setSessionPrinter', [data])
    },
    printPdf: function( options, callback ){

        if( !options || options.file === undefined ){
          console.log("No path for the pdf was specified");
          return;
        }

        var args = [options.file];
        if( options.printer !== undefined ){
          args.push("printer:" + options.printer);
        }
        if( options.paper !== undefined ){
          args.push("paper:" + options.paper);
        }

        cordova.exec(callback, function(err){ console.log('error: '+err)}, 'BrotherPrinter', 'printPdf', args);
    },
    printBitmapImage: function (options, callback) {

        if (!options || options.image === undefined) {
            console.log('No data passed in. Expects a bitmap (base64).')
            return
        }

        var args = [options.image];
        if( options.printer !== undefined ){
          args.push("printer:" + options.printer);
        }
        if( options.paper !== undefined ){
          args.push("paper:" + options.paper);
        }

        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'printViaSDK', args)

    },
    printViaSDK: function (data, callback) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects a bitmap.')
            return
        }
        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'printViaSDK', [data])
    },
    sendUSBConfig: function (data, callback) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects print payload string.')
            return
        }
        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'sendUSBConfig', [data])
    }
}
var plugin = new BrotherPrinter()
module.exports = plugin
