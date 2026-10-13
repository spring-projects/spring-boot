/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.rest.service;

import smoketest.data.rest.domain.City;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "cities", path = "cities")
interface CityRepository extends PagingAndSortingRepository<City, Long> {

	Page<City> findByNameContainingAndCountryContainingAllIgnoringCase(@Param("name") String name,
			@Param("country") String country, Pageable pageable);

	City findByNameAndCountryAllIgnoringCase(@Param("name") String name, @Param("country") String country);

}
