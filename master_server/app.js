
/**
 * Module dependencies.
 */

var express = require('express');
var routes = require('./routes');
var user = require('./routes/user');
var api = require('./routes/api');
var http = require('http');
var stylus = require('stylus');
var bootstrap = require('bootstrap3-stylus');
//var nib = require('nib');
var path = require('path');

var app = express();

function compile(str, path) {
  return stylus(str)
    .set('filename', path)
    .set('compress', true)
    .use(bootstrap());
}

// all environments
app.set('port', process.env.PORT || 3000);
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');
app.use(express.favicon());
app.use(express.logger('dev'));
app.use(express.json());
app.use(express.urlencoded());
app.use(express.methodOverride());
app.use(express.cookieParser('your secret here'));
app.use(express.session());
app.use(app.router);
app.use(stylus.middleware({src: path.join(__dirname, 'public'), compile: compile}));

app.use(express.static(path.join(__dirname, 'public')));

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

app.get('/', routes.index);
app.get('/download', routes.download);
app.get('/signup', routes.signup);
app.post('/api/seedlist', api.seedlist);
app.post('/api/register', api.register);
app.post('/api/unregister', api.unregister);
app.post('/api/auth', api.auth);
app.post('/api/deauth', api.deauth)
app.post('/api/crash', api.crash);

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
});
