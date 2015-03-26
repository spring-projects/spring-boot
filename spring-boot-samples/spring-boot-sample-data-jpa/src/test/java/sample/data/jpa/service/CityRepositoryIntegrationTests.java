/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.data.jpa.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sample.data.jpa.SampleDataJpaApplication;
import sample.data.jpa.domain.City;
import sample.data.jpa.domain.SpaHotel;
import sample.data.jpa.domain.Sport;
import sample.data.jpa.domain.SportHotel;
import sample.data.jpa.domain.SportType;

/**
 * Integration tests for {@link HotelRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleDataJpaApplication.class)
public class CityRepositoryIntegrationTests {

  @Autowired
  SportRepository sportRepository;

  @Autowired
  CityRepository cityRepository;

  @Autowired
  HotelRepository hotelRepository;

  private City montreal;
  private City brisbane;
  private City aspen;
  private City neuchatel;


  @Before
  public void setup() {
    montreal = cityRepository.save(new City("Montreal", "Canada"));
    brisbane = cityRepository.save(new City("Brisbane", "Australia"));
    aspen = cityRepository.save(new City("Aspen", "United States"));
    neuchatel = cityRepository.save(new City("'Neuchatel'", "'Switzerland'"));

    Sport ski = sportRepository.save(new Sport(SportType.WINTER, "Ski"));
    Sport snowboard = sportRepository.save(new Sport(SportType.WINTER, "Snowboard"));
    Sport beachVolley = sportRepository.save(new Sport(SportType.SUMMER, "Beach Volley"));

    hotelRepository.save(new SpaHotel(montreal, "Ritz Carlton"));
    hotelRepository.save(new SportHotel(brisbane, "Conrad Treasury Place", beachVolley));
    hotelRepository.save(new SportHotel(aspen, "Limelight Hotel", ski));
    hotelRepository.save(new SportHotel(neuchatel, "Hotel Beaulac", snowboard));
  }

  @Test
  public void test_findAll() {
    List<City> cities = cityRepository.findAll();
    assertThat(cities, containsInAnyOrder(montreal, aspen, neuchatel, brisbane));
  }

  @Test
  public void test_findAllCitiesWithSpaOrSportHotelJpa() {
    List<City> cities = cityRepository.findAllCitiesWithSpaOrSportHotelJpa(SportType.WINTER);
    assertThat(cities, containsInAnyOrder(montreal, aspen, neuchatel));
  }

  @Test
  public void test_findAllCitiesWithSpaOrSportHotelQueryDsl() {
    List<City> cities = cityRepository.findAllCitiesWithSpaOrSportHotelQueryDsl(SportType.WINTER);
    assertThat(cities, containsInAnyOrder(montreal, aspen, neuchatel));
  }
}
