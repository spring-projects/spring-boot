/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties using lombok @Getter and @Setter with explicitly defining
 * AccessLevel.PUBLIC on element level and overwriting behaviour at field level.
 *
 * @author Jonas Keßler
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@TestConfigurationProperties("accesslevel.overwrite.explicit")
@SuppressWarnings({ "unused", "deprecation" })
public class LombokAccessLevelOverwriteExplicitProperties {

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
