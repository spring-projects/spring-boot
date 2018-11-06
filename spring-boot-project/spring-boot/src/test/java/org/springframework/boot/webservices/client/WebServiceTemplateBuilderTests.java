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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.xml.transform.sax.SAXTransformerFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link WebServiceTemplateBuilder}.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 */
public class WebServiceTemplateBuilderTests {

	private final WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder();

	@Mock
	private WebServiceMessageSender messageSender;

	@Mock
	private ClientInterceptor interceptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void createWithCustomizersShouldApplyCustomizers() {
		WebServiceTemplateCustomizer customizer = mock(
				WebServiceTemplateCustomizer.class);
		WebServiceTemplate template = new WebServiceTemplateBuilder(customizer).build();
		verify(customizer).customize(template);
	}

	@Test
	public void buildShouldDetectHttpMessageSender() {
		WebServiceTemplate webServiceTemplate = this.builder.build();
		assertThat(webServiceTemplate.getMessageSenders()).hasSize(1);
		WebServiceMessageSender messageSender = webServiceTemplate.getMessageSenders()[0];
		assertHttpComponentsRequestFactory(messageSender);
	}

	@Test
	public void detectHttpMessageSenderWhenFalseShouldDisableDetection() {
		WebServiceTemplate webServiceTemplate = this.builder
				.detectHttpMessageSender(false).build();
		assertThat(webServiceTemplate.getMessageSenders()).hasSize(1);
		assertThat(webServiceTemplate.getMessageSenders()[0])
				.isInstanceOf(HttpUrlConnectionMessageSender.class);
	}

