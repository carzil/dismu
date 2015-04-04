var mongojs = require('mongojs');
var db = mongojs.connect('dismu:distributedp0w3r@ds031329.mongolab.com:31329/dismu', ['groups', 'users']);
var console = require('console');

exports.getGroupUsers = function (groupId, callback) {
    db.groups.find({groupId:groupId}, function (err, result) {
        // stripping private "_id" field returned by mongo
        if (!err && result) {
            for (var i = 0; i < result.length; ++i) {
                delete result[i]['_id'];
            }
        }
        callback(err, result);
    });
}

exports.addUser = function (userId, groupId, localIP, remoteIP, port, callback) {
    db.groups.update({userId:userId, groupId:groupId}, {$set:{localIP:localIP, remoteIP:remoteIP, port:port}}, {'upsert':true}, callback);
}

exports.removeUser = function (userId, callback) {
    db.groups.remove({userId:userId}, false, function (err, res) {
        callback(err);
    });
}

exports.getGroupNeighbours = function (userId, callback) {
    db.groups.findOne({userId:userId}, function (err, data) {
        if (!err && data) {
            exports.getGroupUsers(data.groupId, function (error, result) {
                callback(error, result);
            });
        } else {
            callback(err, null);
        }
    });
}