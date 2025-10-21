/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.test.autoconfigure;

import org.htmlunit.BrowserVersion;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.web.htmlunit.UriBuilderFactoryWebConnectionHtmlUnitDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Auto-configuration for Selenium {@link WebDriver} MockMVC integration.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = MockMvcAutoConfiguration.class)
@ConditionalOnClass(HtmlUnitDriver.class)
@ConditionalOnBooleanProperty(name = "spring.test.mockmvc.webdriver.enabled", matchIfMissing = true)
public final class MockMvcWebDriverAutoConfiguration {

	/**
	 * A {@link UriBuilderFactory} that is suitable for Mock access (i.e. without a
	 * running web server).
	 */
	private static final UriBuilderFactory MOCK_URI_BUILDER_FACTORY = new DefaultUriBuilderFactory("http://localhost");

	@Bean
	@ConditionalOnMissingBean({ WebDriver.class, MockMvcHtmlUnitDriverBuilder.class })
	@ConditionalOnBean(MockMvc.class)
	MockMvcHtmlUnitDriverBuilder mockMvcHtmlUnitDriverBuilder(MockMvc mockMvc) {
		return MockMvcHtmlUnitDriverBuilder.mockMvcSetup(mockMvc)
			.withDelegate(
					new UriBuilderFactoryWebConnectionHtmlUnitDriver(MOCK_URI_BUILDER_FACTORY, BrowserVersion.CHROME));
	}

	@Bean
	@ConditionalOnMissingBean(WebDriver.class)
	@ConditionalOnBean(MockMvcHtmlUnitDriverBuilder.class)
	HtmlUnitDriver htmlUnitDriver(MockMvcHtmlUnitDriverBuilder builder,
			ObjectProvider<MockMvcHtmlUnitDriverCustomizer> customizers) {
		HtmlUnitDriver driver = builder.build();
		customizers.orderedStream().forEach((customizer) -> customizer.customize(driver));
		return driver;
	}

}
