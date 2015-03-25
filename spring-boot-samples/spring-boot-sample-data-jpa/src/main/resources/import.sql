--
-- Sample dataset containing a number of Hotels in various Cities across the world.  The reviews are entirely fictional :)
--

-- =================================================================================================
-- Sports
insert into sport(name, type) values ('Ski', 'WINTER')
insert into sport(name, type) values ('Snowboard', 'WINTER')
insert into sport(name, type) values ('Beach Volley', 'SUMMER')


-- Montreal
insert into city(country, name, state, map) values ('Canada', 'Montreal', 'Quebec', '45.508889, -73.554167')
insert into hotel(city_id, name, address, zip, type) values (1, 'Ritz Carlton', '1228 Sherbrooke St', 'H3G1H6', 'spa')

-- Barcelona
insert into city(country, name, state, map) values ('Spain', 'Barcelona', 'Catalunya', '41.387917, 2.169919')
insert into hotel(city_id, name, address, zip, type) values (2, 'Hilton Diagonal Mar', 'Passeig del Taulat 262-264', '08019', 'spa')

-- Neuchatel
insert into city(country, name, state, map) values ('Switzerland', 'Neuchatel', '', '46.992979, 6.931933')
insert into hotel(city_id, name, address, zip, type, main_sport_id) values (3, 'Hotel Beaulac', ' Esplanade Leopold-Robert 2', '2000', 'sport', 1)