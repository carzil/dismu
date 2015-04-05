
/*
 * GET home page.
 */

exports.index = function(req, res){
    res.render('index', { title: 'Express' });
};

exports.download = function(req, res){
    res.render('download', { title: 'Express' });
};

exports.signup = function(req, res) {
    res.render('signup', {});
}