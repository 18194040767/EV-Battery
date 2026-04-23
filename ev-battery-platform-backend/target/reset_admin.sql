UPDATE user
SET password='$2a$10$L2hcre1ZhbGo86QSp6bbMOQ7npm26m8nt4QpXKTk12.vUMug6EgyW', real_name='平台管理员', email='admin@ev.com', status=1
WHERE username='admin';
SELECT username, password FROM user WHERE username='admin';
