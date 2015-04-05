var mongojs = require('mongojs');
var db = mongojs.connect('dismu:distributedp0w3r@ds031329.mongolab.com:31329/dismu', ['reports']);
var console = require('console');

exports.createCrashReport = function(report, callback) {
    db.reports.insert({report: report}, {}, callback);
}

