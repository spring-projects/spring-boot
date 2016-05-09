--liquibase formatted sql

--changeset author:awilkinson

CREATE TABLE customer (
    id int AUTO_INCREMENT NOT NULL PRIMARY KEY,
    name varchar(50) NOT NULL
);