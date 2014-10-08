/*
 * Copyright 2010-2014 the original author or authors.
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

package org.springframework.boot.actuate.system;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.FileReader;

import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Tests {@link EmbeddedServerPortListener}.
 *
 * @author David Liu
 * @since 2.0
 */
public class EmbeddedServerPortListenerTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Before
	@After
	public void resetListener() {
		EmbeddedServerPortListener.reset();
	}

	@Test
	public void createPortFileTest() throws Exception {
		ManagementServerProperties properties = mock(ManagementServerProperties.class);
		properties.setPort(2011);
		EmbeddedServletContainer container = mock(TomcatEmbeddedServletContainer.class);
		ServletContext servletContext = mock(ServletContext.class);
		ApplicationContext mockApplicationContex = new GenericWebApplicationContext();
		doReturn(mockApplicationContex).when(servletContext).getAttribute(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) mockApplicationContex)
				.getBeanFactory();
		beanFactory.registerSingleton("managementServerProperties", properties);
		((ConfigurableApplicationContext) mockApplicationContex).refresh();
		doReturn(9911).when(container).getPort();
		AnnotationConfigEmbeddedWebApplicationContext context = mock(AnnotationConfigEmbeddedWebApplicationContext.class);
		doReturn(container).when(context).getEmbeddedServletContainer();
		doReturn(servletContext).when(context).getServletContext();
		ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
		doReturn(context).when(event).getSource();
		System.setProperty("APPLICATIONPORTFILE", this.temporaryFolder.newFile().getAbsolutePath());
		System.setProperty("MANAGEMENTPORTFILE", this.temporaryFolder.newFile().getAbsolutePath());
		EmbeddedServerPortListener listener = new EmbeddedServerPortListener();
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(System.getProperty("APPLICATIONPORTFILE"))),
				not(isEmptyString()));
		assertThat(FileCopyUtils.copyToString(new FileReader(System.getProperty("MANAGEMENTPORTFILE"))),
				not(isEmptyString()));
	}

}
