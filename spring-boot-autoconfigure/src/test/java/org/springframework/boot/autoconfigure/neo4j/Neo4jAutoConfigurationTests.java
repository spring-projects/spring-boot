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

package org.springframework.boot.autoconfigure.neo4j;

import org.assertj.core.api.Assertions;

import org.junit.After;
import org.junit.Test;

import org.neo4j.ogm.config.Configuration;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests for {@link Neo4jAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Michael Hunger
 * @author Vince Bickers
 */
public class Neo4jAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void configurationExists() {
		this.context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class, Neo4jAutoConfiguration.class);
		Assertions.assertThat(this.context.getBeanNamesForType(Configuration.class).length).isEqualTo(1);
	}

}
