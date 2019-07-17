/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.io.IOException;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.annotation.WebListener;

import org.junit.Test;

import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

/**
 * Tests for {@link WebListenerHandler}.
 *
 * @author Andy Wilkinson
 */
public class WebListenerHandlerTests {

	private final WebListenerHandler handler = new WebListenerHandler();

	private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

	@Test
	public void listener() throws IOException {
		ScannedGenericBeanDefinition scanned = new ScannedGenericBeanDefinition(
				new SimpleMetadataReaderFactory().getMetadataReader(TestListener.class.getName()));
		this.handler.handle(scanned, this.registry);
		this.registry.getBeanDefinition(TestListener.class.getName());
	}

	@WebListener
	static class TestListener implements ServletContextAttributeListener {

		@Override
		public void attributeAdded(ServletContextAttributeEvent event) {

		}

		@Override
		public void attributeRemoved(ServletContextAttributeEvent event) {

		}

		@Override
		public void attributeReplaced(ServletContextAttributeEvent event) {

		}

	}

}
