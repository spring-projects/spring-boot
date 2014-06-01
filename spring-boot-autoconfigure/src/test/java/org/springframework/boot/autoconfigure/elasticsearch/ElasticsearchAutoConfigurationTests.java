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

package org.springframework.boot.autoconfigure.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ElasticsearchAutoConfiguration}.
 * 
 * @author Phillip Webb
 */
public class ElasticsearchAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createNodeClient() {
		this.context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class);
		assertEquals(1, this.context.getBeanNamesForType(Client.class).length);
		assertThat(this.context.getBean(Client.class), instanceOf(NodeClient.class));
	}

	@Test
	public void createTransportClient() throws Exception {
		// We don't have a local elasticsearch server so use an address that's missing
		// a port and check the exception
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.elasticsearch.cluster-nodes:localhost");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class);
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("port");
		this.context.refresh();
	}

}
