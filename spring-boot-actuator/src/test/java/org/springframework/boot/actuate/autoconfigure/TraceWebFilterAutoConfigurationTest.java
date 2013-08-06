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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.TraceRepositoryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.TraceWebFilterAutoConfiguration;
import org.springframework.boot.actuate.trace.WebRequestTraceFilter;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link TraceWebFilterAutoConfiguration}.
 * 
 * @author Phillip Webb
 */
public class TraceWebFilterAutoConfigurationTest {

	@Test
	public void configureFilter() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class,
				TraceRepositoryAutoConfiguration.class,
				TraceWebFilterAutoConfiguration.class);
		assertNotNull(context.getBean(WebRequestTraceFilter.class));
		context.close();
	}

}
