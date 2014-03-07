/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <P>  {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitTemplate}.
 *
 * <p> This configuration class is active only when the RabbitMQ and Spring AMQP client libraries are on the classpath.
 *
 * <P> Registers a {@link org.springframework.amqp.rabbit.core.RabbitTemplate RabbitTemplate} instance if there
 * is no other bean of the same type in the context. Registers a {@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory CachingConnectionFactory}
 * instance if there is no other bean of the same type in the context.
 *
 * <p> Registers a {@link org.springframework.amqp.core.AmqpAdmin } instance as long as {@literal spring.rabbitmq.dynamic=true}.
 *
 * <p>
 * The {@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory} honors the following properties:
 * {@literal spring.rabbitmq.port} is used to specify the port to which the client should connect, and defaults to 5672.
 * {@literal spring.rabbitmq.username} is used to specify the (optional) username, and
 * {@literal spring.rabbitmq.password} is used to specify the (optional) password.
 * {@literal spring.rabbitmq.host} is used to specify the  host, and defaults to {@literal localhost}.
 *  {@literal spring.rabbitmq.virtualHost} is used to specify the  (optional) virtual host to which the client should connect.
 *
 * @author Greg Turnquist
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({RabbitTemplate.class, Channel.class})
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitAutoConfiguration {

    @Bean
    @ConditionalOnExpression("${spring.rabbitmq.dynamic:true}")
    @ConditionalOnMissingBean(AmqpAdmin.class)
    public AmqpAdmin amqpAdmin(CachingConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(this.connectionFactory);
    }

    @Configuration
    @ConditionalOnMissingBean(ConnectionFactory.class)
    protected static class RabbitConnectionFactoryCreator {

        @Bean
        public ConnectionFactory rabbitConnectionFactory(RabbitProperties config) {
            CachingConnectionFactory factory = new CachingConnectionFactory(
                    config.getHost());
            factory.setPort(config.getPort());
            if (config.getUsername() != null) {
                factory.setUsername(config.getUsername());
            }
            if (config.getPassword() != null) {
                factory.setPassword(config.getPassword());
            }
            if (config.getVirtualHost() != null) {
                factory.setVirtualHost(config.getVirtualHost());
            }
            return factory;
        }

    }

}
