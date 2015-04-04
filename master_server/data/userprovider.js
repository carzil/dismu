var mongojs = require('mongojs');
var db = mongojs.connect('dismu:distributedp0w3r@ds031329.mongolab.com:31329/dismu', ['users']);
var console = require('console');

exports.getUser = function(username, callback) {
    db.users.findOne({username: username}, callback);
}