create table custom_users (
  username varchar(50) not null primary key,
  password varchar(500) not null,
  enabled boolean not null
);
