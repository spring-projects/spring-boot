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

import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties.ShowDetails;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link AutoConfiguredHealthEndpointSettings}.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthEndpointSettingsTests {

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
	void includeDetailsWhenShowDetailsIsNeverReturnsFalse() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.NEVER, Collections.emptySet());
		assertThat(settings.includeDetails(SecurityContext.NONE)).isFalse();
	}

	@Test
	void includeDetailsWhenShowDetailsIsAlwaysReturnsTrue() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(settings.includeDetails(SecurityContext.NONE)).isTrue();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndPrincipalIsNullReturnsFalse() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED, Collections.emptySet());
		given(this.securityContext.getPrincipal()).willReturn(null);
		assertThat(settings.includeDetails(this.securityContext)).isFalse();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndRolesAreEmptyReturnsTrue() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED, Collections.emptySet());
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		assertThat(settings.includeDetails(this.securityContext)).isTrue();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndUseIsInRoleReturnsTrue() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED, Arrays.asList("admin", "root", "bossmode"));
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		given(this.securityContext.isUserInRole("root")).willReturn(true);
		assertThat(settings.includeDetails(this.securityContext)).isTrue();
	}

	@Test
	void includeDetailsWhenShowDetailsIsWhenAuthorizedAndUseIsNotInRoleReturnsFalse() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.WHEN_AUTHORIZED, Arrays.asList("admin", "rot", "bossmode"));
		given(this.securityContext.getPrincipal()).willReturn(this.principal);
		given(this.securityContext.isUserInRole("root")).willReturn(true);
		assertThat(settings.includeDetails(this.securityContext)).isFalse();
	}

	@Test
	void getStatusAggregatorReturnsStatusAggregator() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(settings.getStatusAggregator()).isSameAs(this.statusAggregator);
	}

	@Test
	void getHttpCodeStatusMapperReturnsHttpCodeStatusMapper() {
		AutoConfiguredHealthEndpointSettings settings = new AutoConfiguredHealthEndpointSettings(this.statusAggregator,
				this.httpCodeStatusMapper, ShowDetails.ALWAYS, Collections.emptySet());
		assertThat(settings.getHttpCodeStatusMapper()).isSameAs(this.httpCodeStatusMapper);
	}

}
