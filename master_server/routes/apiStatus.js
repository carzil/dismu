var extend = require("extend");

function send(res, obj1, obj2) {
    extend(obj1, obj2);
    console.log(JSON.stringify(obj1));
    res.send(JSON.stringify(obj1));
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
