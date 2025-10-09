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

package org.springframework.boot.configurationsample.lombok;

import java.util.ArrayList;
import java.util.List;

import lombok.Value;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties using Lombok {@code @Value}.
 *
 * @author Mark Jeffrey
 */
@Value
@TestConfigurationProperties("value")
@SuppressWarnings("unused")
public class LombokSimpleValueProperties {

	private final String id = "super-id";

	/**
	 * Name description.
	 */
	private String name;

	private String description;

	private Integer counter;

	@Deprecated
	private Integer number = 0;

	private final List<String> items = new ArrayList<>();

	private final String ignored = "foo";

}
