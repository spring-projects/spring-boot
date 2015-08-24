/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools;

import org.springframework.boot.Banner;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.devtools.remote.client.RemoteClientConfiguration;
import org.springframework.boot.devtools.restart.RestartInitializer;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.core.io.ClassPathResource;

/**
 * Application that can be used to establish a link to remotely running Spring Boot code.
 * Allows remote debugging and remote updates (if enabled). This class should be launched
 * from within your IDE and should have the same classpath configuration as the locally
 * developed application. The remote URL of the application should be provided as a
 * non-option argument.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see RemoteClientConfiguration
 */
public class RemoteSpringApplication {

	private void run(String[] args) {
		Restarter.initialize(args, RestartInitializer.NONE);
		SpringApplication application = new SpringApplication(
				RemoteClientConfiguration.class);
		application.setWebEnvironment(false);
		application.setBanner(getBanner());
		application.addListeners(new RemoteUrlPropertyExtractor());
		application.run(args);
		waitIndefinitely();
	}

	private Banner getBanner() {
		ClassPathResource banner = new ClassPathResource("remote-banner.txt",
				RemoteSpringApplication.class);
		return new ResourceBanner(banner);
	}

	private void waitIndefinitely() {
		while (true) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
			}
		}
	}

	/**
	 * Run the {@link RemoteSpringApplication}.
	 * @param args the program arguments (including the remote URL as a non-option
	 * argument)
	 */
	public static void main(String[] args) {
		new RemoteSpringApplication().run(args);
	}

}
