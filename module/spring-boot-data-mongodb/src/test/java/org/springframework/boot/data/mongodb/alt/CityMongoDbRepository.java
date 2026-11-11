/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.alt;

import org.springframework.boot.data.mongodb.autoconfigure.domain.city.City;
import org.springframework.data.repository.Repository;

public interface CityMongoDbRepository extends Repository<City, Long> {

}
