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

package org.springframework.boot.autoconfigure.paho;

import org.apache.activemq.broker.BrokerService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PahoAutoConfigurationTests implements MqttCallback {

    private AnnotationConfigApplicationContext context;

    private AtomicBoolean isMessageReceived = new AtomicBoolean();

    @After
    public void after() {
        context.close();
    }

    @Test
    public void testConnectionToDefaultLocalhostMqttBroker() throws MqttException {
        context = load(DefaultConfiguration.class);
        PahoMqttClientFactory clientFactory = context.getBean(PahoMqttClientFactory.class);
        MqttClient client = clientFactory.createMqttClient();
        client.setCallback(this);
        client.subscribe("topic");

        client.publish("topic", new MqttMessage("message".getBytes()));

        await().atMost(15, TimeUnit.SECONDS).untilTrue(isMessageReceived);
        assertTrue(isMessageReceived.get());
    }

    @Test
    public void shouldInvokeConnectionConfigurationCallback() {
        context = load(DefaultConfiguration.class, PahoConnectionConfiguration.class);
        PahoMqttConnectOptionsConfiguration connectOptionsConfiguration = context.getBean(PahoMqttConnectOptionsConfiguration.class);
        PahoMqttClientFactory clientFactory = context.getBean(PahoMqttClientFactory.class);

        clientFactory.createMqttClient();

        verify(connectOptionsConfiguration).configure(Mockito.any(MqttConnectOptions.class));
    }

    // Test helpers

    private AnnotationConfigApplicationContext load(Class<?>... config) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(config);
        applicationContext.register(PahoAutoConfiguration.class);
        applicationContext.refresh();
        return applicationContext;
    }

    // Test MQTT callbacks

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        isMessageReceived.set(true);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    // Test configurations

    @Configuration
    static class DefaultConfiguration {

        @Bean(initMethod = "start", destroyMethod = "stop")
        public BrokerService brokerService() throws Exception {
            BrokerService brokerService = new BrokerService();
            brokerService.setPersistent(false);
            brokerService.addConnector("mqtt://0.0.0.0:1883");
            return brokerService;
        }

    }

    @Configuration
    static class PahoConnectionConfiguration {

        @Bean
        public PahoMqttConnectOptionsConfiguration connectOptionsConfiguration() {
            return mock(PahoMqttConnectOptionsConfiguration.class);
        }

    }

}