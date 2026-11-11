/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.cache;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@CacheConfig(cacheNames = "countries")
public class CountryRepository {

	@Cacheable
	public Country findByCode(String code) {
		System.out.println("---> Loading country with code '" + code + "'");
		return new Country(code);
	}

}
