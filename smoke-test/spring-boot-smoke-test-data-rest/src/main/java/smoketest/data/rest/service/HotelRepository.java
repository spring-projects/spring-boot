/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.rest.service;

import smoketest.data.rest.domain.City;
import smoketest.data.rest.domain.Hotel;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "hotels", path = "hotels")
interface HotelRepository extends PagingAndSortingRepository<Hotel, Long> {

	Hotel findByCityAndName(City city, String name);

}
