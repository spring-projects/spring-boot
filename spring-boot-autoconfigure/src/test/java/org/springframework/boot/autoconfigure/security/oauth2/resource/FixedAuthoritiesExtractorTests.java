/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FixedAuthoritiesExtractor}.
 *
 * @author Dave Syer
 */
public class FixedAuthoritiesExtractorTests {

	private FixedAuthoritiesExtractor extractor = new FixedAuthoritiesExtractor();

	private Map<String, Object> map = new LinkedHashMap<String, Object>();

	@Test
	public void authorities() {
		this.map.put("authorities", "ROLE_ADMIN");
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[ROLE_ADMIN]");
	}

	@Test
	public void authoritiesCommaSeparated() {
		this.map.put("authorities", "ROLE_USER,ROLE_ADMIN");
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[ROLE_USER, ROLE_ADMIN]");
	}

	@Test
	public void authoritiesArray() {
		this.map.put("authorities", new String[] { "ROLE_USER", "ROLE_ADMIN" });
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[ROLE_USER, ROLE_ADMIN]");
	}

	@Test
	public void authoritiesList() {
		this.map.put("authorities", Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[ROLE_USER, ROLE_ADMIN]");
	}

	@Test
	public void authoritiesAsListOfMaps() {
		this.map.put("authorities",
				Arrays.asList(Collections.singletonMap("authority", "ROLE_ADMIN")));
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[ROLE_ADMIN]");
	}

	@Test
	public void authoritiesAsListOfMapsWithStandardKey() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("role", "ROLE_ADMIN");
		map.put("extra", "value");
		this.map.put("authorities", Arrays.asList(map));
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[ROLE_ADMIN]");
	}

	@Test
	public void authoritiesAsListOfMapsWithNonStandardKey() {
		this.map.put("authorities",
				Arrays.asList(Collections.singletonMap("any", "ROLE_ADMIN")));
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[ROLE_ADMIN]");
	}

	@Test
	public void authoritiesAsListOfMapsWithMultipleNonStandardKeys() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("any", "ROLE_ADMIN");
		map.put("foo", "bar");
		this.map.put("authorities", Arrays.asList(map));
		assertThat(this.extractor.extractAuthorities(this.map).toString())
				.isEqualTo("[{foo=bar, any=ROLE_ADMIN}]");
	}

}
