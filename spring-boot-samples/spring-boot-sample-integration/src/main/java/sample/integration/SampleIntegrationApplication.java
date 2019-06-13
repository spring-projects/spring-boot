/*
 * Copyright 2012-2019 the original author or authors.
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

package sample.integration;

import java.util.function.Consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;

@SpringBootApplication
public class SampleIntegrationApplication {

	private final ServiceProperties serviceProperties;

	public SampleIntegrationApplication(ServiceProperties serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	@Bean
	public FileReadingMessageSource fileReader() {
		FileReadingMessageSource reader = new FileReadingMessageSource();
		reader.setDirectory(this.serviceProperties.getInputDir());
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
		FileWritingMessageHandler writer = new FileWritingMessageHandler(this.serviceProperties.getOutputDir());
		writer.setExpectReply(false);
		return writer;
	}

	@Bean
	public IntegrationFlow integrationFlow(SampleEndpoint endpoint) {
		return IntegrationFlows.from(fileReader(), new FixedRatePoller()).channel(inputChannel()).handle(endpoint)
				.channel(outputChannel()).handle(fileWriter()).get();
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleIntegrationApplication.class, args);
	}

	private static class FixedRatePoller implements Consumer<SourcePollingChannelAdapterSpec> {

		@Override
		public void accept(SourcePollingChannelAdapterSpec spec) {
			spec.poller(Pollers.fixedRate(500));
		}

	}

}
