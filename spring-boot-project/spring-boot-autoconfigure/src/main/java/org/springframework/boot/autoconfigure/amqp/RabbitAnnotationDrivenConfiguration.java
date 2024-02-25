/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * Configuration for Spring AMQP annotation driven endpoints.
 *
 * @author Stephane Nicoll
 * @author Josh Thornhill
 * @author Moritz Halbritter
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableRabbit.class)
class RabbitAnnotationDrivenConfiguration {

	private final ObjectProvider<MessageConverter> messageConverter;

	private final ObjectProvider<MessageRecoverer> messageRecoverer;

	private final ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers;

	private final RabbitProperties properties;

	/**
     * Constructs a new RabbitAnnotationDrivenConfiguration with the specified parameters.
     *
     * @param messageConverter        the object provider for the message converter
     * @param messageRecoverer        the object provider for the message recoverer
     * @param retryTemplateCustomizers the object provider for the retry template customizers
     * @param properties              the RabbitMQ properties
     */
    RabbitAnnotationDrivenConfiguration(ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<MessageRecoverer> messageRecoverer,
			ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers, RabbitProperties properties) {
		this.messageConverter = messageConverter;
		this.messageRecoverer = messageRecoverer;
		this.retryTemplateCustomizers = retryTemplateCustomizers;
		this.properties = properties;
	}

	/**
     * Creates a new instance of SimpleRabbitListenerContainerFactoryConfigurer.
     * This method is annotated with @Bean, indicating that it is a Spring bean and should be managed by the Spring container.
     * It is also annotated with @ConditionalOnMissingBean, which means that this bean will only be created if there is no existing bean of the same type in the container.
     * Additionally, it is annotated with @ConditionalOnThreading(Threading.PLATFORM), which specifies that this bean should only be created if the threading model is set to PLATFORM.
     * 
     * @return the created SimpleRabbitListenerContainerFactoryConfigurer bean
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	SimpleRabbitListenerContainerFactoryConfigurer simpleRabbitListenerContainerFactoryConfigurer() {
		return simpleListenerConfigurer();
	}

	/**
     * Configures the SimpleRabbitListenerContainerFactory for virtual threads.
     * 
     * @return The SimpleRabbitListenerContainerFactoryConfigurer for virtual threads.
     */
    @Bean(name = "simpleRabbitListenerContainerFactoryConfigurer")
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	SimpleRabbitListenerContainerFactoryConfigurer simpleRabbitListenerContainerFactoryConfigurerVirtualThreads() {
		SimpleRabbitListenerContainerFactoryConfigurer configurer = simpleListenerConfigurer();
		configurer.setTaskExecutor(new VirtualThreadTaskExecutor());
		return configurer;
	}

	/**
     * Creates a SimpleRabbitListenerContainerFactory bean with the name "rabbitListenerContainerFactory".
     * This bean is conditional on the absence of another bean with the same name.
     * It is also conditional on the property "spring.rabbitmq.listener.type" having the value "simple",
     * with a default value of "simple" if the property is missing.
     * 
     * @param configurer The SimpleRabbitListenerContainerFactoryConfigurer used to configure the factory.
     * @param connectionFactory The ConnectionFactory used by the factory.
     * @param simpleContainerCustomizer The ContainerCustomizer used to customize the SimpleMessageListenerContainer.
     * @return The created SimpleRabbitListenerContainerFactory bean.
     */
    @Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "simple",
			matchIfMissing = true)
	SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(
			SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
			ObjectProvider<ContainerCustomizer<SimpleMessageListenerContainer>> simpleContainerCustomizer) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		simpleContainerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	/**
     * Creates a new instance of DirectRabbitListenerContainerFactoryConfigurer.
     * This method is annotated with @Bean, indicating that it is a Spring bean that will be managed by the Spring container.
     * It is also annotated with @ConditionalOnMissingBean, which means that this bean will only be created if there is no existing bean of the same type in the container.
     * Additionally, it is annotated with @ConditionalOnThreading(Threading.PLATFORM), which specifies that this bean will only be created if the threading model is set to PLATFORM.
     * 
     * @return The created DirectRabbitListenerContainerFactoryConfigurer instance.
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	DirectRabbitListenerContainerFactoryConfigurer directRabbitListenerContainerFactoryConfigurer() {
		return directListenerConfigurer();
	}

	/**
     * Configures the DirectRabbitListenerContainerFactoryConfigurer bean for virtual threads.
     * This method is annotated with @Bean to indicate that it is a bean definition method.
     * The name attribute is set to "directRabbitListenerContainerFactoryConfigurer" to specify the bean name.
     * The method is annotated with @ConditionalOnMissingBean to indicate that it should only be executed if there is no existing bean of the same type.
     * The method is also annotated with @ConditionalOnThreading(Threading.VIRTUAL) to specify that it should only be executed if the threading mode is set to virtual.
     * 
     * @return The configured DirectRabbitListenerContainerFactoryConfigurer bean for virtual threads.
     */
    @Bean(name = "directRabbitListenerContainerFactoryConfigurer")
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	DirectRabbitListenerContainerFactoryConfigurer directRabbitListenerContainerFactoryConfigurerVirtualThreads() {
		DirectRabbitListenerContainerFactoryConfigurer configurer = directListenerConfigurer();
		configurer.setTaskExecutor(new VirtualThreadTaskExecutor());
		return configurer;
	}

	/**
     * Creates a DirectRabbitListenerContainerFactory bean with the name "rabbitListenerContainerFactory".
     * This bean is conditional on the absence of another bean with the same name and the property "spring.rabbitmq.listener.type" having the value "direct".
     * 
     * @param configurer The DirectRabbitListenerContainerFactoryConfigurer used to configure the factory.
     * @param connectionFactory The ConnectionFactory used by the factory.
     * @param directContainerCustomizer The ContainerCustomizer used to customize the DirectMessageListenerContainer.
     * @return The created DirectRabbitListenerContainerFactory bean.
     */
    @Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "direct")
	DirectRabbitListenerContainerFactory directRabbitListenerContainerFactory(
			DirectRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
			ObjectProvider<ContainerCustomizer<DirectMessageListenerContainer>> directContainerCustomizer) {
		DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		directContainerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	/**
     * Configures the SimpleRabbitListenerContainerFactoryConfigurer for the RabbitAnnotationDrivenConfiguration class.
     * 
     * @return The configured SimpleRabbitListenerContainerFactoryConfigurer object.
     */
    private SimpleRabbitListenerContainerFactoryConfigurer simpleListenerConfigurer() {
		SimpleRabbitListenerContainerFactoryConfigurer configurer = new SimpleRabbitListenerContainerFactoryConfigurer(
				this.properties);
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setMessageRecoverer(this.messageRecoverer.getIfUnique());
		configurer.setRetryTemplateCustomizers(this.retryTemplateCustomizers.orderedStream().toList());
		return configurer;
	}

	/**
     * Configures the DirectRabbitListenerContainerFactory.
     * 
     * @return The DirectRabbitListenerContainerFactoryConfigurer object.
     */
    private DirectRabbitListenerContainerFactoryConfigurer directListenerConfigurer() {
		DirectRabbitListenerContainerFactoryConfigurer configurer = new DirectRabbitListenerContainerFactoryConfigurer(
				this.properties);
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setMessageRecoverer(this.messageRecoverer.getIfUnique());
		configurer.setRetryTemplateCustomizers(this.retryTemplateCustomizers.orderedStream().toList());
		return configurer;
	}

	/**
     * EnableRabbitConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@EnableRabbit
	@ConditionalOnMissingBean(name = RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableRabbitConfiguration {

	}

}
