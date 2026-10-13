/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties without lombok annotations at element level.
 *
 * @author Jonas Keßler
 */
@TestConfigurationProperties("accesslevel")
@SuppressWarnings("deprecation")
public class LombokAccessLevelProperties {

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PUBLIC)
	private String name0;

	@Getter
	@Setter
	private String name1;

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

	/*
	 * Either PUBLIC getter or setter explicitly defined
	 */
	@Getter(AccessLevel.PUBLIC)
	private String ignoredOnlyPublicGetter;

	@Setter(AccessLevel.PUBLIC)
	private String ignoredOnlyPublicSetter;

}
