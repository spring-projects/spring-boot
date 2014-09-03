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

import java.io.File;
import java.io.FileReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.util.FileCopyUtils;

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
		EmbeddedServletContainer container = mock(TomcatEmbeddedServletContainer.class);
		doReturn(9911).when(container).getPort();
		AnnotationConfigEmbeddedWebApplicationContext context = mock(AnnotationConfigEmbeddedWebApplicationContext.class);
		doReturn(container).when(context).getEmbeddedServletContainer();
		ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
		doReturn(context).when(event).getSource();
		File file = this.temporaryFolder.newFile();
		EmbeddedServerPortListener listener = new EmbeddedServerPortListener(file);
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), not(isEmptyString()));
	}

}
