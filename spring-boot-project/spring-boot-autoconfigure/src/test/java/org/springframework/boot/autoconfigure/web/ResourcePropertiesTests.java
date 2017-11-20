/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.time.Duration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.web.ResourceProperties.CacheControlProperties;
import org.springframework.boot.testsupport.assertj.Matched;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.endsWith;

/**
 * Tests for {@link ResourceProperties}.
 *
 * @author Stephane Nicoll
 * @author Kristine Jetzke
 */
public class ResourcePropertiesTests {

	private final ResourceProperties properties = new ResourceProperties();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void resourceChainNoCustomization() {
		assertThat(this.properties.getChain().getEnabled()).isNull();
	}

	@Test
	public void resourceChainStrategyEnabled() {
		this.properties.getChain().getStrategy().getFixed().setEnabled(true);
		assertThat(this.properties.getChain().getEnabled()).isTrue();
	}

	@Test
	public void resourceChainEnabled() {
		this.properties.getChain().setEnabled(true);
		assertThat(this.properties.getChain().getEnabled()).isTrue();
	}

	@Test
	public void resourceChainDisabled() {
		this.properties.getChain().setEnabled(false);
		assertThat(this.properties.getChain().getEnabled()).isFalse();
	}

	@Test
	public void defaultStaticLocationsAllEndWithTrailingSlash() {
		assertThat(this.properties.getStaticLocations()).are(Matched.by(endsWith("/")));
	}

	@Test
	public void customStaticLocationsAreNormalizedToEndWithTrailingSlash() {
		this.properties.setStaticLocations(new String[] { "/foo", "/bar", "/baz/" });
		String[] actual = this.properties.getStaticLocations();
		assertThat(actual).containsExactly("/foo/", "/bar/", "/baz/");
	}

	@Test
	public void emptyCacheControl() {
		CacheControlProperties cacheControl = new CacheControlProperties();
		this.properties.setCacheControl(cacheControl);
		assertThat(this.properties.getCacheControl().toHttpCacheControl().getHeaderValue()).isNull();
	}

	@Test
	public void cacheControlAllPropertiesSet() {
		CacheControlProperties cacheControl = new CacheControlProperties();
		cacheControl.setMaxAge(Duration.ofSeconds(4));
		cacheControl.setCachePrivate(true);
		cacheControl.setCachePublic(true);
		cacheControl.setMustRevalidate(true);
		cacheControl.setNoTransform(true);
		cacheControl.setProxyRevalidate(true);
		cacheControl.setsMaxAge(Duration.ofSeconds(5));
		cacheControl.setStaleIfError(Duration.ofSeconds(6));
		cacheControl.setStaleWhileRevalidate(Duration.ofSeconds(7));
		this.properties.setCacheControl(cacheControl);
		assertThat(this.properties.getCacheControl().toHttpCacheControl().getHeaderValue()).isEqualTo(
				"max-age=4, must-revalidate, no-transform, public, private, proxy-revalidate," +
						" s-maxage=5, stale-if-error=6, stale-while-revalidate=7");
	}

	@Test
	public void invalidCacheControlCombination() {
		CacheControlProperties cacheControl = new CacheControlProperties();
		cacheControl.setMaxAge(Duration.ofSeconds(4));
		cacheControl.setNoStore(true);
		this.properties.setCacheControl(cacheControl);
		assertThat(this.properties.getCacheControl().toHttpCacheControl().getHeaderValue()).isEqualTo("no-store");
	}

	@Test
	public void cacheControlNoPropertiesSet() {
		this.properties.setCacheControl(new CacheControlProperties());
		assertThat(this.properties.getCacheControl().toHttpCacheControl().getHeaderValue()).isNull();
	}

}
