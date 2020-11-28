/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link DataCassandraTest#properties properties} attribute of
 * {@link DataCassandraTest @DataCassandraTest}.
 *
 * @author Artsiom Yudovin
 */
@DataCassandraTest(properties = "spring.profiles.active=test")
class DataCassandraTestPropertiesIntegrationTests {

	@Autowired
	private Environment environment;

	@Test
	void environmentWithNewProfile() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class CassandraMockConfiguration {

		@Bean
		CqlSession cqlSession() {
			DriverContext context = mock(DriverContext.class);
			CodecRegistry codecRegistry = mock(CodecRegistry.class);
			given(context.getCodecRegistry()).willReturn(codecRegistry);
			CqlSession cqlSession = mock(CqlSession.class);
			given(cqlSession.getContext()).willReturn(context);
			return cqlSession;
		}

	}

}
