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

package org.springframework.boot.autoconfigure.jms;

import static org.junit.Assert.*;

import java.io.File;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.server.JournalType;
import org.junit.Test;

/**
 *
 * @author Stephane Nicoll
 */
public class HornetQPropertiesTests {

	@Test
	public void defaultDataDir() {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setPersistent(true);

		Configuration configuration = new ConfigurationImpl();
		properties.getEmbedded().configure(configuration);

		String expectedRoot = HornetQProperties.Embedded.createDataDir();
		assertEquals("Wrong journal dir", new File(expectedRoot, "journal"),
				new File(configuration.getJournalDirectory()));
	}

	@Test
	public void persistenceSetup() {
		HornetQProperties properties = new HornetQProperties();
		properties.getEmbedded().setPersistent(true);

		Configuration configuration = new ConfigurationImpl();
		properties.getEmbedded().configure(configuration);

		assertTrue(configuration.isPersistenceEnabled());
		assertEquals(JournalType.NIO, configuration.getJournalType());
	}
}
