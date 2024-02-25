/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Class used by {@link SpringApplication} to print the application banner.
 *
 * @author Phillip Webb
 */
class SpringApplicationBannerPrinter {

	static final String BANNER_LOCATION_PROPERTY = "spring.banner.location";

	static final String DEFAULT_BANNER_LOCATION = "banner.txt";

	private static final Banner DEFAULT_BANNER = new SpringBootBanner();

	private final ResourceLoader resourceLoader;

	private final Banner fallbackBanner;

	/**
     * Constructs a new SpringApplicationBannerPrinter with the specified resource loader and fallback banner.
     *
     * @param resourceLoader the resource loader to be used for loading the banner
     * @param fallbackBanner the fallback banner to be used if the specified banner cannot be loaded
     */
    SpringApplicationBannerPrinter(ResourceLoader resourceLoader, Banner fallbackBanner) {
		this.resourceLoader = resourceLoader;
		this.fallbackBanner = fallbackBanner;
	}

	/**
     * Prints a banner based on the given environment, source class, and logger.
     * 
     * @param environment the environment to get the banner from
     * @param sourceClass the class from which the banner is being printed
     * @param logger the logger to use for logging information
     * @return a PrintedBanner object representing the printed banner
     */
    Banner print(Environment environment, Class<?> sourceClass, Log logger) {
		Banner banner = getBanner(environment);
		try {
			logger.info(createStringFromBanner(banner, environment, sourceClass));
		}
		catch (UnsupportedEncodingException ex) {
			logger.warn("Failed to create String for banner", ex);
		}
		return new PrintedBanner(banner, sourceClass);
	}

	/**
     * Prints the banner for the given environment and source class.
     * 
     * @param environment the environment for which the banner is printed
     * @param sourceClass the source class from which the banner is printed
     * @param out the PrintStream to which the banner is printed
     * @return a PrintedBanner object representing the printed banner
     */
    Banner print(Environment environment, Class<?> sourceClass, PrintStream out) {
		Banner banner = getBanner(environment);
		banner.printBanner(environment, sourceClass, out);
		return new PrintedBanner(banner, sourceClass);
	}

	/**
     * Retrieves the banner for the given environment.
     * 
     * @param environment the environment for which to retrieve the banner
     * @return the banner for the given environment
     */
    private Banner getBanner(Environment environment) {
		Banner textBanner = getTextBanner(environment);
		if (textBanner != null) {
			return textBanner;
		}
		if (this.fallbackBanner != null) {
			return this.fallbackBanner;
		}
		return DEFAULT_BANNER;
	}

	/**
     * Retrieves the text banner based on the given environment.
     * 
     * @param environment the environment from which to retrieve the banner
     * @return the text banner if it exists and is not related to Liquibase, null otherwise
     */
    private Banner getTextBanner(Environment environment) {
		String location = environment.getProperty(BANNER_LOCATION_PROPERTY, DEFAULT_BANNER_LOCATION);
		Resource resource = this.resourceLoader.getResource(location);
		try {
			if (resource.exists() && !resource.getURL().toExternalForm().contains("liquibase-core")) {
				return new ResourceBanner(resource);
			}
		}
		catch (IOException ex) {
			// Ignore
		}
		return null;
	}

	/**
     * Creates a string representation of the banner by printing it to a ByteArrayOutputStream
     * and converting it to a string using the specified charset.
     * 
     * @param banner the Banner object to print
     * @param environment the Environment object containing the application's properties
     * @param mainApplicationClass the main application class
     * @return a string representation of the banner
     * @throws UnsupportedEncodingException if the specified charset is not supported
     */
    private String createStringFromBanner(Banner banner, Environment environment, Class<?> mainApplicationClass)
			throws UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		banner.printBanner(environment, mainApplicationClass, new PrintStream(baos));
		String charset = environment.getProperty("spring.banner.charset", "UTF-8");
		return baos.toString(charset);
	}

	/**
	 * Decorator that allows a {@link Banner} to be printed again without needing to
	 * specify the source class.
	 */
	private static class PrintedBanner implements Banner {

		private final Banner banner;

		private final Class<?> sourceClass;

		/**
         * Constructs a new PrintedBanner object with the specified Banner and sourceClass.
         * 
         * @param banner the Banner object to be used for printing
         * @param sourceClass the Class object representing the source class
         */
        PrintedBanner(Banner banner, Class<?> sourceClass) {
			this.banner = banner;
			this.sourceClass = sourceClass;
		}

		/**
         * Prints the banner using the specified environment, source class, and output stream.
         * 
         * @param environment the environment to use for printing the banner
         * @param sourceClass the source class to use for printing the banner, or null to use the default source class
         * @param out the output stream to print the banner to
         */
        @Override
		public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
			sourceClass = (sourceClass != null) ? sourceClass : this.sourceClass;
			this.banner.printBanner(environment, sourceClass, out);
		}

	}

	/**
     * SpringApplicationBannerPrinterRuntimeHints class.
     */
    static class SpringApplicationBannerPrinterRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers the hints for the runtime hints with the specified class loader.
         * 
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for registration
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern(DEFAULT_BANNER_LOCATION);
		}

	}

}
