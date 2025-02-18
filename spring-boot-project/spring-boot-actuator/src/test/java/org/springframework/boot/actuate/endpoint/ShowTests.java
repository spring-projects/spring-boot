/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.security.Principal;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Show}.
 *
 * @author Madhura Bhave
 */
class ShowTests {

	@Test
	void isShownWhenNever() {
		assertThat(Show.NEVER.isShown(null, Collections.emptySet())).isFalse();
		assertThat(Show.NEVER.isShown(true)).isFalse();
		assertThat(Show.NEVER.isShown(false)).isFalse();
	}

	@Test
	void isShownWhenAlways() {
		assertThat(Show.ALWAYS.isShown(null, Collections.emptySet())).isTrue();
		assertThat(Show.ALWAYS.isShown(true)).isTrue();
		assertThat(Show.ALWAYS.isShown(true)).isTrue();
	}

	@Test
	void isShownWithUnauthorizedResult() {
		assertThat(Show.WHEN_AUTHORIZED.isShown(true)).isTrue();
		assertThat(Show.WHEN_AUTHORIZED.isShown(false)).isFalse();
	}

	@Test
	void isShownWhenUserNotInRole() {
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		given(securityContext.isUserInRole("admin")).willReturn(false);
		assertThat(Show.WHEN_AUTHORIZED.isShown(securityContext, Collections.singleton("admin"))).isFalse();
	}

	@Test
	void isShownWhenUserInRole() {
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		given(securityContext.isUserInRole("admin")).willReturn(true);
		assertThat(Show.WHEN_AUTHORIZED.isShown(securityContext, Collections.singleton("admin"))).isTrue();
	}

	@Test
	void isShownWhenPrincipalNull() {
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.isUserInRole("admin")).willReturn(true);
		assertThat(Show.WHEN_AUTHORIZED.isShown(securityContext, Collections.singleton("admin"))).isFalse();
	}

	@Test
	void isShownWhenRolesEmpty() {
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		assertThat(Show.WHEN_AUTHORIZED.isShown(securityContext, Collections.emptySet())).isTrue();
	}

	@Test
	void isShownWhenSpringSecurityAuthenticationAndUnauthorized() {
		SecurityContext securityContext = mock(SecurityContext.class);
		Authentication authentication = mock(Authentication.class);
		given(securityContext.getPrincipal()).willReturn(authentication);
		given(authentication.getAuthorities())
			.willAnswer((invocation) -> Collections.singleton(new SimpleGrantedAuthority("other")));
		assertThat(Show.WHEN_AUTHORIZED.isShown(securityContext, Collections.singleton("admin"))).isFalse();
	}

	@Test
	void isShownWhenSpringSecurityAuthenticationAndAuthorized() {
		SecurityContext securityContext = mock(SecurityContext.class);
		Authentication authentication = mock(Authentication.class);
		given(securityContext.getPrincipal()).willReturn(authentication);
		given(authentication.getAuthorities())
			.willAnswer((invocation) -> Collections.singleton(new SimpleGrantedAuthority("admin")));
		assertThat(Show.WHEN_AUTHORIZED.isShown(securityContext, Collections.singleton("admin"))).isTrue();
	}

}
