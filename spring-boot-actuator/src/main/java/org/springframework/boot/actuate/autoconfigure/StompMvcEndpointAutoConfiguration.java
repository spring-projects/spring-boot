/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.actuate.endpoint.mvc.StompMvcEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

/**
 * @author Vladimir Tsanev
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration")
public class StompMvcEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "managementSubProtocolWebSocketHandler")
    public WebSocketHandler managementSubProtocolWebSocketHandler(final AbstractSubscribableChannel clientInboundChannel,
                                                                  final AbstractSubscribableChannel clientOutboundChannel) {
        return new SubProtocolWebSocketHandler(clientInboundChannel, clientOutboundChannel);
    }

    @Bean
    @ConditionalOnMissingBean
    public StompMvcEndpoint stompMvcEndpoint(final ManagementServerProperties managementServerProperties,
                                             final WebSocketHandler managementSubProtocolWebSocketHandler) {
        return new StompMvcEndpoint(managementServerProperties.getContextPath(), managementSubProtocolWebSocketHandler);

    }

    @Configuration
    static class SimpMessagingConfiguration extends AbstractMessageBrokerConfiguration {

        @Override
        protected SimpUserRegistry createLocalUserRegistry() {
            return new DefaultSimpUserRegistry();
        }

    }

}
