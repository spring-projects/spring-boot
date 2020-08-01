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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebListenerHandler}.
 *
 * @author Andy Wilkinson
 */
class WebListenerHandlerTests {

	private final WebListenerHandler handler = new WebListenerHandler();

	private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

	@Test
	void listener() throws IOException {
		AnnotatedBeanDefinition definition = mock(AnnotatedBeanDefinition.class);
		given(definition.getBeanClassName()).willReturn(TestListener.class.getName());
		given(definition.getMetadata()).willReturn(new SimpleMetadataReaderFactory()
				.getMetadataReader(TestListener.class.getName()).getAnnotationMetadata());
		this.handler.handle(definition, this.registry);
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
