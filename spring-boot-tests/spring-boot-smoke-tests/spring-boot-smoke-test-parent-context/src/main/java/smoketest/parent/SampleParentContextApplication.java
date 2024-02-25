/*
 * Copyright 2012-2024 the original author or authors.
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

/**
 * SampleParentContextApplication class.
 */
@SpringBootApplication
public class SampleParentContextApplication {

	/**
	 * The main method is the entry point of the application. It creates a new
	 * SpringApplicationBuilder object with the Parent class as the parent context, and
	 * the SampleParentContextApplication class as the child context. It then runs the
	 * application with the provided command line arguments.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		new SpringApplicationBuilder(Parent.class).child(SampleParentContextApplication.class).run(args);
	}

	/**
	 * Parent class.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class Parent {

		private final ServiceProperties serviceProperties;

		/**
		 * Constructs a new Parent object with the specified ServiceProperties.
		 * @param serviceProperties the ServiceProperties to be associated with the Parent
		 * object
		 */
		public Parent(ServiceProperties serviceProperties) {
			this.serviceProperties = serviceProperties;
		}

		/**
		 * Creates a file reader message source.
		 * @return the file reader message source
		 */
		@Bean
		public FileReadingMessageSource fileReader() {
			FileReadingMessageSource reader = new FileReadingMessageSource();
			reader.setDirectory(this.serviceProperties.getInputDir());
			return reader;
		}

		/**
		 * Creates a new DirectChannel object.
		 * @return the newly created DirectChannel object
		 */
		@Bean
		public DirectChannel inputChannel() {
			return new DirectChannel();
		}

		/**
		 * Creates a new DirectChannel object.
		 * @return the newly created DirectChannel object
		 */
		@Bean
		public DirectChannel outputChannel() {
			return new DirectChannel();
		}

		/**
		 * Creates a {@link FileWritingMessageHandler} bean for writing messages to a
		 * file.
		 * @return the created {@link FileWritingMessageHandler} bean
		 */
		@Bean
		public FileWritingMessageHandler fileWriter() {
			FileWritingMessageHandler writer = new FileWritingMessageHandler(this.serviceProperties.getOutputDir());
			writer.setExpectReply(false);
			return writer;
		}

		/**
		 * Creates an integration flow for processing files using the provided sample
		 * endpoint.
		 * @param endpoint the sample endpoint to be used for processing files
		 * @return the integration flow for processing files
		 */
		@Bean
		public IntegrationFlow integrationFlow(SampleEndpoint endpoint) {
			return IntegrationFlow.from(fileReader(), new FixedRatePoller())
				.channel(inputChannel())
				.handle(endpoint)
				.channel(outputChannel())
				.handle(fileWriter())
				.get();
		}

		/**
		 * FixedRatePoller class.
		 */
		private static final class FixedRatePoller implements Consumer<SourcePollingChannelAdapterSpec> {

			/**
			 * Sets the polling configuration for the SourcePollingChannelAdapterSpec.
			 * @param spec the SourcePollingChannelAdapterSpec to configure
			 */
			@Override
			public void accept(SourcePollingChannelAdapterSpec spec) {
				spec.poller(Pollers.fixedRate(500));
			}

		}

	}

}
