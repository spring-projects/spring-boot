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

package org.springframework.boot.actuate.autoconfigure.health;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfiguredHealthEndpointGroup}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class AutoConfiguredHealthEndpointGroupTests {

	@Mock
	private StatusAggregator statusAggregator;

	@Mock
	private HttpCodeStatusMapper httpCodeStatusMapper;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Principal principal;

	@Test
	void isMemberWhenMemberPredicateMatchesAcceptsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> name.startsWith("a"),
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.ALWAYS, Collections.emptySet(), null);
		assertThat(group.isMember("albert")).isTrue();
		assertThat(group.isMember("arnold")).isTrue();
	}

	@Test
	void isMemberWhenMemberPredicateRejectsReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> name.startsWith("a"),
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.ALWAYS, Collections.emptySet(), null);
		assertThat(group.isMember("bert")).isFalse();
		assertThat(group.isMember("ernie")).isFalse();
	}

	@Test
	void showDetailsWhenShowDetailsIsNeverReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.NEVER, Collections.emptySet(), null);
		assertThat(group.showDetails(SecurityContext.NONE)).isFalse();
	}

	@Test
	void showDetailsWhenShowDetailsIsAlwaysReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.ALWAYS, Collections.emptySet(), null);
		assertThat(group.showDetails(SecurityContext.NONE)).isTrue();
	}

	@Test
	void showDetailsWhenShowDetailsIsWhenAuthorizedAndPrincipalIsNullReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.WHEN_AUTHORIZED, Collections.emptySet(),
				null);
		given(this.securityContext.getPrincipal()).willReturn(null);
		assertThat(group.showDetails(this.securityContext)).isFalse();
	}

	@Test
	void showDetailsWhenShowDetailsIsWhenAuthorizedAndRolesAreEmptyReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.WHEN_AUTHORIZED, Collections.emptySet(),
				null);
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		assertThat(group.showDetails(this.securityContext)).isTrue();
	}

	@Test
	void showDetailsWhenShowDetailsIsWhenAuthorizedAndUseIsInRoleReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.WHEN_AUTHORIZED,
				Arrays.asList("admin", "root", "bossmode"), null);
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		given(this.securityContext.isUserInRole("admin")).willReturn(false);
		given(this.securityContext.isUserInRole("root")).willReturn(true);
		assertThat(group.showDetails(this.securityContext)).isTrue();
	}

	@Test
	void showDetailsWhenShowDetailsIsWhenAuthorizedAndUserIsNotInRoleReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.WHEN_AUTHORIZED,
				Arrays.asList("admin", "root", "bossmode"), null);
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		assertThat(group.showDetails(this.securityContext)).isFalse();
	}

	@Test
	void showDetailsWhenShowDetailsIsWhenAuthorizedAndUserHasRightAuthorityReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.WHEN_AUTHORIZED,
				Arrays.asList("admin", "root", "bossmode"), null);
		Authentication principal = mock(Authentication.class);
		given(principal.getAuthorities())
			.willAnswer((invocation) -> Collections.singleton(new SimpleGrantedAuthority("admin")));
		given(this.securityContext.getPrincipal()).willReturn(principal);
		assertThat(group.showDetails(this.securityContext)).isTrue();
	}

	@Test
	void showDetailsWhenShowDetailsIsWhenAuthorizedAndUserDoesNotHaveRightAuthoritiesReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.WHEN_AUTHORIZED,
				Arrays.asList("admin", "rot", "bossmode"), null);
		Authentication principal = mock(Authentication.class);
		given(principal.getAuthorities())
			.willAnswer((invocation) -> Collections.singleton(new SimpleGrantedAuthority("other")));
		given(this.securityContext.getPrincipal()).willReturn(principal);
		assertThat(group.showDetails(this.securityContext)).isFalse();
	}

	@Test
	void showComponentsWhenShowComponentsIsNullDelegatesToShowDetails() {
		AutoConfiguredHealthEndpointGroup alwaysGroup = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.ALWAYS, Collections.emptySet(), null);
		assertThat(alwaysGroup.showComponents(SecurityContext.NONE)).isTrue();
		AutoConfiguredHealthEndpointGroup neverGroup = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.NEVER, Collections.emptySet(), null);
		assertThat(neverGroup.showComponents(SecurityContext.NONE)).isFalse();
	}

	@Test
	void showComponentsWhenShowComponentsIsNeverReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.NEVER, Show.ALWAYS, Collections.emptySet(),
				null);
		assertThat(group.showComponents(SecurityContext.NONE)).isFalse();
	}

	@Test
	void showComponentsWhenShowComponentsIsAlwaysReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.ALWAYS, Show.NEVER, Collections.emptySet(),
				null);
		assertThat(group.showComponents(SecurityContext.NONE)).isTrue();
	}

	@Test
	void showComponentsWhenShowComponentsIsWhenAuthorizedAndPrincipalIsNullReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.WHEN_AUTHORIZED, Show.NEVER,
				Collections.emptySet(), null);
		given(this.securityContext.getPrincipal()).willReturn(null);
		assertThat(group.showComponents(this.securityContext)).isFalse();
	}

	@Test
	void showComponentsWhenShowComponentsIsWhenAuthorizedAndRolesAreEmptyReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.WHEN_AUTHORIZED, Show.NEVER,
				Collections.emptySet(), null);
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		assertThat(group.showComponents(this.securityContext)).isTrue();
	}

	@Test
	void showComponentsWhenShowComponentsIsWhenAuthorizedAndUseIsInRoleReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.WHEN_AUTHORIZED, Show.NEVER,
				Arrays.asList("admin", "root", "bossmode"), null);
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		given(this.securityContext.isUserInRole("admin")).willReturn(false);
		given(this.securityContext.isUserInRole("root")).willReturn(true);
		assertThat(group.showComponents(this.securityContext)).isTrue();
	}

	@Test
	void showComponentsWhenShowComponentsIsWhenAuthorizedAndUserIsNotInRoleReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.WHEN_AUTHORIZED, Show.NEVER,
				Arrays.asList("admin", "rot", "bossmode"), null);
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		assertThat(group.showComponents(this.securityContext)).isFalse();
	}

	@Test
	void showComponentsWhenShowComponentsIsWhenAuthorizedAndUserHasRightAuthoritiesReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.WHEN_AUTHORIZED, Show.NEVER,
				Arrays.asList("admin", "root", "bossmode"), null);
		Authentication principal = mock(Authentication.class);
		given(principal.getAuthorities())
			.willAnswer((invocation) -> Collections.singleton(new SimpleGrantedAuthority("admin")));
		given(this.securityContext.getPrincipal()).willReturn(principal);
		assertThat(group.showComponents(this.securityContext)).isTrue();
	}

	@Test
	void showComponentsWhenShowComponentsIsWhenAuthorizedAndUserDoesNotHaveRightAuthoritiesReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, Show.WHEN_AUTHORIZED, Show.NEVER,
				Arrays.asList("admin", "rot", "bossmode"), null);
		Authentication principal = mock(Authentication.class);
		given(principal.getAuthorities())
			.willAnswer((invocation) -> Collections.singleton(new SimpleGrantedAuthority("other")));
		given(this.securityContext.getPrincipal()).willReturn(principal);
		assertThat(group.showComponents(this.securityContext)).isFalse();
	}

	@Test
	void getStatusAggregatorReturnsStatusAggregator() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.ALWAYS, Collections.emptySet(), null);
		assertThat(group.getStatusAggregator()).isSameAs(this.statusAggregator);
	}

	@Test
	void getHttpCodeStatusMapperReturnsHttpCodeStatusMapper() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, null, Show.ALWAYS, Collections.emptySet(), null);
		assertThat(group.getHttpCodeStatusMapper()).isSameAs(this.httpCodeStatusMapper);
	}

}
