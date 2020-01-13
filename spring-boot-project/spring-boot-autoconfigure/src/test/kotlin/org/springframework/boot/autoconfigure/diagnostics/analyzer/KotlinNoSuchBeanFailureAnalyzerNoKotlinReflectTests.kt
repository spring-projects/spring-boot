package org.springframework.boot.autoconfigure.diagnostics.analyzer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.FatalBeanException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.diagnostics.FailureAnalysis
import org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.testsupport.classpath.ClassPathExclusions
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration

/**
 * Tests for {@link ConfigurationProperties @ConfigurationProperties}-annotated beans when kotlin-reflect is not present
 * on the classpath.
 *
 * @author Madhura Bhave
 */
@ClassPathExclusions("kotlin-reflect*.jar")
class KotlinNoSuchBeanFailureAnalyzerNoKotlinReflectTests {

	private val analyzer = NoSuchBeanDefinitionFailureAnalyzer()

	@Test
	fun failureAnalysisForConfigurationPropertiesThatMaybeShouldHaveBeenConstructorBound() {
		val analysis = analyzeFailure(
				createFailure(ConstructorBoundConfigurationPropertiesConfiguration::class.java))
		assertThat(analysis!!.getAction()).startsWith(
				java.lang.String.format("Consider defining a bean of type '%s' in your configuration.", String::class.java!!.getName()))
		assertThat(analysis!!.getAction()).contains(java.lang.String.format(
				"Consider adding a dependency on kotlin-reflect so that the constructor used for @ConstructorBinding can be located. Also, ensure that @ConstructorBinding is present on '%s' ", ConstructorBoundProperties::class.java!!.getName()))
	}

	private fun createFailure(config: Class<*>, vararg environment: String): FatalBeanException? {
		try {
			AnnotationConfigApplicationContext().use { context ->
				this.analyzer.setBeanFactory(context.beanFactory)
				TestPropertyValues.of(*environment).applyTo(context)
				context.register(config)
				context.refresh()
				return null
			}
		} catch (ex: FatalBeanException) {
			return ex
		}

	}

	private fun analyzeFailure(failure: Exception?): FailureAnalysis? {
		val analysis = this.analyzer.analyze(failure)
		if (analysis != null) {
			LoggingFailureAnalysisReporter().report(analysis)
		}
		return analysis
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ConstructorBoundProperties::class)
	internal class ConstructorBoundConfigurationPropertiesConfiguration

	@ConfigurationProperties("test")
	@ConstructorBinding
	internal class ConstructorBoundProperties(val name: String)
}