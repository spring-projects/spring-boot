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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

/**
 * An {@link org.springframework.context.ApplicationListener} that saves
 * embedded server port into file. This application listener will be triggered
 * exactly once per JVM.
 *
 * @author David Liu
 * @since 1.2.0
 */
public class EmbeddedServerPortListener implements ApplicationListener<ApplicationStartedEvent> {

	private static final Log logger = LogFactory.getLog(EmbeddedServerPortListener.class);

	private static final String DEFAULT_FILE_NAME = "server.port";

	private static final AtomicBoolean created = new AtomicBoolean(false);

	private final File file;

	/**
	 * Create a new {@link EmbeddedServerPortListener} instance using the
	 * filename 'server.port'.
	 */
	public EmbeddedServerPortListener() {
		this.file = new File(DEFAULT_FILE_NAME);
	}

	/**
	 * Create a new {@link EmbeddedServerPortListener} instance with a specified
	 * filename.
	 * @param filename the name of file containing port
	 */
	public EmbeddedServerPortListener(String filename) {
		Assert.notNull(filename, "Filename must not be null");
		this.file = new File(filename);
	}

	/**
	 * Create a new {@link EmbeddedServerPortListener} instance with a specified
	 * file.
	 * @param file the file containing port
	 */
	public EmbeddedServerPortListener(File file) {
		Assert.notNull(file, "File must not be null");
		this.file = file;
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		if (event.getSource() instanceof AnnotationConfigEmbeddedWebApplicationContext) {
			EmbeddedServletContainer embeddedServletContainer = ((AnnotationConfigEmbeddedWebApplicationContext) event
					.getSource()).getEmbeddedServletContainer();
			if (created.compareAndSet(false, true)) {
				try {
					FileWriter writer = new FileWriter(this.file);
					try {
						writer.append("" + embeddedServletContainer.getPort());
					}
					finally {
						writer.close();
					}
					this.file.deleteOnExit();
				}
				catch (Exception ex) {
					logger.warn(String.format("Cannot create port file %s", this.file));
				}
			}
		}
	}

	/**
	 * Reset the created flag for testing purposes.
	 */
	static void reset() {
		created.set(false);
	}
}
