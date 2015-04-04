var extend = require("extend");

exports.ok = function(res, obj) {
    var r = extend(true, {status: 0, error: ""}, obj);
    console.log(r);
    res.send(JSON.stringify(r));
}

exports.invalidSignature = function(res) {
    res.send(JSON.stringify({status: 1, error: "invalid signature"}));
}

exports.invalidSessionId = function(res) {
    res.send(JSON.stringify({status: 2, error: "invalid session id"}));
}

exports.wrongUserNameOrPassword = function(res) {
    res.send(JSON.stringify({status: 3, error: "wrong username or password"}));
}

exports.internalError = function(res) {
    res.send(JSON.stringify({status: 4, error: "internal error"}));
}

