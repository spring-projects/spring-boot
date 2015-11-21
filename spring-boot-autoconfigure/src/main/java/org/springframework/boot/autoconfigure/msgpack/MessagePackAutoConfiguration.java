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

package org.springframework.boot.autoconfigure.msgpack;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;



/**
 * {@link EnableAutoConfiguration Auto-configuration} for MessagePack.
 * @author Toshiaki Maki
 * @since 1.3.2
 */
@Configuration
@ConditionalOnClass({ HttpMessageConverter.class, RestTemplate.class, MessagePackFactory.class, ObjectMapper.class })
@AutoConfigureBefore(HttpMessageConvertersAutoConfiguration.class)
public class MessagePackAutoConfiguration {

	@Autowired(required = false)
	RestTemplate restTemplate;

	@ConditionalOnMissingBean
	@Bean
	public MessagePackHttpMessageConverter messagePackHttpMessageConverter() {
		return new MessagePackHttpMessageConverter();
	}

	@Bean
	public InitializingBean messagePackRestTemplateInitializer() {
		return new InitializingBean() {
			@Override
			public void afterPropertiesSet() throws Exception {
				if (MessagePackAutoConfiguration.this.restTemplate != null) {
					List<HttpMessageConverter<?>> converters = MessagePackAutoConfiguration.this.restTemplate
							.getMessageConverters();
					HttpMessageConverter<?> converter = CollectionUtils.findValueOfType(
							converters, MessagePackHttpMessageConverter.class);
					if (converter == null) {
						converters.add(messagePackHttpMessageConverter());
					}
				}
			}
		};
	}

}
