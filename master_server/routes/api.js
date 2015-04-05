/*
 * POST API.
 */

var gp = require('../data/groupprovider.js');
var up = require('../data/userprovider.js');
var sp = require('../data/sessionprovider.js');
var cp = require('../data/reportsprovider.js');
var apiStatus = require('./apiStatus.js');
var crypto = require('crypto');
var console = require('console');

function generateSignature(method, sessionId, sessionSecret) {
    var hash = crypto.createHash('md5');
    hash.update(method + sessionId + sessionSecret);
    return hash.digest('hex');
}

function securityCheck(res, req, method, callback) {
    var requestSignature = req.body.signature;
    var sessionId = req.body.sessionId;
    var remoteAddress = req.connection.remoteAddress;
    sp.getSession(sessionId, function(err, result) {
        if (!err && result) {
            var signature = generateSignature(method, sessionId, result.sessionSecret);
            if (signature == requestSignature) {
                console.log(remoteAddress, result.remoteAddress);
                if (remoteAddress == result.remoteAddress) {
                    apiStatus.ok(res, callback());
                } else {
                    apiStatus.permissionDenied(res);
                }
            } else {
                apiStatus.invalidSignature(res);
            }
        } else {
            apiStatus.invalidSessionId(res);
        }
    });
}

exports.seedlist = function(req, res) {
    securityCheck(res, req, "seedlist", function() {
        gp.getGroupNeighbours(req.body.userId, function(error, seeds) {
            apiStatus.ok(res, {seeds: seeds});
        });
    });
};

exports.register = function(req, res) {
    securityCheck(res, req, "register", function() {
        gp.addUser(
            req.body.userId, req.body.groupId,
            req.body.localIP, req.body.remoteIP,
            parseInt(req.body.port),
            function(error) {
                apiStatus.ok(res);
            }
        );
    });
};

exports.unregister = function(req, res) {
    securityCheck(res, req, "unregister", function() {
        gp.removeUser(req.body.userId, function(error) {
            apiStatus.ok(res);
        });
    })
};

exports.auth = function(req, res) {
    up.getUser(req.body.username, function(error, result) {
        if (!error && result) {
            if (result.password != req.body.password) {
                apiStatus.wrongUserNameOrPassword(res);
            } else {
                console.log(req.connection.remoteAddress);
                sp.createSession(req.body.username, req.body.deviceInfo, req.connection.remoteAddress, function(error, result) {
                    apiStatus.ok(res, {sessionId: result.sessionId, sessionSecret: result.sessionSecret});
                });
            }
        } else if (!result) {
            apiStatus.wrongUserNameOrPassword(res);
        } else {
            apiStatus.internalError(res);
        }
    });
}

exports.deauth = function(req, res) {
    securityCheck(res, req, "deauth", function() {
        sp.removeSession(req.body.sessionId, function(error, result) {
            if (!error && result) {
                apiStatus.ok(res);
            } else {
                apiStatus.internalError(res);
            }
        });
    })
}

exports.crash = function(req, res) {
    cp.createCrashReport(req.body.report, function(error, result) {
        if (!error && result) {
            apiStatus.ok(res);
        } else {
            apiStatus.internalError(res);
        }
    });
}
