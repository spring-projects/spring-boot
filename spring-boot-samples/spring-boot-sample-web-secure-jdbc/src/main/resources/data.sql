insert into users (username, password, enabled) values ('user', '{noop}user', true);

insert into authorities (username, authority) values ('user', 'ROLE_ADMIN');