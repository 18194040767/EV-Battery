INSERT INTO role (role_code, role_name, description) VALUES
('ROLE_ADMIN', '管理员', '管理员角色'),
('ROLE_USER', '普通用户', '普通用户角色')
ON DUPLICATE KEY UPDATE
role_name = VALUES(role_name),
description = VALUES(description);

INSERT INTO user (username, password, real_name, email, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVwE5q', '平台管理员', 'admin@ev.com', 1),
('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVwE5q', '测试用户', 'test@ev.com', 1)
ON DUPLICATE KEY UPDATE
password = VALUES(password),
real_name = VALUES(real_name),
email = VALUES(email),
status = VALUES(status);

INSERT INTO user_role (user_id, role_id) VALUES
((SELECT id FROM user WHERE username='admin'), (SELECT id FROM role WHERE role_code='ROLE_ADMIN')),
((SELECT id FROM user WHERE username='test'), (SELECT id FROM role WHERE role_code='ROLE_USER'))
ON DUPLICATE KEY UPDATE
user_id = VALUES(user_id),
role_id = VALUES(role_id);

INSERT INTO permission (perm_code, perm_name, perm_type, path) VALUES
('user:list', '用户列表', 'API', '/api/admin/users'),
('battery:upload', '电池上传', 'API', '/api/battery/upload/single'),
('trade:order', '订单操作', 'API', '/api/trade/order/place')
ON DUPLICATE KEY UPDATE
perm_name = VALUES(perm_name),
perm_type = VALUES(perm_type),
path = VALUES(path);

INSERT INTO role_permission (role_id, permission_id) VALUES
((SELECT id FROM role WHERE role_code='ROLE_ADMIN'), (SELECT id FROM permission WHERE perm_code='user:list')),
((SELECT id FROM role WHERE role_code='ROLE_USER'), (SELECT id FROM permission WHERE perm_code='battery:upload')),
((SELECT id FROM role WHERE role_code='ROLE_USER'), (SELECT id FROM permission WHERE perm_code='trade:order'))
ON DUPLICATE KEY UPDATE
role_id = VALUES(role_id),
permission_id = VALUES(permission_id);
