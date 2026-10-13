/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Enables scanning for Servlet components ({@link WebFilter filters}, {@link WebServlet
 * servlets}, and {@link WebListener listeners}). Scanning is only performed when using an
 * embedded web server.
 * <p>
 * Typically, one of {@code value}, {@code basePackages}, or {@code basePackageClasses}
 * should be specified to control the packages to be scanned for components. In their
 * absence, scanning will be performed from the package of the class with the annotation.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 * @see WebServlet
 * @see WebFilter
 * @see WebListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ServletComponentScanRegistrar.class)
public @interface ServletComponentScan {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
	 * declarations e.g.: {@code @ServletComponentScan("org.my.pkg")} instead of
	 * {@code @ServletComponentScan(basePackages="org.my.pkg")}.
	 * @return the base packages to scan
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan for annotated servlet components. {@link #value()} is an
	 * alias for (and mutually exclusive with) this attribute.
	 * <p>
	 * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
	 * package names.
	 * @return the base packages to scan
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to
	 * scan for annotated servlet components. The package of each class specified will be
	 * scanned.
	 * @return classes from the base packages to scan
	 */
	Class<?>[] basePackageClasses() default {};

}
