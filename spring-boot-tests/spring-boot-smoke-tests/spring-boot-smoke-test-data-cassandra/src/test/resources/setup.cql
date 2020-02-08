CREATE KEYSPACE IF NOT EXISTS mykeyspace WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};
CREATE TABLE mykeyspace.customer (id TimeUUID PRIMARY KEY, firstname text, lastname text);
CREATE INDEX customerfistnameindex ON mykeyspace.customer (firstname);
CREATE INDEX customersecondnameindex ON mykeyspace.customer (lastname);
