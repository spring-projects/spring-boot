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

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * An {@link org.springframework.context.ApplicationListener} that saves
 * embedded server port and management port into file. This application listener
 * will be triggered exactly once per JVM.
 *
 * @author David Liu
 * @since 1.2.0
 */
public class EmbeddedServerPortListener implements ApplicationListener<ApplicationStartedEvent> {

	private static final Log logger = LogFactory.getLog(EmbeddedServerPortListener.class);

	private static final String APPLICATION_PORT_FILE_NAME = "application.port";

	private static final String MANAGEMENT_PORT_FILE_NAME = "management.port";

	private static final AtomicBoolean applicationPortCreated = new AtomicBoolean(false);

	private static final AtomicBoolean managementPortCreated = new AtomicBoolean(false);

	private final File applicationFile;

	private final File managementFile;

	/**
	 * Create a new {@link EmbeddedServerPortListener} instance using the
	 * filename 'application.port' and 'management.port'.
	 */
	public EmbeddedServerPortListener() {
		this.applicationFile = new File(APPLICATION_PORT_FILE_NAME);
		this.managementFile = new File(MANAGEMENT_PORT_FILE_NAME);
	}

	/**
	 * Create a new {@link EmbeddedServerPortListener} instance with two
	 * specified filename.
	 * @param applicationFileName the name of file containing server port
	 * @param managementFileName the name of file containing management port
	 */
	public EmbeddedServerPortListener(String applicationFileName, String managementFileName) {
		Assert.notNull(applicationFileName, "ApplicationFileName must not be null");
		Assert.notNull(managementFileName, "ManagementFileName must not be null");
		this.applicationFile = new File(applicationFileName);
		this.managementFile = new File(managementFileName);
	}

	/**
	 * Create a new {@link EmbeddedServerPortListener} instance with two
	 * specified files.
	 * @param applicationFile the file containing server port
	 * @param managementFile the file containing managementFile port
	 */
	public EmbeddedServerPortListener(File applicationFile, File managementFile) {
		Assert.notNull(applicationFile, "ApplicationFile must not be null");
		Assert.notNull(managementFile, "ManagementFile must not be null");
		this.applicationFile = applicationFile;
		this.managementFile = managementFile;
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		if (event.getSource() instanceof AnnotationConfigEmbeddedWebApplicationContext) {
			EmbeddedServletContainer embeddedServletContainer = ((AnnotationConfigEmbeddedWebApplicationContext) event
					.getSource()).getEmbeddedServletContainer();
			if (applicationPortCreated.compareAndSet(false, true)) {
				try {
					FileWriter writer = new FileWriter(this.applicationFile);
					try {
						writer.append("" + embeddedServletContainer.getPort());
					}
					finally {
						writer.close();
					}
					this.applicationFile.deleteOnExit();
				}
				catch (Exception ex) {
					logger.warn(String.format("Cannot create application port file %s", this.applicationFile));
				}
			}
			if (managementPortCreated.compareAndSet(false, true)) {
				ServletContext servletContext = ((AnnotationConfigEmbeddedWebApplicationContext) event.getSource())
						.getServletContext();
				try {
					ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
					ManagementServerProperties bean = context.getBean(ManagementServerProperties.class);
					FileWriter writer = new FileWriter(this.managementFile);
					try {
						writer.append("" + bean.getPort());
					}
					finally {
						writer.close();
					}
					this.managementFile.deleteOnExit();
				}
				catch (Exception ex) {
					logger.warn(String.format("Cannot create management port file %s", this.managementFile));
				}
			}
		}
	}

	/**
	 * Reset the created flag for testing purposes.
	 */
	static void reset() {
		applicationPortCreated.set(false);
		managementPortCreated.set(false);
	}
}
