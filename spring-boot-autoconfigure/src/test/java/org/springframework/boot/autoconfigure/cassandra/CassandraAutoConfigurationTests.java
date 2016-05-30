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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.driver.core.Cluster;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraAutoConfiguration}
 *
 * @author Eddú Meléndez
 */
public class CassandraAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createClusterWithDefault() {
		this.context = doLoad();
		assertThat(this.context.getBeanNamesForType(Cluster.class).length).isEqualTo(1);
		Cluster cluster = this.context.getBean(Cluster.class);
		assertThat(cluster.getClusterName()).startsWith("cluster");
	}

	@Test
	public void createClusterWithOverrides() {
		this.context = doLoad("spring.data.cassandra.cluster-name=testcluster");
		assertThat(this.context.getBeanNamesForType(Cluster.class).length).isEqualTo(1);
		Cluster cluster = this.context.getBean(Cluster.class);
		assertThat(cluster.getClusterName()).isEqualTo("testcluster");
	}

	private AnnotationConfigApplicationContext doLoad(String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}

}
