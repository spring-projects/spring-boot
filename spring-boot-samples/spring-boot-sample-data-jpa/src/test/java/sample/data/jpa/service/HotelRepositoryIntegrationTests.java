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
import static org.hamcrest.Matchers.equalTo;
import static sample.data.jpa.domain.QHotel.hotel;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sample.data.jpa.SampleDataJpaApplication;
import sample.data.jpa.domain.Hotel;

/**
 * Integration tests for {@link HotelRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleDataJpaApplication.class)
public class HotelRepositoryIntegrationTests {

  @Autowired
  HotelRepository repository;

  @Test
  public void test() {
    Hotel ritzCarltonMontreal = repository.findOne(hotel.city.country.eq("Canada"));
    assertThat(ritzCarltonMontreal.getName(), equalTo("Ritz Carlton"));
  }
}
