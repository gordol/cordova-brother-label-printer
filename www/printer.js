var BrotherPrinter = function () {}
BrotherPrinter.prototype = {
  printLabel: function (data, options, successCallback, errorCallback) {
    if (!data || !data.length) {
      console.log('No data passed in. Expects a bitmap.')
      return;
    }

    if (!options || !options.numberOfCopies || !options.orientation || !options.labelName || !options.modelName || !options.port) {
      console.log('Invalid print options... Options should be for instance in the format: {"numberOfCopies":1, "orientation":"LANDSCAPE", "labelName":"W62RB", "modelName":"QL-820NBW", "port":"NET"}.')
      return;
    }

    cordova.exec(successCallback, errorCallback, 'BrotherPrinter', 'printLabel', [data, options])
  },

}
var plugin = new BrotherPrinter()
module.exports = plugin
