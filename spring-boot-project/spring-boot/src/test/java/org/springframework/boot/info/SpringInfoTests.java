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

package org.springframework.boot.info;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringInfo}.
 *
 * @author Jonatan Ivanov
 */
@ExtendWith(MockitoExtension.class)
class SpringInfoTests {

	private MockEnvironment environment;

	@BeforeEach
	void setUp() {
		this.environment = new MockEnvironment();
	}

	@Test
	void springInfoIsAvailableWithoutExplicitProfiles() {
		SpringInfo info = new SpringInfo(this.environment);
		assertThat(info.getFramework().getVersion()).isEqualTo(SpringVersion.getVersion());
		assertThat(info.getBoot().getVersion()).isEqualTo(SpringBootVersion.getVersion());
		assertThat(info.getProfiles()).containsExactly("default");
	}

	@Test
	void springInfoContainsDefaultProfilesIfDefaultProfileIsSet() {
		this.environment.setDefaultProfiles("test-default-profile");
		SpringInfo info = new SpringInfo(this.environment);
		assertThat(info.getProfiles()).containsExactly("test-default-profile");
	}

	@Test
	void springInfoContainsActiveProfilesIfActiveProfilesIsSet() {
		this.environment.setActiveProfiles("test-active-profile");
		SpringInfo info = new SpringInfo(this.environment);
		assertThat(info.getProfiles()).containsExactly("test-active-profile");
	}

	@Test
	void springInfoContainsActiveProfilesIfBothDefaultAndActiveProfilesAreSet() {
		this.environment.setDefaultProfiles("test-default-profile");
		this.environment.setActiveProfiles("test-active-profile");
		SpringInfo info = new SpringInfo(this.environment);
		assertThat(info.getProfiles()).containsExactly("test-active-profile");
	}

}
