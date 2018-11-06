/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.cloud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationSorter;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudServiceConnectorsAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class CloudServiceConnectorsAutoConfigurationTests {

	@Test
	public void testOrder() {
		TestAutoConfigurationSorter sorter = new TestAutoConfigurationSorter(
				new CachingMetadataReaderFactory());
		Collection<String> classNames = new ArrayList<>();
		classNames.add(MongoAutoConfiguration.class.getName());
		classNames.add(DataSourceAutoConfiguration.class.getName());
		classNames.add(MongoRepositoriesAutoConfiguration.class.getName());
		classNames.add(JpaRepositoriesAutoConfiguration.class.getName());
		classNames.add(CloudServiceConnectorsAutoConfiguration.class.getName());
		List<String> ordered = sorter.getInPriorityOrder(classNames);
		assertThat(ordered.get(0))
				.isEqualTo(CloudServiceConnectorsAutoConfiguration.class.getName());
	}

}
