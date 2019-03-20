/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationsample.lombok;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Configuration properties using lombok @Getter/@Setter at field level.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "explicit")
public class LombokExplicitProperties {

	@Getter
	private final String id = "super-id";

	/**
	 * Name description.
	 */
	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	private String description;

	@Getter
	@Setter
	private Integer counter;

	@Deprecated
	@Getter
	@Setter
	private Integer number = 0;

	@Getter
	private final List<String> items = new ArrayList<String>();

	// Should be ignored if no annotation is set
	@SuppressWarnings("unused")
	private String ignored;

	@Getter
	private String ignoredOnlyGetter;

	@Setter
	private String ignoredOnlySetter;

}
