/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for {@link MockitoTestExecutionListener}.
 *
 * @author Moritz Halbritter
 * @deprecated since 3.4.0 for removal in 3.6.0
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.4.0", forRemoval = true)
@ExtendWith(SpringExtension.class)
class MockitoTestExecutionListenerIntegrationTests {

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class MockedStaticTests {

		private static final UUID uuid = UUID.randomUUID();

		@Mock
		private MockedStatic<UUID> mockedStatic;

		@Test
		@Order(1)
		@Disabled
		void shouldReturnConstantValueDisabled() {
			this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			UUID result = UUID.randomUUID();
			assertThat(result).isEqualTo(uuid);
		}

		@Test
		@Order(2)
		void shouldNotFailBecauseOfMockedStaticNotBeingClosed() {
			this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			UUID result = UUID.randomUUID();
			assertThat(result).isEqualTo(uuid);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
	class MockedStaticTestsDirtiesContext {

		private static final UUID uuid = UUID.randomUUID();

		@Mock
		private MockedStatic<UUID> mockedStatic;

		@Test
		@Order(1)
		@Disabled
		void shouldReturnConstantValueDisabled() {
			this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			UUID result = UUID.randomUUID();
			assertThat(result).isEqualTo(uuid);
		}

		@Test
		@Order(2)
		void shouldNotFailBecauseOfMockedStaticNotBeingClosed() {
			this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			UUID result = UUID.randomUUID();
			assertThat(result).isEqualTo(uuid);
		}

		@Test
		@Order(3)
		void shouldNotFailBecauseOfMockedStaticNotBeingClosedWhenMocksAreReinjected() {
			this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			UUID result = UUID.randomUUID();
			assertThat(result).isEqualTo(uuid);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	class MockedStaticTestsIfClassContainsOnlyDisabledTests {

		@Nested
		@Order(1)
		class TestClass1 {

			private static final UUID uuid = UUID.randomUUID();

			@Mock
			private MockedStatic<UUID> mockedStatic;

			@Test
			@Order(1)
			@Disabled
			void disabledTest() {
				this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			}

		}

		@Nested
		@Order(2)
		class TestClass2 {

			private static final UUID uuid = UUID.randomUUID();

			@Mock
			private MockedStatic<UUID> mockedStatic;

			@Test
			@Order(1)
			void shouldNotFailBecauseMockedStaticHasNotBeenClosed() {
				this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
				UUID result = UUID.randomUUID();
				assertThat(result).isEqualTo(uuid);
			}

		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	class MockedStaticTestsIfClassContainsNoTests {

		@Nested
		@Order(1)
		class TestClass1 {

			@Mock
			private MockedStatic<UUID> mockedStatic;

		}

		@Nested
		@Order(2)
		class TestClass2 {

			private static final UUID uuid = UUID.randomUUID();

			@Mock
			private MockedStatic<UUID> mockedStatic;

			@Test
			@Order(1)
			void shouldNotFailBecauseMockedStaticHasNotBeenClosed() {
				this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
				UUID result = UUID.randomUUID();
				assertThat(result).isEqualTo(uuid);
			}

		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ConfigureMockInBeforeEach {

		@Mock
		private List<String> mock;

		@BeforeEach
		void setUp() {
			given(this.mock.size()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldUseSetUpConfiguration() {
			assertThat(this.mock.size()).isEqualTo(1);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.size()).willReturn(2);
			assertThat(this.mock.size()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldNotBeAffectedByOtherTests() {
			assertThat(this.mock.size()).isEqualTo(1);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@TestInstance(Lifecycle.PER_CLASS)
	@Disabled("https://github.com/spring-projects/spring-framework/issues/33690")
	class ConfigureMockInBeforeAll {

		@Mock
		private List<String> mock;

		@BeforeAll
		void setUp() {
			given(this.mock.size()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldUseSetUpConfiguration() {
			assertThat(this.mock.size()).isEqualTo(1);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.size()).willReturn(2);
			assertThat(this.mock.size()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldNotBeAffectedByOtherTest() {
			assertThat(this.mock.size()).isEqualTo(2);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@Import(MyBeanConfiguration.class)
	class ConfigureMockBeanWithResetAfterInBeforeEach {

		@MockBean(reset = MockReset.AFTER)
		private MyBean mock;

		@BeforeEach
		void setUp() {
			given(this.mock.call()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldUseSetUpConfiguration() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.call()).willReturn(2);
			assertThat(this.mock.call()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldNotBeAffectedByOtherTests() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@Import(MyBeanConfiguration.class)
	class ConfigureMockBeanWithResetBeforeInBeforeEach {

		@MockBean(reset = MockReset.BEFORE)
		private MyBean mock;

		@BeforeEach
		void setUp() {
			given(this.mock.call()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldUseSetUpConfiguration() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.call()).willReturn(2);
			assertThat(this.mock.call()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldNotBeAffectedByOtherTests() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@Import(MyBeanConfiguration.class)
	class ConfigureMockBeanWithResetNoneInBeforeEach {

		@MockBean(reset = MockReset.NONE)
		private MyBean mock;

		@BeforeEach
		void setUp() {
			given(this.mock.call()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldUseSetUpConfiguration() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.call()).willReturn(2);
			assertThat(this.mock.call()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldNotBeAffectedByOtherTests() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@TestInstance(Lifecycle.PER_CLASS)
	@Import(MyBeanConfiguration.class)
	class ConfigureMockBeanWithResetAfterInBeforeAll {

		@MockBean(reset = MockReset.AFTER)
		private MyBean mock;

		@BeforeAll
		void setUp() {
			given(this.mock.call()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldUseSetUpConfiguration() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.call()).willReturn(2);
			assertThat(this.mock.call()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldResetMockAfterReconfiguration() {
			assertThat(this.mock.call()).isEqualTo(0);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@TestInstance(Lifecycle.PER_CLASS)
	@Import(MyBeanConfiguration.class)
	class ConfigureMockBeanWithResetBeforeInBeforeAll {

		@MockBean(reset = MockReset.BEFORE)
		private MyBean mock;

		@BeforeAll
		void setUp() {
			given(this.mock.call()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldResetMockBeforeThisMethod() {
			assertThat(this.mock.call()).isEqualTo(0);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.call()).willReturn(2);
			assertThat(this.mock.call()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldResetMockAfterReconfiguration() {
			assertThat(this.mock.call()).isEqualTo(0);
		}

	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@TestInstance(Lifecycle.PER_CLASS)
	@Import(MyBeanConfiguration.class)
	class ConfigureMockBeanWithResetNoneInBeforeAll {

		@MockBean(reset = MockReset.NONE)
		private MyBean mock;

		@BeforeAll
		void setUp() {
			given(this.mock.call()).willReturn(1);
		}

		@Test
		@Order(1)
		void shouldUseSetUpConfiguration() {
			assertThat(this.mock.call()).isEqualTo(1);
		}

		@Test
		@Order(2)
		void shouldBeAbleToReconfigureMock() {
			given(this.mock.call()).willReturn(2);
			assertThat(this.mock.call()).isEqualTo(2);
		}

		@Test
		@Order(3)
		void shouldNotResetMock() {
			assertThat(this.mock.call()).isEqualTo(2);
		}

	}

	interface MyBean {

		int call();

	}

	private static final class DefaultMyBean implements MyBean {

		@Override
		public int call() {
			return -1;
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	private static final class MyBeanConfiguration {

		@Bean
		MyBean myBean() {
			return new DefaultMyBean();
		}

	}

}
