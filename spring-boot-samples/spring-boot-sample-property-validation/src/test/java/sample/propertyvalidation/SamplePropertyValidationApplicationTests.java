/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.propertyvalidation;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SamplePropertyValidationApplication}.
 *
 * @author Lucas Saldanha
 * @author Stephane Nicoll
 */
public class SamplePropertyValidationApplicationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void closeContext() {
		this.context.close();
	}

	@Test
	public void bindValidProperties() {
		this.context.register(SamplePropertyValidationApplication.class);
		EnvironmentTestUtils.addEnvironment(this.context, "sample.host:192.168.0.1",
				"sample.port:9090");
		this.context.refresh();
		SampleProperties properties = this.context.getBean(SampleProperties.class);
		assertThat(properties.getHost()).isEqualTo("192.168.0.1");
		assertThat(properties.getPort()).isEqualTo(Integer.valueOf(9090));
	}

	@Test
	public void bindInvalidHost() {
		this.context.register(SamplePropertyValidationApplication.class);
		EnvironmentTestUtils.addEnvironment(this.context, "sample.host:xxxxxx",
				"sample.port:9090");
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("xxxxxx");
		this.context.refresh();
	}

	@Test
	public void bindNullHost() {
		this.context.register(SamplePropertyValidationApplication.class);
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("null");
		this.thrown.expectMessage("host");
		this.context.refresh();
	}

	@Test
	public void validatorOnlyCalledOnSupportedClass() {
		this.context.register(SamplePropertyValidationApplication.class);
		this.context.register(ServerProperties.class); // our validator will not apply
		EnvironmentTestUtils.addEnvironment(this.context, "sample.host:192.168.0.1",
				"sample.port:9090");
		this.context.refresh();
		SampleProperties properties = this.context.getBean(SampleProperties.class);
		assertThat(properties.getHost()).isEqualTo("192.168.0.1");
		assertThat(properties.getPort()).isEqualTo(Integer.valueOf(9090));
	}

}
