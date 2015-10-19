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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthMvcEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class HealthMvcEndpointTests {

	private static final PropertySource<?> NON_SENSITIVE = new MapPropertySource("test",
			Collections.<String, Object>singletonMap("endpoints.health.sensitive",
					"false"));

	private HealthEndpoint endpoint = null;

	private HealthMvcEndpoint mvc = null;

	private MockEnvironment environment;

	private UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(
			"user", "password",
			AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));

	private UsernamePasswordAuthenticationToken admin = new UsernamePasswordAuthenticationToken(
			"user", "password",
			AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ADMIN"));

	@Before
	public void init() {
		this.endpoint = mock(HealthEndpoint.class);
		given(this.endpoint.isEnabled()).willReturn(true);
		this.mvc = new HealthMvcEndpoint(this.endpoint);
		this.environment = new MockEnvironment();
		this.mvc.setEnvironment(this.environment);
	}

	@Test
	public void up() {
		given(this.endpoint.invoke()).willReturn(new Health.Builder().up().build());
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void down() {
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof ResponseEntity);
		ResponseEntity<Health> response = (ResponseEntity<Health>) result;
		assertTrue(response.getBody().getStatus() == Status.DOWN);
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void customMapping() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().status("OK").build());
		this.mvc.setStatusMapping(
				Collections.singletonMap("OK", HttpStatus.INTERNAL_SERVER_ERROR));
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof ResponseEntity);
		ResponseEntity<Health> response = (ResponseEntity<Health>) result;
		assertTrue(response.getBody().getStatus().equals(new Status("OK")));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void customMappingWithRelaxedName() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().outOfService().build());
		this.mvc.setStatusMapping(Collections.singletonMap("out-of-service",
				HttpStatus.INTERNAL_SERVER_ERROR));
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof ResponseEntity);
		ResponseEntity<Health> response = (ResponseEntity<Health>) result;
		assertTrue(response.getBody().getStatus().equals(Status.OUT_OF_SERVICE));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	public void secureEvenWhenNotSensitive() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		given(this.endpoint.isSensitive()).willReturn(false);
		Object result = this.mvc.invoke(this.admin);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
		assertEquals("bar", ((Health) result).getDetails().get("foo"));
	}

	@Test
	public void secureNonAdmin() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.user);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
		assertNull(((Health) result).getDetails().get("foo"));
	}

	@Test
	public void healthIsCached() {
		given(this.endpoint.getTimeToLive()).willReturn(10000L);
		given(this.endpoint.isSensitive()).willReturn(true);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.admin);
		assertTrue(result instanceof Health);
		Health health = (Health) result;
		assertTrue(health.getStatus() == Status.UP);
		assertThat(health.getDetails().size(), is(equalTo(1)));
		assertThat(health.getDetails().get("foo"), is(equalTo((Object) "bar")));
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		result = this.mvc.invoke(null); // insecure now
		assertTrue(result instanceof Health);
		health = (Health) result;
		// so the result is cached
		assertTrue(health.getStatus() == Status.UP);
		// but the details are hidden
		assertThat(health.getDetails().size(), is(equalTo(0)));
	}

	@Test
	public void unsecureAnonymousAccessUnrestricted() {
		this.mvc = new HealthMvcEndpoint(this.endpoint, false);
		this.mvc.setEnvironment(this.environment);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
		assertEquals("bar", ((Health) result).getDetails().get("foo"));
	}

	@Test
	public void unsensitiveAnonymousAccessRestricted() {
		this.environment.getPropertySources().addLast(NON_SENSITIVE);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
		assertNull(((Health) result).getDetails().get("foo"));
	}

	@Test
	public void unsecureUnsensitiveAnonymousAccessUnrestricted() {
		this.mvc = new HealthMvcEndpoint(this.endpoint, false);
		this.mvc.setEnvironment(this.environment);
		this.environment.getPropertySources().addLast(NON_SENSITIVE);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
		assertEquals("bar", ((Health) result).getDetails().get("foo"));
	}

	@Test
	public void noCachingWhenTimeToLiveIsZero() {
		given(this.endpoint.getTimeToLive()).willReturn(0L);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		result = this.mvc.invoke(null);
		@SuppressWarnings("unchecked")
		Health health = ((ResponseEntity<Health>) result).getBody();
		assertTrue(health.getStatus() == Status.DOWN);
	}

	@Test
	public void newValueIsReturnedOnceTtlExpires() throws InterruptedException {
		given(this.endpoint.getTimeToLive()).willReturn(50L);
		given(this.endpoint.isSensitive()).willReturn(false);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(null);
		assertTrue(result instanceof Health);
		assertTrue(((Health) result).getStatus() == Status.UP);
		Thread.sleep(100);
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		result = this.mvc.invoke(null);
		@SuppressWarnings("unchecked")
		Health health = ((ResponseEntity<Health>) result).getBody();
		assertTrue(health.getStatus() == Status.DOWN);
	}

}
