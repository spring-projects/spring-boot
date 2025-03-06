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

package org.springframework.boot.autoconfigure.data.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test configuration that mocks access to Cassandra.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
class CassandraMockConfiguration {

	final CodecRegistry codecRegistry = mock(CodecRegistry.class);

	@Bean
	CqlSession cqlSession() {
		DriverContext context = mock(DriverContext.class);
		given(context.getCodecRegistry()).willReturn(this.codecRegistry);
		CqlSession cqlSession = mock(CqlSession.class);
		given(cqlSession.getContext()).willReturn(context);
		return cqlSession;
	}

}
