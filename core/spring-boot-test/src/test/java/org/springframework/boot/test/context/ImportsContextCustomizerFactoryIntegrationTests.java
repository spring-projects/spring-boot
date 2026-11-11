/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ImportsContextCustomizerFactoryIntegrationTests.ImportedBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link ImportsContextCustomizerFactory} and
 * {@link ImportsContextCustomizer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@Import(ImportedBean.class)
class ImportsContextCustomizerFactoryIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ImportedBean bean;

	@Test
	void beanWasImported() {
		assertThat(this.bean).isNotNull();
	}

	@Test
	void testItselfIsNotABean() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.context.getBean(getClass()));
	}

	@Component
	static class ImportedBean {

	}

}
