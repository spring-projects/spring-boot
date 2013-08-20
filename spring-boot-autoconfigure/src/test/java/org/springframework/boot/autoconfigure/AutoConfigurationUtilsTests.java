/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AutoConfigurationUtils}.
 * 
 * @author Phillip Webb
 */
public class AutoConfigurationUtilsTests {

	private ConfigurableListableBeanFactory beanFactory;

	@Before
	public void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
	}

	@Test
	public void storeAndGetBasePackages() throws Exception {
		List<String> packageList = Arrays.asList("com.mycorp.test1", "com.mycorp.test2");
		AutoConfigurationUtils.storeBasePackages(this.beanFactory, packageList);
		List<String> actual = AutoConfigurationUtils.getBasePackages(this.beanFactory);
		assertThat(actual, equalTo(packageList));
	}

	@Test
	public void doubleAdd() throws Exception {
		List<String> list1 = Arrays.asList("com.mycorp.test1", "com.mycorp.test2");
		List<String> list2 = Arrays.asList("com.mycorp.test2", "com.mycorp.test3");
		AutoConfigurationUtils.storeBasePackages(this.beanFactory, list1);
		AutoConfigurationUtils.storeBasePackages(this.beanFactory, list2);
		List<String> actual = AutoConfigurationUtils.getBasePackages(this.beanFactory);
		assertThat(actual, equalTo(Arrays.asList("com.mycorp.test1", "com.mycorp.test2",
				"com.mycorp.test3")));
	}

	@Test
	public void excludedPackages() throws Exception {
		List<String> packageList = Arrays.asList("com.mycorp.test1",
				"org.springframework.data.rest.webmvc");
		AutoConfigurationUtils.storeBasePackages(this.beanFactory, packageList);
		List<String> actual = AutoConfigurationUtils.getBasePackages(this.beanFactory);
		assertThat(actual, equalTo(Arrays.asList("com.mycorp.test1")));
	}
}
