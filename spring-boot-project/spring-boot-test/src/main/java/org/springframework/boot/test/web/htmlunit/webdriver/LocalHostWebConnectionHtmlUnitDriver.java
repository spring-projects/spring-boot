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

package org.springframework.boot.test.web.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.openqa.selenium.Capabilities;

import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver;
import org.springframework.util.Assert;

/**
 * {@link LocalHostWebConnectionHtmlUnitDriver} will automatically prefix relative URLs
 * with <code>localhost:$&#123;local.server.port&#125;</code>.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class LocalHostWebConnectionHtmlUnitDriver extends WebConnectionHtmlUnitDriver {

	private final Environment environment;

	/**
     * Constructs a new LocalHostWebConnectionHtmlUnitDriver with the specified environment.
     * 
     * @param environment the environment to be used for the driver
     * @throws IllegalArgumentException if the environment is null
     */
    public LocalHostWebConnectionHtmlUnitDriver(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	/**
     * Constructs a new LocalHostWebConnectionHtmlUnitDriver with the specified environment and enableJavascript flag.
     * 
     * @param environment the environment to be used for the driver
     * @param enableJavascript flag indicating whether JavaScript should be enabled or not
     * @throws IllegalArgumentException if the environment is null
     */
    public LocalHostWebConnectionHtmlUnitDriver(Environment environment, boolean enableJavascript) {
		super(enableJavascript);
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	/**
     * Constructs a new LocalHostWebConnectionHtmlUnitDriver with the specified environment and browser version.
     * 
     * @param environment the environment to be used for the web connection
     * @param browserVersion the version of the browser to be used
     * @throws IllegalArgumentException if the environment is null
     */
    public LocalHostWebConnectionHtmlUnitDriver(Environment environment, BrowserVersion browserVersion) {
		super(browserVersion);
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	/**
     * Constructs a new LocalHostWebConnectionHtmlUnitDriver with the specified environment and capabilities.
     * 
     * @param environment the environment to be used for the driver
     * @param capabilities the capabilities to be used for the driver
     * @throws IllegalArgumentException if the environment is null
     */
    public LocalHostWebConnectionHtmlUnitDriver(Environment environment, Capabilities capabilities) {
		super(capabilities);
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	/**
     * Sends a GET request to the specified URL.
     * If the URL starts with "/", it is assumed to be a relative path and the local server port is appended to it.
     * The request is then sent using the super class's get method.
     *
     * @param url the URL to send the GET request to
     */
    @Override
	public void get(String url) {
		if (url.startsWith("/")) {
			String port = this.environment.getProperty("local.server.port", "8080");
			url = "http://localhost:" + port + url;
		}
		super.get(url);
	}

}
