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

package smoketest.integration;

import java.util.function.Consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;

/**
 * SampleIntegrationApplication class.
 */
@SpringBootApplication
public class SampleIntegrationApplication {

	private final ServiceProperties serviceProperties;

	/**
	 * Constructs a new SampleIntegrationApplication with the specified ServiceProperties.
	 * @param serviceProperties the ServiceProperties to be used by the
	 * SampleIntegrationApplication
	 */
	public SampleIntegrationApplication(ServiceProperties serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	/**
	 * Creates a FileReadingMessageSource object for reading files from the specified
	 * directory.
	 * @return the FileReadingMessageSource object configured with the input directory
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
	 * Creates a FileWritingMessageHandler bean.
	 * @return the created FileWritingMessageHandler bean
	 */
	@Bean
	public FileWritingMessageHandler fileWriter() {
		FileWritingMessageHandler writer = new FileWritingMessageHandler(this.serviceProperties.getOutputDir());
		writer.setExpectReply(false);
		return writer;
	}

	/**
	 * Creates an integration flow for processing files using a sample endpoint.
	 * @param endpoint the sample endpoint to be used for handling the files
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
	 * The main method is the entry point of the application. It starts the Spring Boot
	 * application by calling the SpringApplication.run() method.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(SampleIntegrationApplication.class, args);
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
