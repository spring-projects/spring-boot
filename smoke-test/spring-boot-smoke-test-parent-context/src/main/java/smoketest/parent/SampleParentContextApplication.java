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

package smoketest.parent;

import java.io.File;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.util.Assert;

@SpringBootApplication
public class SampleParentContextApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(Parent.class).child(SampleParentContextApplication.class).run(args);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class Parent {

		private final ServiceProperties serviceProperties;

		public Parent(ServiceProperties serviceProperties) {
			this.serviceProperties = serviceProperties;
		}

		@Bean
		public FileReadingMessageSource fileReader() {
			FileReadingMessageSource reader = new FileReadingMessageSource();
			File inputDir = this.serviceProperties.getInputDir();
			Assert.state(inputDir != null, "'inputDir' must not be null");
			reader.setDirectory(inputDir);
			return reader;
		}

		@Bean
		public DirectChannel inputChannel() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel outputChannel() {
			return new DirectChannel();
		}

		@Bean
		public FileWritingMessageHandler fileWriter() {
			File outputDir = this.serviceProperties.getOutputDir();
			Assert.state(outputDir != null, "'outputDir' must not be null");
			FileWritingMessageHandler writer = new FileWritingMessageHandler(outputDir);
			writer.setExpectReply(false);
			return writer;
		}

		@Bean
		public IntegrationFlow integrationFlow(SampleEndpoint endpoint) {
			return IntegrationFlow.from(fileReader(), new FixedRatePoller())
				.channel(inputChannel())
				.handle(endpoint)
				.channel(outputChannel())
				.handle(fileWriter())
				.get();
		}

		private static final class FixedRatePoller implements Consumer<SourcePollingChannelAdapterSpec> {

			@Override
			public void accept(SourcePollingChannelAdapterSpec spec) {
				spec.poller(Pollers.fixedRate(500));
			}

		}

	}

}
