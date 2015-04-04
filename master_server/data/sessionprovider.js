var mongojs = require('mongojs');
var db = mongojs.connect('dismu:distributedp0w3r@ds031329.mongolab.com:31329/dismu', ['sessions']);
var crypto = require('crypto');
var console = require('console');

function generateSessionId(username) {
    var hash = crypto.createHash('md5');
    hash.update(username + Date.now());
    return hash.digest('hex');
}

function generateSessionSecret(sessioId) {
    var hash = crypto.createHash('md5');
    hash.update(sessioId + Date.now());
    return hash.digest('hex');
}

exports.createSession = function(username, callback) {
    var sessionId = generateSessionId(username);
    var sessionSecret = generateSessionSecret(sessionId);
    db.sessions.insert({sessionId: sessionId, sessionSecret: sessionSecret}, {$set:{username: username}}, callback);
}

exports.removeSession = function(sessionId, callback) {
    db.sessions.remove({sessionId:sessionId}, false, callback);
}

exports.getSession = function(sessionId, callback) {
    db.sessions.findOne({sessionId: sessionId}, callback);
}