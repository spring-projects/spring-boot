/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot;

/**
 * Provide application-related information such as the {@link ApplicationArguments} and
 * the {@link Banner}.
 *
 * @author Stephane Nicoll
 * @since 1.4.1
 */
public final class ApplicationInfo {

	private final Class<?> mainApplicationClass;

	private final ApplicationArguments applicationArguments;

	private final Banner banner;

	protected ApplicationInfo(SpringApplication application,
			ApplicationArguments applicationArguments, Banner banner) {
		this.mainApplicationClass = application.getMainApplicationClass();
		this.applicationArguments = applicationArguments;
		this.banner = banner;
	}

	/**
	 * Returns the main application class that has been deduced or explicitly configured.
	 * @return the main application class or {@code null}
	 */
	public Class<?> getMainApplicationClass() {
		return this.mainApplicationClass;
	}

	/**
	 * Returns the {@link ApplicationArguments} used to start this instance.
	 * @return the application arguments
	 */
	public ApplicationArguments getApplicationArguments() {
		return this.applicationArguments;
	}

	/**
	 * Returns the {@link Banner} used by this instance.
	 * @return the banner or {@code null}
	 */
	public Banner getBanner() {
		return this.banner;
	}

}
