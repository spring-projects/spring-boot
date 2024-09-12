/*
 * Copyright 2012-2024 the original author or authors.
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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.Environment;

/**
 * Class used by {@link SpringApplication} to print the application banner.
 *
 * @author Phillip Webb
 * @author Junhyung Park
 */
class DefaultSpringApplicationBannerPrinter implements SpringApplicationBannerPrinter {

	private static final Log logger = LogFactory.getLog(DefaultSpringApplicationBannerPrinter.class);

	@Override
	public Banner print(Environment environment, Class<?> sourceClass, Banner.Mode bannerMode, Banner banner) {
		banner.printBanner(environment, sourceClass, System.out);
		switch (bannerMode) {
			case OFF:
			case LOG:
				try {
					logger.info(createStringFromBanner(banner, environment, sourceClass));
				}
				catch (UnsupportedEncodingException ex) {
					logger.warn("Failed to create String for banner", ex);
				}
			case CONSOLE:
				banner.printBanner(environment, sourceClass, System.out);
		}
		return new PrintedBanner(banner, sourceClass);
	}

	private String createStringFromBanner(Banner banner, Environment environment, Class<?> mainApplicationClass)
			throws UnsupportedEncodingException {
		String charset = environment.getProperty("spring.banner.charset", StandardCharsets.UTF_8.name());
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (PrintStream out = new PrintStream(byteArrayOutputStream, false, charset)) {
			banner.printBanner(environment, mainApplicationClass, out);
		}
		return byteArrayOutputStream.toString(charset);
	}

	/**
	 * Decorator that allows a {@link Banner} to be printed again without needing to
	 * specify the source class.
	 */
	private static class PrintedBanner implements Banner {

		private final Banner banner;

		private final Class<?> sourceClass;

		PrintedBanner(Banner banner, Class<?> sourceClass) {
			this.banner = banner;
			this.sourceClass = sourceClass;
		}

		@Override
		public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
			sourceClass = (sourceClass != null) ? sourceClass : this.sourceClass;
			this.banner.printBanner(environment, sourceClass, out);
		}

	}

}
