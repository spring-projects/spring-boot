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

package org.springframework.boot.logging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.LoggerConfiguration.ConfigurationScope;
import org.springframework.boot.logging.LoggerConfiguration.LevelConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link LoggerConfiguration}.
 *
 * @author Phillip Webb
 */
class LoggerConfigurationTests {

	@Test
	void createWithLogLevelWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LoggerConfiguration(null, null, LogLevel.DEBUG))
			.withMessage("Name must not be null");
	}

	@Test
	void createWithLogLevelWhenEffectiveLevelIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new LoggerConfiguration("test", null, (LogLevel) null))
			.withMessage("EffectiveLevel must not be null");
	}

	@Test
	void createWithLevelConfigurationWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new LoggerConfiguration(null, null, LevelConfiguration.of(LogLevel.DEBUG)))
			.withMessage("Name must not be null");
	}

	@Test
	void createWithLevelConfigurationWhenInheritedLevelConfigurationIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new LoggerConfiguration("test", null, (LevelConfiguration) null))
			.withMessage("InheritedLevelConfiguration must not be null");
	}

	@Test
	void getNameReturnsName() {
		LoggerConfiguration configuration = new LoggerConfiguration("test", null,
				LevelConfiguration.of(LogLevel.DEBUG));
		assertThat(configuration.getName()).isEqualTo("test");
	}

	@Test
	void getConfiguredLevelWhenConfiguredReturnsLevel() {
		LoggerConfiguration configuration = new LoggerConfiguration("test", LevelConfiguration.of(LogLevel.DEBUG),
				LevelConfiguration.of(LogLevel.DEBUG));
		assertThat(configuration.getConfiguredLevel()).isEqualTo(LogLevel.DEBUG);
	}

	@Test
	void getConfiguredLevelWhenNotConfiguredReturnsNull() {
		LoggerConfiguration configuration = new LoggerConfiguration("test", null,
				LevelConfiguration.of(LogLevel.DEBUG));
		assertThat(configuration.getConfiguredLevel()).isNull();
	}

	@Test
	void getEffectiveLevelReturnsEffectiveLevel() {
		LoggerConfiguration configuration = new LoggerConfiguration("test", null,
				LevelConfiguration.of(LogLevel.DEBUG));
		assertThat(configuration.getEffectiveLevel()).isEqualTo(LogLevel.DEBUG);
	}

	@Test
	void getLevelConfigurationWithDirectScopeWhenConfiguredReturnsConfiguration() {
		LevelConfiguration assigned = LevelConfiguration.of(LogLevel.DEBUG);
		LoggerConfiguration configuration = new LoggerConfiguration("test", assigned,
				LevelConfiguration.of(LogLevel.DEBUG));
		assertThat(configuration.getLevelConfiguration(ConfigurationScope.DIRECT)).isEqualTo(assigned);
	}

	@Test
	void getLevelConfigurationWithDirectScopeWhenNotConfiguredReturnsNull() {
		LoggerConfiguration configuration = new LoggerConfiguration("test", null,
				LevelConfiguration.of(LogLevel.DEBUG));
		assertThat(configuration.getLevelConfiguration(ConfigurationScope.DIRECT)).isNull();
	}

	@Test
	void getLevelConfigurationWithInheritedScopeReturnsConfiguration() {
		LevelConfiguration effective = LevelConfiguration.of(LogLevel.DEBUG);
		LoggerConfiguration configuration = new LoggerConfiguration("test", null, effective);
		assertThat(configuration.getLevelConfiguration(ConfigurationScope.INHERITED)).isEqualTo(effective);
	}

	/**
	 * Tests for {@link LevelConfiguration}.
	 */
	@Nested
	class LevelConfigurationTests {

		@Test
		void ofWhenLogLevelIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> LevelConfiguration.of(null))
				.withMessage("LogLevel must not be null");
		}

		@Test
		void ofCreatesConfiguration() {
			LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
			assertThat(configuration.getLevel()).isEqualTo(LogLevel.DEBUG);
		}

		@Test
		void ofCustomWhenNameIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> LevelConfiguration.ofCustom(null))
				.withMessage("Name must not be empty");
		}

		@Test
		void ofCustomWhenNameIsEmptyThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> LevelConfiguration.ofCustom(""))
				.withMessage("Name must not be empty");
		}

		@Test
		void ofCustomCreatesConfiguration() {
			LevelConfiguration configuration = LevelConfiguration.ofCustom("FINE");
			assertThat(configuration).isNotNull();
		}

		@Test
		void getNameWhenFromLogLevelReturnsName() {
			LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
			assertThat(configuration.getName()).isEqualTo("DEBUG");
		}

		@Test
		void getNameWhenCustomReturnsName() {
			LevelConfiguration configuration = LevelConfiguration.ofCustom("FINE");
			assertThat(configuration.getName()).isEqualTo("FINE");
		}

		@Test
		void getLevelWhenCustomThrowsException() {
			LevelConfiguration configuration = LevelConfiguration.ofCustom("FINE");
			assertThatIllegalStateException().isThrownBy(() -> configuration.getLevel())
				.withMessage("Unable to provide LogLevel for 'FINE'");
		}

		@Test
		void getLevelReturnsLevel() {
			LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
			assertThat(configuration.getLevel()).isEqualTo(LogLevel.DEBUG);
		}

		@Test
		void isCustomWhenNotCustomReturnsFalse() {
			LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
			assertThat(configuration.isCustom()).isFalse();
		}

		@Test
		void isCustomWhenCustomReturnsTrue() {
			LevelConfiguration configuration = LevelConfiguration.ofCustom("DEBUG");
			assertThat(configuration.isCustom()).isTrue();
		}

	}

}
