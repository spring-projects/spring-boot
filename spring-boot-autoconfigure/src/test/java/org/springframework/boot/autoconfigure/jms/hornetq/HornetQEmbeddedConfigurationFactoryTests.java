/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.boot.autoconfigure.jms.hornetq.HornetQEmbeddedConfigurationFactory;
import org.springframework.boot.autoconfigure.jms.hornetq.HornetQProperties;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HornetQEmbeddedConfigurationFactory}.
 * 
 * @author Stephane Nicol
 * @author Phillip Webb
 */
public class HornetQEmbeddedConfigurationFactoryTests {

	@Test
	public void defaultDataDir() {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setPersistent(true);
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.getJournalDirectory(),
				startsWith(System.getProperty("java.io.tmpdir")));
		assertThat(configuration.getJournalDirectory(), endsWith("/journal"));
	}

	@Test
	public void persistenceSetup() {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setPersistent(true);
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.isPersistenceEnabled(), equalTo(true));
		assertThat(configuration.getJournalType(), equalTo(JournalType.NIO));
	}

	@Test
	public void generatedClusterPassoword() throws Exception {
		HornetQProperties properties = new HornetQProperties();
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.getClusterPassword().length(), equalTo(36));
	}

	@Test
	public void specificClusterPassoword() throws Exception {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setClusterPassword("password");
		Configuration configuration = new HornetQEmbeddedConfigurationFactory(properties)
				.createConfiguration();
		assertThat(configuration.getClusterPassword(), equalTo("password"));
	}

}
