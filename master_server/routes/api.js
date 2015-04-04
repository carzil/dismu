/*
 * POST API.
 */

var gp = require('../data/groupprovider.js');
var up = require('../data/userprovider.js');
var sp = require('../data/sessionprovider.js');
var apiStatus = require('./apiStatus.js');
var crypto = require('crypto');
var console = require('console');

function generateSignature(method, sessionId, sessionSecret) {
    var hash = crypto.createHash('md5');
    hash.update(method + sessionId + sessionSecret);
    return hash.digest('hex');
}

function signatureCheck(method, requestSignature, sessionId, callback) {
    sp.getSession(sessionId, function(err, result) {
        if (!err && result) {
            var signature = generateSignature(method, sessionId, result.sessionSecret);
            if (signature == requestSignature) {
                callback(null);
            } else {
                callback(JSON.stringify({error: "invalid signature"}));
            }
        } else {
            callback(JSON.stringify({error: "invalid session id"}));
        }
    });
}

exports.seedlist = function(req, res) {
    signatureCheck("seedlist", req.body.signature, req.body.sessionId, function(error) {
        if (!error) {
            gp.getGroupNeighbours(req.body.userId, function(error, seeds) {
                apiStatus.ok(res, {seeds: seeds});
            });
        } else {
            apiStatus.internalError(res);
        }
    });
};

exports.register = function(req, res) {
    signatureCheck("register", req.body.signature, req.body.sessionId, function(error) {
        if (!error) {
            gp.addUser(
                req.body.userId, req.body.groupId,
                req.body.localIP, req.body.remoteIP,
                parseInt(req.body.port),
                function(error) {
                    apiStatus.ok(res);
                }
            );
        } else {
            apiStatus.internalError();
        }
    });
};

exports.unregister = function(req, res) {
    signatureCheck("unregister", req.body.signature, req.body.sessionId, function(error) {
        if (!error) {
            gp.removeUser(req.body.userId, function(error) {
                apiStatus.ok()
            });
        } else {
            apiStatus.internalError();
        }
    })
};

exports.auth = function(req, res) {
    up.getUser(req.body.username, function(error, result) {
        if (!error && result) {
            if (result.password != req.body.password) {
                apiStatus.wrongUserNameOrPassword(res);
            } else {
                sp.createSession(req.body.username, function(error, result) {
                    apiStatus.ok(res, {sessionId: result.sessionId, sessionSecret: result.sessionSecret});
                });
            }
        } else {
            apiStatus.wrongUserNameOrPassword(res);
        }
    });
}

exports.deauth = function(req, res) {
    signatureCheck("deauth", req.body.signature, req.body.sessionId, function(error) {
        if (!error) {
            apiStatus.ok(res);
        } else {
            apiStatus.internalError(res);
        }
    })
}
