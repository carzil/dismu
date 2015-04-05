var mongojs = require('mongojs');
var db = mongojs.connect('dismu:distributedp0w3r@ds031329.mongolab.com:31329/dismu', ['sessions']);
var crypto = require('crypto');
var console = require('console');

function generateSessionId(username) {
    var hash = crypto.createHash('md5');
    hash.update(username + Date.now());
    return hash.digest('hex');
}

function generateSessionSecret(sessionId) {
    var hash = crypto.createHash('md5');
    hash.update(sessionId + Date.now());
    return hash.digest('hex');
}

exports.createSession = function(username, deviceInfo, remoteAddress, callback) {
    var sessionId = generateSessionId(username);
    var sessionSecret = generateSessionSecret(sessionId);
    db.sessions.insert({sessionId: sessionId, sessionSecret: sessionSecret, username: username, deviceInfo: deviceInfo, remoteAddress: remoteAddress}, {}, callback);
}

exports.removeSession = function(sessionId, callback) {
    db.sessions.remove({sessionId:sessionId}, false, callback);
}

exports.getSession = function(sessionId, callback) {
    db.sessions.findOne({sessionId: sessionId}, callback);
}
