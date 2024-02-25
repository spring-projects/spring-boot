/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} for Logback.
 *
 * @author Andy Wilkinson
 */
class LogbackRuntimeHints implements RuntimeHintsRegistrar {

	/**
     * Registers hints for Logback logging system.
     * 
     * @param hints the runtime hints object
     * @param classLoader the class loader to use for checking class presence
     */
    @Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (!ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext", classLoader)) {
			return;
		}
		ReflectionHints reflection = hints.reflection();
		registerHintsForLogbackLoggingSystemTypeChecks(reflection, classLoader);
		registerHintsForBuiltInLogbackConverters(reflection);
		registerHintsForSpringBootConverters(reflection);
	}

	/**
     * Registers type hints for Logback logging system type checks.
     * 
     * @param reflection   the ReflectionHints object used for registering type hints
     * @param classLoader  the ClassLoader used for loading classes
     */
    private void registerHintsForLogbackLoggingSystemTypeChecks(ReflectionHints reflection, ClassLoader classLoader) {
		reflection.registerType(LoggerContext.class);
		reflection.registerTypeIfPresent(classLoader, "org.slf4j.bridge.SLF4JBridgeHandler", (typeHint) -> {
		});
	}

	/**
     * Registers hints for the built-in Logback converters.
     * 
     * @param reflection the ReflectionHints object used for registering hints
     */
    private void registerHintsForBuiltInLogbackConverters(ReflectionHints reflection) {
		registerForPublicConstructorInvocation(reflection, DateTokenConverter.class, IntegerTokenConverter.class,
				SyslogStartConverter.class);
	}

	/**
     * Registers hints for Spring Boot converters.
     * 
     * @param reflection the reflection hints object
     */
    private void registerHintsForSpringBootConverters(ReflectionHints reflection) {
		registerForPublicConstructorInvocation(reflection, ColorConverter.class,
				ExtendedWhitespaceThrowableProxyConverter.class, WhitespaceThrowableProxyConverter.class,
				CorrelationIdConverter.class);
	}

	/**
     * Registers the given classes for public constructor invocation in the provided ReflectionHints object.
     * 
     * @param reflection The ReflectionHints object to register the classes with.
     * @param classes The classes to be registered.
     */
    private void registerForPublicConstructorInvocation(ReflectionHints reflection, Class<?>... classes) {
		reflection.registerTypes(TypeReference.listOf(classes),
				(hint) -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

}