	@Test
	public void messageSendersWhenSendersAreAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.builder.messageSenders((WebServiceMessageSender[]) null))
				.withMessageContaining("MessageSenders must not be null");
	}

	@Test
	public void messageSendersCollectionWhenSendersAreAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.messageSenders(
						(Collection<? extends WebServiceMessageSender>) null))
				.withMessageContaining("MessageSenders must not be null");
	}

	@Test
	public void messageSendersShouldApply() {
		WebServiceTemplate template = this.builder.messageSenders(this.messageSender)
				.build();
		assertThat(template.getMessageSenders()).containsOnly(this.messageSender);
	}

	@Test
	public void messageSendersShouldReplaceExisting() {
		WebServiceTemplate template = this.builder
				.messageSenders(new ClientHttpRequestMessageSender())
				.messageSenders(this.messageSender).build();
		assertThat(template.getMessageSenders()).containsOnly(this.messageSender);
	}

	@Test
	public void additionalMessageSendersWhenSendersAreAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder
						.additionalMessageSenders((WebServiceMessageSender[]) null))
				.withMessageContaining("MessageSenders must not be null");
	}

	@Test
	public void additionalMessageSendersCollectionWhenSendersAreAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.additionalMessageSenders(
						(Collection<? extends WebServiceMessageSender>) null))
				.withMessageContaining("MessageSenders must not be null");
	}

	@Test
	public void additionalMessageSendersShouldAddToExisting() {
		ClientHttpRequestMessageSender httpMessageSender = new ClientHttpRequestMessageSender();
		WebServiceTemplate template = this.builder.messageSenders(httpMessageSender)
				.additionalMessageSenders(this.messageSender).build();
		assertThat(template.getMessageSenders()).containsOnly(httpMessageSender,
				this.messageSender);
	}

	@Test
	public void additionalMessageSendersShouldKeepDetectedHttpMessageSender() {
		WebServiceTemplate template = this.builder
				.additionalMessageSenders(this.messageSender).build();
		assertThat(template.getMessageSenders()).contains(this.messageSender);
		assertThat(template.getMessageSenders()).hasSize(2);
	}

	@Test
	public void interceptorsWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.interceptors((ClientInterceptor[]) null))
				.withMessageContaining("Interceptors must not be null");
	}

	@Test
	public void interceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder
						.interceptors((Collection<? extends ClientInterceptor>) null))
				.withMessageContaining("Interceptors must not be null");
	}

	@Test
	public void interceptorsShouldApply() {
		WebServiceTemplate template = this.builder.interceptors(this.interceptor).build();
		assertThat(template.getInterceptors()).containsOnly(this.interceptor);
	}

	@Test
	public void interceptorsShouldReplaceExisting() {
		WebServiceTemplate template = this.builder
				.interceptors(mock(ClientInterceptor.class))
				.interceptors(Collections.singleton(this.interceptor)).build();
		assertThat(template.getInterceptors()).containsOnly(this.interceptor);
	}

	@Test
	public void additionalInterceptorsWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.builder.additionalInterceptors((ClientInterceptor[]) null))
				.withMessageContaining("Interceptors must not be null");
	}

	@Test
	public void additionalInterceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.builder.additionalInterceptors((Set<ClientInterceptor>) null))
				.withMessageContaining("Interceptors must not be null");
	}

	@Test
	public void additionalInterceptorsShouldAddToExisting() {
		ClientInterceptor interceptor = mock(ClientInterceptor.class);
		WebServiceTemplate template = this.builder.interceptors(interceptor)
				.additionalInterceptors(this.interceptor).build();
		assertThat(template.getInterceptors()).containsOnly(interceptor,
				this.interceptor);
	}

	@Test
	public void additionalInterceptorsShouldAddToExistingWebServiceTemplate() {
		ClientInterceptor f1 = Mockito.mock(ClientInterceptor.class);
		ClientInterceptor f2 = Mockito.mock(ClientInterceptor.class);
		WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
		webServiceTemplate.setInterceptors(new ClientInterceptor[] { f1 });
		this.builder.additionalInterceptors(f2).configure(webServiceTemplate);
		assertThat(webServiceTemplate.getInterceptors()).containsExactlyInAnyOrder(f2,
				f1);
	}

	@Test
	public void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.builder.customizers((WebServiceTemplateCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.customizers(
						(Collection<? extends WebServiceTemplateCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void customizersShouldApply() {
		WebServiceTemplateCustomizer customizer = mock(
				WebServiceTemplateCustomizer.class);
		WebServiceTemplate template = this.builder.customizers(customizer).build();
		verify(customizer).customize(template);
	}

	@Test
	public void customizersShouldBeAppliedLast() {
		WebServiceTemplate template = spy(new WebServiceTemplate());
		this.builder
				.additionalCustomizers(((webServiceTemplate) -> verify(webServiceTemplate)
						.setMessageSenders(any())));
		this.builder.configure(template);
	}

	@Test
	public void customizersShouldReplaceExisting() {
		WebServiceTemplateCustomizer customizer1 = mock(
				WebServiceTemplateCustomizer.class);
		WebServiceTemplateCustomizer customizer2 = mock(
				WebServiceTemplateCustomizer.class);
		WebServiceTemplate template = this.builder.customizers(customizer1)
				.customizers(Collections.singleton(customizer2)).build();
		verifyZeroInteractions(customizer1);
		verify(customizer2).customize(template);
	}

	@Test
	public void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder
						.additionalCustomizers((WebServiceTemplateCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.additionalCustomizers(
						(Collection<? extends WebServiceTemplateCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void additionalCustomizersShouldAddToExisting() {
		WebServiceTemplateCustomizer customizer1 = mock(
				WebServiceTemplateCustomizer.class);
		WebServiceTemplateCustomizer customizer2 = mock(
				WebServiceTemplateCustomizer.class);
		WebServiceTemplate template = this.builder.customizers(customizer1)
				.additionalCustomizers(customizer2).build();
		verify(customizer1).customize(template);
		verify(customizer2).customize(template);
	}

	@Test
	public void setCheckConnectionForFault() {
		WebServiceTemplate template = mock(WebServiceTemplate.class);
		this.builder.setCheckConnectionForFault(false).configure(template);
		verify(template).setCheckConnectionForFault(false);
	}

	@Test
	public void setCheckConnectionForError() {
		WebServiceTemplate template = mock(WebServiceTemplate.class);
		this.builder.setCheckConnectionForError(false).configure(template);
		verify(template).setCheckConnectionForError(false);

	}

	@Test
	public void setTransformerFactoryClass() {
		WebServiceTemplate template = mock(WebServiceTemplate.class);
		this.builder.setTransformerFactoryClass(SAXTransformerFactory.class)
				.configure(template);
		verify(template).setTransformerFactoryClass(SAXTransformerFactory.class);
	}

	@Test
	public void setWebServiceMessageFactory() {
		WebServiceMessageFactory messageFactory = mock(WebServiceMessageFactory.class);
		WebServiceTemplate template = this.builder
				.setWebServiceMessageFactory(messageFactory).build();
		assertThat(template.getMessageFactory()).isEqualTo(messageFactory);
	}

	@Test
	public void setMarshaller() {
		Marshaller marshaller = mock(Marshaller.class);
		WebServiceTemplate template = this.builder.setMarshaller(marshaller).build();
		assertThat(template.getMarshaller()).isEqualTo(marshaller);
	}

	@Test
	public void setUnmarshaller() {
		Unmarshaller unmarshaller = mock(Unmarshaller.class);
		WebServiceTemplate webServiceTemplate = this.builder.setUnmarshaller(unmarshaller)
				.build();
		assertThat(webServiceTemplate.getUnmarshaller()).isEqualTo(unmarshaller);
	}

	@Test
	public void setFaultMessageResolver() {
		FaultMessageResolver faultMessageResolver = mock(FaultMessageResolver.class);
		WebServiceTemplate webServiceTemplate = this.builder
				.setFaultMessageResolver(faultMessageResolver).build();
		assertThat(webServiceTemplate.getFaultMessageResolver())
				.isEqualTo(faultMessageResolver);
	}

	@Test
	public void setDefaultUri() {
		URI uri = URI.create("http://localhost:8080");
		WebServiceTemplate webServiceTemplate = this.builder.setDefaultUri(uri.toString())
				.build();
		assertThat(webServiceTemplate.getDestinationProvider().getDestination())
				.isEqualTo(uri);
	}

	@Test
	public void setDestinationProvider() {
		DestinationProvider destinationProvider = () -> URI
				.create("http://localhost:8080");
		WebServiceTemplate webServiceTemplate = this.builder
				.setDestinationProvider(destinationProvider).build();
		assertThat(webServiceTemplate.getDestinationProvider())
				.isEqualTo(destinationProvider);
	}

	private void assertHttpComponentsRequestFactory(
			WebServiceMessageSender messageSender) {
		assertThat(messageSender).isInstanceOf(ClientHttpRequestMessageSender.class);
		ClientHttpRequestMessageSender sender = (ClientHttpRequestMessageSender) messageSender;
		ClientHttpRequestFactory requestFactory = sender.getRequestFactory();
		assertThat(requestFactory)
				.isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

}
