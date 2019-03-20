/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthMvcEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Madhura Bhave
 */
public class HealthMvcEndpointTests {

	private static final PropertySource<?> SECURITY_ROLES = new MapPropertySource("test",
			Collections.<String, Object>singletonMap("management.security.roles",
					"HERO"));

	private HttpServletRequest request = new MockHttpServletRequest();

	private HealthEndpoint endpoint = null;

	private HealthMvcEndpoint mvc = null;

	private MockEnvironment environment;

	private HttpServletRequest defaultUser = createAuthenticationRequest("ROLE_ACTUATOR");

	private HttpServletRequest hero = createAuthenticationRequest("HERO");

	private HttpServletRequest createAuthenticationRequest(String role) {
		MockServletContext servletContext = new MockServletContext();
		servletContext.declareRoles(role);
		return new MockHttpServletRequest(servletContext);
	}

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
		Object result = this.mvc.invoke(this.request, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void down() {
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		Object result = this.mvc.invoke(this.request, null);
		assertThat(result instanceof ResponseEntity).isTrue();
		ResponseEntity<Health> response = (ResponseEntity<Health>) result;
		assertThat(response.getBody().getStatus() == Status.DOWN).isTrue();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void customMapping() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().status("OK").build());
		this.mvc.setStatusMapping(
				Collections.singletonMap("OK", HttpStatus.INTERNAL_SERVER_ERROR));
		Object result = this.mvc.invoke(this.request, null);
		assertThat(result instanceof ResponseEntity).isTrue();
		ResponseEntity<Health> response = (ResponseEntity<Health>) result;
		assertThat(response.getBody().getStatus().equals(new Status("OK"))).isTrue();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void customMappingWithRelaxedName() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().outOfService().build());
		this.mvc.setStatusMapping(Collections.singletonMap("out-of-service",
				HttpStatus.INTERNAL_SERVER_ERROR));
		Object result = this.mvc.invoke(this.request, null);
		assertThat(result instanceof ResponseEntity).isTrue();
		ResponseEntity<Health> response = (ResponseEntity<Health>) result;
		assertThat(response.getBody().getStatus().equals(Status.OUT_OF_SERVICE)).isTrue();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void presenceOfRightRoleShouldExposeDetails() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.defaultUser, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isEqualTo("bar");
	}

	@Test
	public void managementSecurityDisabledShouldExposeDetails() throws Exception {
		this.mvc = new HealthMvcEndpoint(this.endpoint, false);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.defaultUser, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isEqualTo("bar");
	}

	@Test
	public void rightRoleNotPresentShouldNotExposeDetails() {
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.hero, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isNull();
	}

	@Test
	public void rightAuthorityPresentShouldExposeDetails() throws Exception {
		this.environment.getPropertySources().addLast(SECURITY_ROLES);
		Authentication principal = mock(Authentication.class);
		Set<SimpleGrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("HERO"));
		doReturn(authorities).when(principal).getAuthorities();
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.defaultUser, principal);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isEqualTo("bar");
	}

	@Test
	public void customRolePresentShouldExposeDetails() {
		this.environment.getPropertySources().addLast(SECURITY_ROLES);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.hero, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isEqualTo("bar");
	}

	@Test
	public void customRoleShouldNotExposeDetailsForDefaultRole() {
		this.environment.getPropertySources().addLast(SECURITY_ROLES);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.defaultUser, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isNull();
	}

	@Test
	public void customRoleFromListShouldExposeDetails() {
		// gh-8314
		this.mvc = new HealthMvcEndpoint(this.endpoint, true,
				Arrays.asList("HERO", "USER"));
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.hero, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isEqualTo("bar");
	}

	@Test
	public void customRoleFromListShouldNotExposeDetailsForDefaultRole() {
		// gh-8314
		this.mvc = new HealthMvcEndpoint(this.endpoint, true,
				Arrays.asList("HERO", "USER"));
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.defaultUser, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isNull();
	}

	@Test
	public void healthIsCached() {
		given(this.endpoint.getTimeToLive()).willReturn(10000L);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.defaultUser, null);
		assertThat(result instanceof Health).isTrue();
		Health health = (Health) result;
		assertThat(health.getStatus() == Status.UP).isTrue();
		assertThat(health.getDetails()).hasSize(1);
		assertThat(health.getDetails().get("foo")).isEqualTo("bar");
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		result = this.mvc.invoke(this.request, null); // insecure now
		assertThat(result instanceof Health).isTrue();
		health = (Health) result;
		// so the result is cached
		assertThat(health.getStatus() == Status.UP).isTrue();
		// but the details are hidden
		assertThat(health.getDetails()).isEmpty();
	}

	@Test
	public void noCachingWhenTimeToLiveIsZero() {
		given(this.endpoint.getTimeToLive()).willReturn(0L);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.request, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		result = this.mvc.invoke(this.request, null);
		@SuppressWarnings("unchecked")
		Health health = ((ResponseEntity<Health>) result).getBody();
		assertThat(health.getStatus() == Status.DOWN).isTrue();
	}

	@Test
	public void newValueIsReturnedOnceTtlExpires() throws InterruptedException {
		given(this.endpoint.getTimeToLive()).willReturn(50L);
		given(this.endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		Object result = this.mvc.invoke(this.request, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		Thread.sleep(100);
		given(this.endpoint.invoke()).willReturn(new Health.Builder().down().build());
		result = this.mvc.invoke(this.request, null);
		@SuppressWarnings("unchecked")
		Health health = ((ResponseEntity<Health>) result).getBody();
		assertThat(health.getStatus() == Status.DOWN).isTrue();
	}

}
