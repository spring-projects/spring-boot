/*
 * Copyright 2012-2018 the original author or authors.
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
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Configuration properties using lombok @Getter and @Setter without explicitly defining
 * AccessLevel on element level and overwriting behaviour at field level.
 *
 * @author Jonas Keßler
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "accesslevel.overwrite.default")
public class LombokAccessLevelOverwriteDefaultProperties {

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
	private String ignoredAccessLevelNone;

	/*
	 * AccessLevel.PRIVATE
	 */
	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private String ignoredAccessLevelPrivate;

	/*
	 * AccessLevel.PACKAGE
	 */
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private String ignoredAccessLevelPackage;

	/*
	 * AccessLevel.PROTECTED
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private String ignoredAccessLevelProtected;

	/*
	 * AccessLevel.MODULE
	 */
	@Getter(AccessLevel.MODULE)
	@Setter(AccessLevel.MODULE)
	private String ignoredAccessLevelModule;

}
