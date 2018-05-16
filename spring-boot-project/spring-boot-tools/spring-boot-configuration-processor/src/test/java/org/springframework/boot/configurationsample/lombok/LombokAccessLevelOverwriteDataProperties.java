/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.configurationsample.lombok;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Configuration properties using lombok @Data on element level and overwriting behaviour
 * with @Getter und @Setter at field level.
 *
 * @author Jonas Keßler
 */
@Data
@ConfigurationProperties(prefix = "accesslevel.overwrite.data")
public class LombokAccessLevelOverwriteDataProperties {

	private String name0;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PUBLIC)
	private String name1;

	@Getter(AccessLevel.PUBLIC)
	private String name2;

	@Setter(AccessLevel.PUBLIC)
	private String name3;

	@Getter
	@Setter
	private String name4;

	@Getter
	private String name5;

	@Setter
	private String name6;

	/*
	 * AccessLevel.NONE
	 */
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private String ignored0;

	@Getter(AccessLevel.NONE)
	private String ignored1;

	@Setter(AccessLevel.NONE)
	private String ignored2;

	/*
	 * AccessLevel.PRIVATE
	 */
	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private String ignored3;

	@Getter(AccessLevel.PRIVATE)
	private String ignored4;

	@Setter(AccessLevel.PRIVATE)
	private String ignored5;

	/*
	 * AccessLevel.PACKAGE
	 */
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private String ignored6;

	@Getter(AccessLevel.PACKAGE)
	private String ignored7;

	@Setter(AccessLevel.PACKAGE)
	private String ignored8;

	/*
	 * AccessLevel.PROTECTED
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private String ignored9;

	@Getter(AccessLevel.PROTECTED)
	private String ignored10;

	@Setter(AccessLevel.PROTECTED)
	private String ignored11;

	/*
	 * AccessLevel.MODULE
	 */
	@Getter(AccessLevel.MODULE)
	@Setter(AccessLevel.MODULE)
	private String ignored12;

	@Getter(AccessLevel.MODULE)
	private String ignored13;

	@Setter(AccessLevel.MODULE)
	private String ignored14;
}
