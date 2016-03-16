/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.geode.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * The {@link SampleDataGeodeApplicationProperties} class encapsulates properties
 * used to configure the {@link sample.data.geode.SampleDataGeodeApplication}.
 *
 * @author John Blum
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "sample.data.geode")
@SuppressWarnings("unused")
public class SampleDataGeodeApplicationProperties {

	protected static final String DEFAULT_CALCULATOR = "factorial";

	private String calculator;

	protected String defaultIfEmpty(String value, String defaultValue) {
		return (StringUtils.hasText(value) ? value : defaultValue);
	}

	public String getCalculator() {
		return defaultIfEmpty(calculator, DEFAULT_CALCULATOR);
	}

	public void setCalculator(String calculator) {
		this.calculator = calculator;
	}
}
