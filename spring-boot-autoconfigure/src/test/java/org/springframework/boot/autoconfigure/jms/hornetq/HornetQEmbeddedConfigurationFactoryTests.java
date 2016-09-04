/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jms.hornetq;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.JournalType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HornetQEmbeddedConfigurationFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@Deprecated
public class HornetQEmbeddedConfigurationFactoryTests {

	@Test
	public void defaultDataDir() {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setPersistent(true);
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.getJournalDirectory())
				.startsWith(System.getProperty("java.io.tmpdir")).endsWith("/journal");
	}

	@Test
	public void persistenceSetup() {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setPersistent(true);
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.isPersistenceEnabled()).isTrue();
		assertThat(configuration.getJournalType()).isEqualTo(JournalType.NIO);
	}

	@Test
	public void generatedClusterPassword() throws Exception {
		HornetQProperties properties = new HornetQProperties();
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.getClusterPassword().length()).isEqualTo(36);
	}

	@Test
	public void specificClusterPassword() throws Exception {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setClusterPassword("password");
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.getClusterPassword()).isEqualTo("password");
	}

}
