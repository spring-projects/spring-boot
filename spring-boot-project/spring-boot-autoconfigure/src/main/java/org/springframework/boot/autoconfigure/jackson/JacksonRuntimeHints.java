package org.springframework.boot.autoconfigure.jackson;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * {@link RuntimeHintsRegistrar} for Jackson.
 * This is necessary for the configuration of Jackson's propertyNamingStrategy
 * to work in native build and execution.
 *
 * @author Ralf Ueberfuhr
 */
class JacksonRuntimeHints implements RuntimeHintsRegistrar {

	/*
	 * We need this for
	 *
	 *     JacksonAutoConfiguration
	 *       .Jackson2ObjectMapperBuilderCustomizerConfiguration
	 *       .StandardJackson2ObjectMapperBuilderCustomizer
	 *       .configurePropertyNamingStrategyField(...)
	 *
	 * to get the field using reflection!
	 */

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if(ClassUtils.isPresent("com.fasterxml.jackson.databind.PropertyNamingStrategy", classLoader)) {
			registerHints(hints.reflection());
		}
	}

	private void registerHints(ReflectionHints reflection) {
		Field[] fieldsOfStrategies = PropertyNamingStrategies.class.getDeclaredFields();
		// Jackson 2.12 pre
		Field[] fieldsOfStrategy = PropertyNamingStrategy.class.getDeclaredFields();
		// Find all static fields that provide a PropertyNamingStrategy
		// (this way we automatically support new constants
		// that may be added by Jackson in the future)
		Stream.concat(Stream.of(fieldsOfStrategies), Stream.of(fieldsOfStrategy))
				.filter(f -> Modifier.isStatic(f.getModifiers()))
				.filter(f -> f.getType().isAssignableFrom(PropertyNamingStrategy.class))
				.forEach(reflection::registerField);
	}

}
