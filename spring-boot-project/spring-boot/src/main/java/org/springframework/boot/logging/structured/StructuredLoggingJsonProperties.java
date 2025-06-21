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

package org.springframework.boot.logging.structured;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.StandardStackTracePrinter;
import org.springframework.boot.util.Instantiator;
import org.springframework.core.env.Environment;

/**
 * Properties that can be used to customize structured logging JSON.
 *
 * @param include the paths that should be included. An empty set includes all names
 * @param exclude the paths that should be excluded. An empty set excludes nothing
 * @param rename a map of path to replacement names
 * @param add a map of additional elements {@link StructuredLoggingJsonMembersCustomizer}
 * @param stackTrace stack trace properties
 * @param customizer the fully qualified names of
 * {@link StructuredLoggingJsonMembersCustomizer} implementations
 * @param context context specific properties
 * @author Phillip Webb
 * @author Yanming Zhou
 */
record StructuredLoggingJsonProperties(Set<String> include, Set<String> exclude, Map<String, String> rename,
		Map<String, String> add, StackTrace stackTrace, Context context,
		Set<Class<? extends StructuredLoggingJsonMembersCustomizer<?>>> customizer) {

	StructuredLoggingJsonProperties {
		customizer = (customizer != null) ? customizer : Collections.emptySet();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Collection<StructuredLoggingJsonMembersCustomizer<Object>> customizers(Instantiator<?> instantiator) {
		return (List) customizer().stream().map(instantiator::instantiateType).toList();
	}

	static StructuredLoggingJsonProperties get(Environment environment) {
		return Binder.get(environment)
			.bind("logging.structured.json", StructuredLoggingJsonProperties.class)
			.orElse(null);
	}

	/**
	 * Properties to influence stack trace printing.
	 *
	 * @param printer the name of the printer to use. Can be {@code null},
	 * {@code "standard"}, {@code "logging-system"}, or the fully-qualified class name of
	 * a {@link StackTracePrinter} implementation. A {@code null} value will be treated as
	 * {@code "standard"} when any other property is set, otherwise it will be treated as
	 * {@code "logging-system"}. {@link StackTracePrinter} implementations may optionally
	 * inject a {@link StandardStackTracePrinter} instance into their constructor which
	 * will be configured from the properties.
	 * @param root the root ordering (root first or root last)
	 * @param maxLength the maximum length to print
	 * @param maxThrowableDepth the maximum throwable depth to print
	 * @param includeCommonFrames whether common frames should be included
	 * @param includeHashes whether stack trace hashes should be included
	 */
	record StackTrace(String printer, Root root, Integer maxLength, Integer maxThrowableDepth,
			Boolean includeCommonFrames, Boolean includeHashes) {

		StackTracePrinter createPrinter() {
			String name = sanitizePrinter();
			if ("loggingsystem".equals(name) || (name.isEmpty() && !hasAnyOtherProperty())) {
				return null;
			}
			StandardStackTracePrinter standardPrinter = createStandardPrinter();
			if ("standard".equals(name) || name.isEmpty()) {
				return standardPrinter;
			}
			return (StackTracePrinter) new Instantiator<>(StackTracePrinter.class,
					(parameters) -> parameters.add(StandardStackTracePrinter.class, standardPrinter))
				.instantiate(printer());
		}

		boolean hasCustomPrinter() {
			String name = sanitizePrinter();
			if (name.isEmpty()) {
				return false;
			}
			return !("loggingsystem".equals(name) || "standard".equals(name));
		}

		private String sanitizePrinter() {
			return Objects.toString(printer(), "").toLowerCase(Locale.ROOT).replace("-", "");
		}

		private boolean hasAnyOtherProperty() {
			return Stream.of(root(), maxLength(), maxThrowableDepth(), includeCommonFrames(), includeHashes())
				.anyMatch(Objects::nonNull);
		}

		private StandardStackTracePrinter createStandardPrinter() {
			StandardStackTracePrinter printer = (root() == Root.FIRST) ? StandardStackTracePrinter.rootFirst()
					: StandardStackTracePrinter.rootLast();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			printer = map.from(this::maxLength).to(printer, StandardStackTracePrinter::withMaximumLength);
			printer = map.from(this::maxThrowableDepth)
				.to(printer, StandardStackTracePrinter::withMaximumThrowableDepth);
			printer = map.from(this::includeCommonFrames)
				.to(printer, apply(StandardStackTracePrinter::withCommonFrames));
			printer = map.from(this::includeHashes).to(printer, apply(StandardStackTracePrinter::withHashes));
			return printer;
		}

		private BiFunction<StandardStackTracePrinter, Boolean, StandardStackTracePrinter> apply(
				UnaryOperator<StandardStackTracePrinter> action) {
			return (printer, value) -> (!value) ? printer : action.apply(printer);
		}

		/**
		 * Root ordering.
		 */
		enum Root {

			LAST, FIRST

		}

	}

	/**
	 * Properties that influence context values (usually elements propagated from the
	 * logging MDC).
	 *
	 * @param include if context elements should be included
	 * @param prefix the prefix to use for context elements
	 * @since 3.5.0
	 */
	record Context(@DefaultValue("true") boolean include, String prefix) {

	}

	static class StructuredLoggingJsonPropertiesRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			BindableRuntimeHintsRegistrar.forTypes(StructuredLoggingJsonProperties.class)
				.registerHints(hints, classLoader);
		}

	}

}
