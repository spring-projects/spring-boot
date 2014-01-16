--
-- Sample dataset containing a number of Hotels in various Cities across the world.  The reviews are entirely fictional :)
--

-- =================================================================================================
-- AUSTRALIA

-- Brisbane
insert into city(country, name, state, map) values ('Australia', 'Brisbane', 'Queensland', '-27.470933, 153.023502')
insert into hotel(city_id, name, address, zip) values (1, 'Conrad Treasury Place', 'William & George Streets', '4001')

-- Melbourne
insert into city(country, name, state, map) values ('Australia', 'Melbourne', 'Victoria', '-37.813187, 144.96298')
insert into hotel(city_id, name, address, zip) values (2, 'The Langham', '1 Southgate Ave, Southbank', '3006')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (2, 0, '2005-05-10', 2, 4, 'Pretty average', 'I stayed in 2005, the hotel was nice enough but nothing special.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (2, 1, '2006-01-12', 4, 2, 'Bright hotel with big rooms', 'This hotel has a fantastic lovely big windows.  The room we stayed in had lots of space.  Recommended.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (2, 2, '2006-05-25', 3, 1, 'Pretty good', 'I liked this hotel and would stay again.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (2, 3, '2009-01-20', 3, 2, 'Nice clean rooms', 'The rooms are maintained to a high standard and very clean, the bathroom was spotless!!')

-- Sydney
insert into city(country, name, state, map) values ('Australia', 'Sydney', 'New South Wales', '-33.868901, 151.207091')
insert into hotel(city_id, name, address, zip) values (3, 'Swissotel', '68 Market Street', '2000')


-- =================================================================================================
-- CANADA

-- Montreal
insert into city(country, name, state, map) values ('Canada', 'Montreal', 'Quebec', '45.508889, -73.554167')
insert into hotel(city_id, name, address, zip) values (4, 'Ritz Carlton', '1228 Sherbrooke St', 'H3G1H6')


-- =================================================================================================
-- ISRAEL

-- Tel Aviv
insert into city(country, name, state, map) values ('Israel', 'Tel Aviv', '', '32.066157, 34.777821')
insert into hotel(city_id, name, address, zip) values (5, 'Hilton Tel Aviv', 'Independence Park', '63405')


-- =================================================================================================
-- JAPAN

-- Tokyo
insert into city(country, name, state, map) values ('Japan', 'Tokyo', '', '35.689488, 139.691706')
insert into hotel(city_id, name, address, zip) values (6, 'InterContinental Tokyo Bay', 'Takeshiba Pier', '105')


-- =================================================================================================
-- SPAIN

-- Barcelona
insert into city(country, name, state, map) values ('Spain', 'Barcelona', 'Catalunya', '41.387917, 2.169919')
insert into hotel(city_id, name, address, zip) values (7, 'Hilton Diagonal Mar', 'Passeig del Taulat 262-264', '08019')

-- =================================================================================================
-- SWITZERLAND

-- Neuchatel
insert into city(country, name, state, map) values ('Switzerland', 'Neuchatel', '', '46.992979, 6.931933')
insert into hotel(city_id, name, address, zip) values (8, 'Hotel Beaulac', ' Esplanade Leopold-Robert 2', '2000')


-- =================================================================================================
-- UNITED KINGDOM

-- Bath
insert into city(country, name, state, map) values ('UK', 'Bath', 'Somerset', '51.381428, -2.357454')
insert into hotel(city_id, name, address, zip) values (9, 'The Bath Priory Hotel', 'Weston Road', 'BA1 2XT')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 0, '2000-01-23', 4, 1, 'A lovely hotel', 'We stayed here after a wedding and it was fantastic.  Recommend to all.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 1, '2000-08-04', 3, 1, 'Very special', 'A very special hotel with lovely staff.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 2, '2001-01-01', 2, 4, 'Nice but too hot', 'Stayed during the summer heat wave (exceptional for England!) and the room was very hot.  Still recommended.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 3, '2002-01-20', 3, 1, 'Big rooms and a great view', 'Considering how central this hotel is the rooms are a very good size.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 4, '2002-11-03', 2, 1, 'Good but pricey', 'A nice hotel but be prepared to pay over the odds for your stay.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 5, '2003-09-18', 4, 1, 'Fantastic place', 'Just lovely.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 6, '2004-03-21', 4, 3, 'A very special place', 'I stayed here in 2004 and found it to be very relaxing, a nice pool and good gym is cherry on the cake.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 7, '2004-04-10', 0, 0, 'Terrible', 'I complained after I was told I could not check out after 11pm.  Ridiculous!!!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 8, '2004-12-20', 4, 4, 'A perfect location', 'Central location makes this a perfect hotel.  Be warned though, it''s not cheap.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 9, '2005-04-19', 3, 2, 'Expensive but worth it', 'Dig deep into your pockets and enjoy this lovely City and fantastic hotel.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 10, '2005-05-21', 4, 1, 'The best hotel in the area', 'Top hotel in the area, would not stay anywhere else.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 11, '2005-11-17', 4, 2, 'Lovely hotel, fantastic grounds', 'The garden upkeep run into thousands (perhaps explaining why the rooms are so much) but so lovely and relaxing.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 12, '2006-01-04', 3, 4, 'Gorgeous Top Quality Hotel', 'Top draw stuff.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 13, '2006-01-21', 4, 1, 'Fabulous Hotel and Restaurant', 'The food at this hotel is second to none, try the peppered steak!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 14, '2006-01-29', 4, 4, 'Feels like home', 'A lovely home away from home.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 15, '2006-03-21', 1, 1, 'Far too expensive', 'Overpriced, Overpriced, Overpriced!!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 16, '2006-05-10', 4, 1, 'Excellent Hotel, Wonderful Staff', 'The staff went out of their way to help us after we missed our last train home, organising a Taxi back to Newport even after we had checked out.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 17, '2007-09-11', 3, 2, 'The perfect retreat', 'If you want a relaxing stay, this is the place.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 18, '2008-06-01', 3, 3, 'Lovely stay, fantastic staff', 'As other reviews have noted, the staff in this hotel really are the best in Bath.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 19, '2009-05-14', 4, 2, 'Can''t Wait to go back', 'We will stay again for sure.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 20, '2010-04-26', 4, 1, 'Amazing Hotel', 'We won a trip here after entering a competition.  Not sure we would pay the full price but such a wonderful place.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (9, 21, '2010-10-26', 2, 2, 'Dissapointed', 'The pool was closed, the chief was ill, the staff were rude my wallet is bruised!')
insert into hotel(city_id, name, address, zip) values (9, 'Bath Travelodge', 'Rossiter Road, Widcombe Basin', 'BA2 4JP')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 0, '2002-08-21', 0, 2, 'Terrible hotel', 'One of the worst hotels that I have ever stayed in.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 1, '2003-01-28', 0, 0, 'Rude and unpleasant staff', 'The staff refused to help me with any aspect of my stay, I will not stay here again.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 2, '2004-06-17', 1, 0, 'Below par', 'Don''t stay here!!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 3, '2005-07-12', 0, 1, 'Small and Unpleasant', 'The room was far too small and felt unclean.  Not recommended.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 4, '2006-01-07', 1, 4, 'Cheap if you are not fussy', 'This hotel has some rough edges but I challenge you to find somewhere cheaper.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 5, '2006-01-13', 0, 2, 'Terrible', 'Just terrible!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 6, '2006-03-25', 0, 0, 'Smelly and dirty room', 'My room smelt of damp and I found the socks of the previous occupant under my bed.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 7, '2006-04-09', 0, 4, 'Grim', 'Grim.  I would try elsewhere before staying here.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 8, '2006-08-01', 1, 3, 'Very Noisy', 'Building work during the day and a disco at night.  Good grief!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 9, '2009-01-03', 1, 4, 'Tired and falling down', 'This hotel is in serious need of refurbishment, the windows are rotting, the paintwork is tired and the carpets are from the 1970s.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 10, '2009-07-20', 0, 0, 'Not suitable for human habitation', 'I would not put my dog up in this hotel.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 11, '2010-05-20', 1, 0, 'Conveient for the railway', 'Average place but useful if you need to commute')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 12, '2010-01-22', 2, 2, 'Not as bad as the reviews', 'Some of the reviews seem a bit harsh, it''s not too bad for the price.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (10, 13, '2011-01-10', 3, 1, 'Reburished and nice', 'Looks like this hotel has had a major facelift.  If you have stayed before 2011 perhaps it''s time to give this hotel another try.  Very good value for money and pretty nice.')

-- London
insert into city(country, name, state, map) values ('UK', 'London', '', '51.500152, -0.126236')
insert into hotel(city_id, name, address, zip) values (10, 'Melia White House', 'Albany Street', 'NW1 3UP')

-- Southampton
insert into city(country, name, state, map) values ('UK', 'Southampton', 'Hampshire', '50.902571, -1.397238')
insert into hotel(city_id, name, address, zip) values (11, 'Chilworth Manor', 'The Cottage, Southampton Business Park', 'SO16 7JF')


-- =================================================================================================
-- USA

-- Atlanta
insert into city(country, name, state, map) values ('USA', 'Atlanta', 'GA', '33.748995, -84.387982')
insert into hotel(city_id, name, address, zip) values (12, 'Marriott Courtyard', 'Tower Place, Buckhead', '30305')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (13, 0, '2009-01-20', 3, 0, 'Better than most', 'Most other hotels is this area are a bit ropey, this one is actually pretty good.')
insert into hotel(city_id, name, address, zip) values (12, 'Ritz Carlton', 'Peachtree Rd, Buckhead', '30326')
insert into hotel(city_id, name, address, zip) values (12, 'Doubletree', 'Tower Place, Buckhead', '30305')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (15, 0, '2006-01-12', 2, 3, 'No fuss hotel', 'Cheap, no fuss hotel.  Good if you are travelling on business and just need a place to stay.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (15, 1, '2009-01-20', 2, 2, 'Nice area but small rooms', 'The area felt nice and safe but the rooms are a little on the small side')

-- Chicago
insert into city(country, name, state, map) values ('USA', 'Chicago', 'IL', '41.878114, -87.629798')
insert into hotel(city_id, name, address, zip) values (13, 'Hotel Allegro', '171 West Randolph Street', '60601')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (16, 0, '2009-12-15', 3, 2, 'Cheap and Recommended', 'Good value for money, can''t really fault it.')

-- Eau Claire
insert into city(country, name, state, map) values ('USA', 'Eau Claire', 'WI', '44.811349, -91.498494')
insert into hotel(city_id, name, address, zip) values (14, 'Sea Horse Inn', '2106 N Clairemont Ave', '54703')
insert into hotel(city_id, name, address, zip) values (14, 'Super 8 Eau Claire Campus Area', '1151 W Macarthur Ave', '54701')

-- Hollywood
insert into city(country, name, state, map) values ('USA', 'Hollywood', 'FL', '26.011201, -80.14949')
insert into hotel(city_id, name, address, zip) values (15, 'Westin Diplomat', '3555 S. Ocean Drive', '33019')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (19, 0, '2006-01-11', 0, 0, 'Avoid', 'The hotel has a very bad reputation.  I would avoid it if I were you.')

-- Miami
insert into city(country, name, state, map) values ('USA', 'Miami', 'FL', '25.788969, -80.226439')
insert into hotel(city_id, name, address, zip) values (16, 'Conrad Miami', '1395 Brickell Ave', '33131')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (20, 0, '2010-01-09', 3, 2, 'Close to the local attractions', 'Fantastic access to all the local attractions mean you won''t mind the small rooms.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (20, 1, '2010-09-10', 2, 2, 'Good value and friendly', 'Not expensive and very welcoming staff. I would stay again.')

-- Melbourne
insert into city(country, name, state, map) values ('USA', 'Melbourne', 'FL', '28.083627, -80.608109')
insert into hotel(city_id, name, address, zip) values (17, 'Radisson Suite Hotel Oceanfront', '3101 North Hwy', '32903')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (21, 0, '2005-06-15', 3, 3, 'A very nice hotel', 'I can''t fault this hotel and I have stayed here many times.  Always friendly staff and lovely atmosphere.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (21, 1, '2006-01-20', 2, 4, 'Comfortable and good value', 'To complaints at all.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (21, 2, '2007-08-21', 3, 1, 'Above average', 'Better than a lot of hotels in the area and not too pricey.')

-- New York
insert into city(country, name, state, map) values ('USA', 'New York', 'NY', '40.714353, -74.005973')
insert into hotel(city_id, name, address, zip) values (18, 'W Union Hotel', 'Union Square, Manhattan', '10011')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (22, 0, '2002-01-19', 0, 1, 'Too noisy, too small', 'The city never sleeps and neither will you if you say here.  The rooms are small and the sound insulation is poor!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (22, 1, '2004-03-10', 1, 4, 'Overpriced', 'Far too much money for such a tiny room!')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (22, 2, '2007-04-11', 2, 0, 'So so, nothing special', 'Not brilliant but not too bad either.')
insert into hotel(city_id, name, address, zip) values (18, 'W Lexington Hotel', 'Lexington Ave, Manhattan', '10011')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (23, 0, '2004-07-21', 3, 2, 'Excellent location', 'So close to the heart of the city.  Recommended.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (23, 1, '2006-05-20', 3, 1, 'Very nice', 'I can''t fault this hotel, clean, good location and nice staff.')
insert into hotel(city_id, name, address, zip) values (18, '70 Park Avenue Hotel', '70 Park Avenue', '10011')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (24, 0, '2003-11-10', 4, 1, 'Great!!', 'I own this hotel and I think it is pretty darn good.')

-- Palm Bay
insert into city(country, name, state, map) values ('USA', 'Palm Bay', 'FL', '28.034462, -80.588665')
insert into hotel(city_id, name, address, zip) values (19, 'Jameson Inn', '890 Palm Bay Rd NE', '32905')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (25, 0, '2005-10-20', 3, 2, 'Fantastical', 'This is the BEST hotel in Palm Bay, not complaints at all.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (25, 1, '2006-01-12', 4, 1, 'Top marks', 'I rate this hotel 5 stars, the best in the area by miles.')

-- San Francisco
insert into city(country, name, state, map) values ('USA', 'San Francisco', 'CA', '37.77493, -122.419415')
insert into hotel(city_id, name, address, zip) values (20, 'Marriot Downtown', '55 Fourth Street', '94103')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (26, 0, '2006-07-02', 2, 3, 'Could be better', 'I stayed in late 2006 with work, the room was very small and the restaurant does not stay open very late.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (26, 1, '2008-07-01', 1, 4, 'Brrrr cold!', 'My room was freezing cold, I would not recommend this place.')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (26, 2, '2009-01-05', 3, 2, 'Nice for money', 'You can''t really go wrong here for the money.  There may be better places to stay but not for this price.')

-- Washington
insert into city(country, name, state, map) values ('USA', 'Washington', 'DC', '38.895112, -77.036366')
insert into hotel(city_id, name, address, zip) values (21, 'Hotel Rouge', '1315 16th Street NW', '20036')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (27, 0, '2000-01-29', 0, 2, 'Never again', 'I will never ever stay here again!!  They wanted extra cash to get fresh batteries for the TV remote')
insert into review(hotel_id, index, check_in_date, rating, trip_type, title, details) values (27, 1, '2006-02-20', 0, 0, 'Avoid', 'This place is the pits, they charged us twice for a single night stay.  I only got refunded after contacting my credit card company.')
