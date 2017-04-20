package org.springframework.boot.autoconfigure.condition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.extractProperty;

/**
 * Test for {@link ConditionalOnEnabledProperty} and {@link OnEnabledPropertyCondition}.<br>
 * This test is defined by test metadata rather then code
 * @author
 * 		<br>20 Apr 2017 idosu
 */
@RunWith(Parameterized.class)
public class ConditionalOnEnabledPropertyTest {
	private  static final String PREFIX = "spring.foo";
	private  static final String PROPERTY = PREFIX + "." + "enabled";
	private  static final String ENABLED = PROPERTY + "=" + "true";
	private  static final String DISABLED = PROPERTY + "=" + "false";

	/**
	 * The environment to load into the {@link org.springframework.context.ApplicationContext}
	 */
	@Retention(RUNTIME)
	@Target(TYPE)
	private @interface Environment {
		String[] value();
	}

	/**
	 * Marker for the metadata that the bean should work and be configured,
	 * it this annotation is missing the bean should not be configured.
	 */
	@Retention(RUNTIME)
	@Target(TYPE)
	private @interface ShouldWork {
	}

	/** The configuration class */
	public @Parameter(0) Class<?> config;
	/** Unused, only used to print nice message in {@link Parameters} */
	public @Parameter(1) String simpleClassName;
	/** The environment to load into the context see {@link org.springframework.boot.autoconfigure.condition.ConditionalOnEnabledPropertyTest.Environment} */
	public @Parameter(2) List<String> environment;
	/** If the bean should be configured */
	public @Parameter(3) boolean shouldWork;

	private AnnotationConfigApplicationContext context;

	@Parameters(name="{index} - bean {1} with environment {2} should-work:{3}")
	public static Iterable<Object[]> params() {
		List<Object[]> params = new LinkedList<Object[]>();

		for (Class<?> clazz : ConditionalOnEnabledPropertyTest.class.getClasses()) {
			int mod = clazz.getModifiers();
			// Filters the annotations and more(if added)
			if (clazz.isMemberClass() && Modifier.isPublic(mod) && !Modifier.isAbstract(mod)) {
				Environment env = clazz.getAnnotation(Environment.class);
				List<String> environment = env == null ? emptyList() : asList(env.value());
				boolean shouldWork = clazz.isAnnotationPresent(ShouldWork.class);

				params.add(new Object[] {
					clazz,
					clazz.getSimpleName(),
					environment,
					shouldWork
				});
			}
		}

		return params;
	}

	@Before
	public void setUp() {
		context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, environment.toArray(new String[0]));
		context.register(config);
		context.refresh();
	}

	@After
	public void tearDown() {
		context.close();
	}

	@Test
	public void run() {
		NoSuchBeanDefinitionException exception = null;
		try {
			context.getBean(config);
		} catch (NoSuchBeanDefinitionException e) {
			exception = e;
		}

		// If the test should work the bean should exist and there should be no exception
		assertThat(exception == null).isEqualTo(shouldWork);
	}

	// Actual Test metadata
	// --------------------

	@Configuration
	@ConditionalOnEnabledProperty
	@ConfigurationProperties(PREFIX)
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface ConfiguredCorrectly {
	}

	@ConfiguredCorrectly
	public static class ConfiguredCorrectlyNoEnvironment {
	}

	@ShouldWork
	@Environment(ENABLED)
	@ConfiguredCorrectly
	public static class ConfiguredCorrectlyEnabled {
	}

	@ShouldWork
	@Environment({ ENABLED, "foo=bar" })
	@ConfiguredCorrectly
	public static class ConfiguredCorrectlyEnabledWithMoreEnvironment {
	}

	@ShouldWork
	@Environment(PROPERTY + "=" + "TRUE")
	@ConfiguredCorrectly
	public static class ConfiguredCorrectlyEnabledUpperCase {
	}

	@Environment(DISABLED)
	@ConfiguredCorrectly
	public static class ConfiguredCorrectlyDisabled {
	}

	@Environment("foo=bar")
	@ConfiguredCorrectly
	public static class ConfiguredCorrectlyRandomProperty {
	}

	@Environment(ENABLED)
	@Configuration
	@ConditionalOnEnabledProperty
	public static class MissingConfigurationProperties {
	}

	@Environment(ENABLED)
	@Configuration
	@ConditionalOnEnabledProperty
	@ConfigurationProperties
	public static class MissingPrefix {
	}
}
