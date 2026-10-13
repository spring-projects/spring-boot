/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.propertyvalidation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SamplePropertyValidationApplication}.
 *
 * @author Lucas Saldanha
 * @author Stephane Nicoll
 */
class SamplePropertyValidationApplicationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	void closeContext() {
		this.context.close();
	}

	@Test
	void bindValidProperties() {
		this.context.register(SamplePropertyValidationApplication.class);
		TestPropertyValues.of("sample.host:192.168.0.1", "sample.port:9090").applyTo(this.context);
		this.context.refresh();
		SampleProperties properties = this.context.getBean(SampleProperties.class);
		assertThat(properties.getHost()).isEqualTo("192.168.0.1");
		assertThat(properties.getPort()).isEqualTo(Integer.valueOf(9090));
	}

	@Test
	void bindInvalidHost() {
		this.context.register(SamplePropertyValidationApplication.class);
		TestPropertyValues.of("sample.host:xxxxxx", "sample.port:9090").applyTo(this.context);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(this.context::refresh)
			.havingRootCause()
			.isInstanceOf(BindValidationException.class);
	}

	@Test
	void bindNullHost() {
		this.context.register(SamplePropertyValidationApplication.class);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(this.context::refresh)
			.havingRootCause()
			.isInstanceOf(BindValidationException.class);
	}

	@Test
	void validatorOnlyCalledOnSupportedClass() {
		this.context.register(SamplePropertyValidationApplication.class);
		this.context.register(ServerProperties.class); // our validator will not apply
		TestPropertyValues.of("sample.host:192.168.0.1", "sample.port:9090").applyTo(this.context);
		this.context.refresh();
		SampleProperties properties = this.context.getBean(SampleProperties.class);
		assertThat(properties.getHost()).isEqualTo("192.168.0.1");
		assertThat(properties.getPort()).isEqualTo(Integer.valueOf(9090));
	}

}
