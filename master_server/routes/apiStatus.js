var extend = require("extend");

function send(res, obj1, obj2) {
    var r = obj1;
    extend(r, obj2)
    res.send(JSON.stringify(r));
}

exports.ok = function(res, obj) {
    send(res, {status: 0, error: ""}, obj);
}

exports.invalidSignature = function(res, obj) {
    send(res, {status: 1, error: "invalid signature"}, obj);
}

exports.invalidSessionId = function(res,  obj) {
    send(res, {status: 2, error: "invalid session id"}, obj);
}

exports.wrongUserNameOrPassword = function(res, obj) {
    send(res, {status: 3, error: "wrong username or password"}, obj);
}

exports.internalError = function(res, obj) {
    send(res, {status: 4, error: "internal error"}, obj);
}

exports.permissionDenied = function(res, obj) {
    send(res, {status: 5, error: "permission denied"}, obj);
}
