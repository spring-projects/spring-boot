INSERT INTO language VALUES (1, 'EN', 'English');

INSERT INTO author VALUES (1, 'Greg', 'Turnquest', '1804-09-17', 1804, 1);
INSERT INTO author VALUES (2, 'Craig', 'Walls', '1804-09-18', 1804, 1);

INSERT INTO book VALUES (1, 1, 'Learning Spring Boot', 2015, 1);
INSERT INTO book VALUES (2, 2, 'Spring Boot in Action', 2015, 1);

INSERT INTO book_store VALUES ('Barnes & Noble');

INSERT INTO book_to_book_store VALUES ('Barnes & Noble', 1, 10);
INSERT INTO book_to_book_store VALUES ('Barnes & Noble', 2, 3);
