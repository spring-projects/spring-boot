/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.webservices.client;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServiceTemplateBuilder}. This test class check that builder will
 * create HttpUrlConnectionMessageSender If Ok-http and Apache client are not present in
 * the classpath.
 *
 * @author Dmytro Nosan
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "httpclient-*.jar", "okhttp-*.jar" })
public class WebServiceTemplateBuilderHttpUrlConnectionMessageSenderTests {

	private WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder();

	@Test
	public void build() {
		WebServiceTemplate webServiceTemplate = this.builder.build();

		assertThat(webServiceTemplate.getMessageSenders()).hasSize(1);

		assertThat(webServiceTemplate.getMessageSenders()[0])
				.isInstanceOf(HttpUrlConnectionMessageSender.class);

	}

	@Test
	public void setTimeout() {
		HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();

		this.builder.setConnectionTimeout(5000).setReadTimeout(2000)
				.setWebServiceMessageSender(() -> sender).build();

		assertThat(ReflectionTestUtils.getField(sender, "connectionTimeout"))
				.isEqualTo(Duration.ofMillis(5000));
		assertThat(ReflectionTestUtils.getField(sender, "readTimeout"))
				.isEqualTo(Duration.ofMillis(2000));

	}

}
