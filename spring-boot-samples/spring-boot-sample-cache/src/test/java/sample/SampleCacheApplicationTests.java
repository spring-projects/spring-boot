/*
 * Copyright 2012-2015 the original author or authors.
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

package sample;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { SampleCacheApplication.class })
public class SampleCacheApplicationTests {

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CountryRepository countryRepository;

	@Test
	public void validateCache() {
		Cache countries = this.cacheManager.getCache("countries");
		assertThat(countries, is(notNullValue()));
		countries.clear(); // Simple test assuming the cache is empty
		assertThat(countries.get("BE"), is(nullValue()));
		Country be = this.countryRepository.findByCode("BE");
		assertThat((Country) countries.get("BE").get(), is(be));
	}

}
