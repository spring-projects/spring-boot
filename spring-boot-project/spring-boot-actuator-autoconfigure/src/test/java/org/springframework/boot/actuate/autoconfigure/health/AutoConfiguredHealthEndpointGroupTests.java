/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.autoconfigure.health.HealthProperties.ShowDetails;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link AutoConfiguredHealthEndpointGroup}.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthEndpointGroupTests {

	@Mock
	private StatusAggregator statusAggregator;

	@Mock
	private HttpCodeStatusMapper httpCodeStatusMapper;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Principal principal;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void isMemberWhenMemberPredicateMatchesAcceptsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> name.startsWith("a"),
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(group.isMember("albert")).isTrue();
		assertThat(group.isMember("arnold")).isTrue();
	}

	@Test
	void isMemberWhenMemberPredicateRejectsReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> name.startsWith("a"),
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(group.isMember("bert")).isFalse();
		assertThat(group.isMember("ernie")).isFalse();
	}

	@Test
	void includeDetailsWhenShowDetailsIsNeverReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.NEVER, Collections.emptySet());
		assertThat(group.includeDetails(SecurityContext.NONE)).isFalse();
	}

	@Test
	void includeDetailsWhenShowDetailsIsAlwaysReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(group.includeDetails(SecurityContext.NONE)).isTrue();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndPrincipalIsNullReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED, Collections.emptySet());
		given(this.securityContext.getPrincipal()).willReturn(null);
		assertThat(group.includeDetails(this.securityContext)).isFalse();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndRolesAreEmptyReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED, Collections.emptySet());
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		assertThat(group.includeDetails(this.securityContext)).isTrue();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndUseIsInRoleReturnsTrue() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED,
				Arrays.asList("admin", "root", "bossmode"));
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		given(this.securityContext.isUserInRole("root")).willReturn(true);
		assertThat(group.includeDetails(this.securityContext)).isTrue();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndUseIsNotInRoleReturnsFalse() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED,
				Arrays.asList("admin", "rot", "bossmode"));
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		given(this.securityContext.isUserInRole("root")).willReturn(true);
		assertThat(group.includeDetails(this.securityContext)).isFalse();
	}

	@Test
	void getStatusAggregatorReturnsStatusAggregator() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(group.getStatusAggregator()).isSameAs(this.statusAggregator);
	}

	@Test
	void getHttpCodeStatusMapperReturnsHttpCodeStatusMapper() {
		AutoConfiguredHealthEndpointGroup group = new AutoConfiguredHealthEndpointGroup((name) -> true,
				this.statusAggregator, this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(group.getHttpCodeStatusMapper()).isSameAs(this.httpCodeStatusMapper);
	}

}
