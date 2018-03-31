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
import java.util.function.Supplier;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.client.core.SoapFaultMessageResolver;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServiceTemplateBuilder}.
 *
 * @author Dmytro Nosan
 */
public class WebServiceTemplateBuilderTests {

	private WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder();

	@Test
	public void addInterceptors() {
		ClientInterceptor f1 = Mockito.mock(ClientInterceptor.class);
		ClientInterceptor f2 = Mockito.mock(ClientInterceptor.class);

		WebServiceTemplate webServiceTemplate = this.builder.addInterceptors(f1)
				.addInterceptors(f2).build();

		assertThat(webServiceTemplate.getInterceptors()).containsExactlyInAnyOrder(f1,
				f2);
	}

	@Test
	public void addInterceptorsCollection() {
		ClientInterceptor f1 = Mockito.mock(ClientInterceptor.class);
		ClientInterceptor f2 = Mockito.mock(ClientInterceptor.class);

		WebServiceTemplate webServiceTemplate = this.builder
				.addInterceptors(Collections.singletonList(f1))
				.addInterceptors(Collections.singleton(f2)).build();

		assertThat(webServiceTemplate.getInterceptors()).containsExactlyInAnyOrder(f1,
				f2);

	}

	@Test
	public void setInterceptors() {
		ClientInterceptor f1 = Mockito.mock(ClientInterceptor.class);
		ClientInterceptor f2 = Mockito.mock(ClientInterceptor.class);

		WebServiceTemplate webServiceTemplate = this.builder.setInterceptors(f1)
				.setInterceptors(f2).build();

		assertThat(webServiceTemplate.getInterceptors()).doesNotContain(f1).contains(f2);
	}

	@Test
	public void setInterceptorsCollection() {
		ClientInterceptor f1 = Mockito.mock(ClientInterceptor.class);
		ClientInterceptor f2 = Mockito.mock(ClientInterceptor.class);

		WebServiceTemplate webServiceTemplate = this.builder
				.setInterceptors(Collections.singletonList(f1))
				.setInterceptors(Collections.singleton(f2)).build();

		assertThat(webServiceTemplate.getInterceptors()).doesNotContain(f1).contains(f2);

	}

	@Test
	public void addCustomizers() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		WebServiceTemplateCustomizer customizer = (ws) -> ws
				.setMarshaller(jaxb2Marshaller);
		WebServiceTemplateCustomizer customizer1 = (ws) -> ws
				.setUnmarshaller(jaxb2Marshaller);

		WebServiceTemplate webServiceTemplate = this.builder.addCustomizers(customizer)
				.addCustomizers(customizer1).build();

		assertThat(webServiceTemplate.getMarshaller()).isEqualTo(jaxb2Marshaller);
		assertThat(webServiceTemplate.getUnmarshaller()).isEqualTo(jaxb2Marshaller);

	}

	@Test
	public void addCustomizersCollection() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		WebServiceTemplateCustomizer customizer = (ws) -> ws
				.setMarshaller(jaxb2Marshaller);
		WebServiceTemplateCustomizer customizer1 = (ws) -> ws
				.setUnmarshaller(jaxb2Marshaller);

		WebServiceTemplate webServiceTemplate = this.builder
				.addCustomizers(Collections.singleton(customizer))
				.addCustomizers(Collections.singletonList(customizer1)).build();

		assertThat(webServiceTemplate.getMarshaller()).isEqualTo(jaxb2Marshaller);
		assertThat(webServiceTemplate.getUnmarshaller()).isEqualTo(jaxb2Marshaller);
	}

	@Test
	public void setCustomizers() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		WebServiceTemplateCustomizer customizer = (ws) -> ws
				.setMarshaller(jaxb2Marshaller);
		WebServiceTemplateCustomizer customizer1 = (ws) -> ws
				.setUnmarshaller(jaxb2Marshaller);

		WebServiceTemplate webServiceTemplate = this.builder.setCustomizers(customizer)
				.setCustomizers(customizer1).build();

		assertThat(webServiceTemplate.getMarshaller()).isNull();
		assertThat(webServiceTemplate.getUnmarshaller()).isEqualTo(jaxb2Marshaller);

	}

	@Test
	public void setCustomizersCollection() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		WebServiceTemplateCustomizer customizer = (ws) -> ws
				.setMarshaller(jaxb2Marshaller);
		WebServiceTemplateCustomizer customizer1 = (ws) -> ws
				.setUnmarshaller(jaxb2Marshaller);

		WebServiceTemplate webServiceTemplate = this.builder
				.setCustomizers(Collections.singleton(customizer))
				.setCustomizers(Collections.singletonList(customizer1)).build();

		assertThat(webServiceTemplate.getMarshaller()).isNull();
		assertThat(webServiceTemplate.getUnmarshaller()).isEqualTo(jaxb2Marshaller);
	}

	@Test
	public void addWebServiceMessageSenders() {
		WebServiceMessageSender sender = Mockito.mock(WebServiceMessageSender.class);
		WebServiceMessageSender sender1 = Mockito.mock(WebServiceMessageSender.class);

		WebServiceTemplate webServiceTemplate = this.builder
				.addWebServiceMessageSenders(Collections.singleton(() -> sender))
				.addWebServiceMessageSenders(Collections.singletonList(() -> sender1))
				.build();

		assertThat(webServiceTemplate.getMessageSenders())
				.containsExactlyInAnyOrder(sender, sender1);
	}

	@Test
	public void setWebServiceMessageSenders() {
		WebServiceMessageSender sender = Mockito.mock(WebServiceMessageSender.class);
		WebServiceMessageSender sender1 = Mockito.mock(WebServiceMessageSender.class);

		WebServiceTemplate webServiceTemplate = this.builder
				.setWebServiceMessageSenders(Collections.singleton(() -> sender))
				.setWebServiceMessageSenders(Collections.singletonList(() -> sender1))
				.build();

		assertThat(webServiceTemplate.getMessageSenders()).doesNotContain(sender)
				.contains(sender1);

	}

	@Test
	public void addWebServiceMessageSenderClass() {

		WebServiceTemplate webServiceTemplate = this.builder
				.addWebServiceMessageSender(ClientHttpRequestMessageSender.class)
				.addWebServiceMessageSender(HttpUrlConnectionMessageSender.class).build();

		assertThat(webServiceTemplate.getMessageSenders()).hasSize(2);

		assertThat(webServiceTemplate.getMessageSenders()[0])
				.isInstanceOf(ClientHttpRequestMessageSender.class);
		assertThat(webServiceTemplate.getMessageSenders()[1])
				.isInstanceOf(HttpUrlConnectionMessageSender.class);
	}

	@Test
	public void setWebServiceMessageSenderClass() {

		WebServiceTemplate webServiceTemplate = this.builder
				.setWebServiceMessageSender(ClientHttpRequestMessageSender.class)
				.setWebServiceMessageSender(HttpUrlConnectionMessageSender.class).build();

		assertThat(webServiceTemplate.getMessageSenders()).hasSize(1);
		assertThat(webServiceTemplate.getMessageSenders()[0])
				.isInstanceOf(HttpUrlConnectionMessageSender.class);

	}

	@Test
	public void addWebServiceMessageSender() {
		WebServiceMessageSender sender = Mockito.mock(WebServiceMessageSender.class);
		WebServiceMessageSender sender1 = Mockito.mock(WebServiceMessageSender.class);

		WebServiceTemplate webServiceTemplate = this.builder
				.addWebServiceMessageSender(() -> sender)
				.addWebServiceMessageSender(() -> sender1).build();

		assertThat(webServiceTemplate.getMessageSenders())
				.containsExactlyInAnyOrder(sender, sender1);
	}

	@Test
	public void setWebServiceMessageSender() {
		WebServiceMessageSender sender = Mockito.mock(WebServiceMessageSender.class);
		WebServiceMessageSender sender1 = Mockito.mock(WebServiceMessageSender.class);

		WebServiceTemplate webServiceTemplate = this.builder
				.setWebServiceMessageSender(() -> sender)
				.setWebServiceMessageSender(() -> sender1).build();

		assertThat(webServiceTemplate.getMessageSenders()).doesNotContain(sender)
				.contains(sender1);

	}

	@Test
	public void setCheckConnectionForFault() {
		MockWebServiceTemplate webServiceTemplate = this.builder
				.setCheckConnectionForFault(false).build(MockWebServiceTemplate.class);

		assertThat(webServiceTemplate.isCheckConnectionForFault()).isFalse();
	}

	@Test
	public void setCheckConnectionForError() {

		MockWebServiceTemplate webServiceTemplate = this.builder
				.setCheckConnectionForError(false).build(MockWebServiceTemplate.class);

		assertThat(webServiceTemplate.isCheckConnectionForError()).isFalse();

	}

	@Test
	public void setTransformerFactoryClass() {
		MockWebServiceTemplate webServiceTemplate = this.builder
				.setTransformerFactoryClass(SAXTransformerFactory.class)
				.build(MockWebServiceTemplate.class);

		assertThat(webServiceTemplate.getTransformerFactoryClass())
				.isEqualTo(SAXTransformerFactory.class);

	}

	@Test
	public void setWebServiceMessageFactory() {

		SaajSoapMessageFactory messageFactory = new SaajSoapMessageFactory();

		WebServiceTemplate webServiceTemplate = this.builder
				.setWebServiceMessageFactory(messageFactory).build();

		assertThat(webServiceTemplate.getMessageFactory()).isEqualTo(messageFactory);

	}

	@Test
	public void setMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();

		WebServiceTemplate webServiceTemplate = this.builder
				.setMarshaller(jaxb2Marshaller).build();
		assertThat(webServiceTemplate.getMarshaller()).isEqualTo(jaxb2Marshaller);
	}

	@Test
	public void setUnmarshaller() {
		Jaxb2Marshaller jaxb2Unmarshaller = new Jaxb2Marshaller();

		WebServiceTemplate webServiceTemplate = this.builder
				.setUnmarshaller(jaxb2Unmarshaller).build();

		assertThat(webServiceTemplate.getUnmarshaller()).isEqualTo(jaxb2Unmarshaller);
	}

	@Test
	public void setFaultMessageResolver() {

		FaultMessageResolver faultMessageResolver = new SoapFaultMessageResolver();
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
		URI uri = URI.create("http://localhost:8080");

		WebServiceTemplate webServiceTemplate = this.builder
				.setDestinationProvider(() -> uri).build();

		assertThat(webServiceTemplate.getDestinationProvider().getDestination())
				.isEqualTo(uri);

	}

	@Test
	public void shouldNotOverrideDefaultSender() {
		WebServiceMessageSender sender = Mockito.mock(WebServiceMessageSender.class);
		WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
		webServiceTemplate.setMessageSender(sender);

		this.builder.detectWebServiceMessageSender(false).configure(webServiceTemplate);

		assertThat(webServiceTemplate.getMessageSenders()).hasSize(1).contains(sender);

	}

	@Test
	public void addInterceptorsToExistingWebServiceTemplate() {
		ClientInterceptor f1 = Mockito.mock(ClientInterceptor.class);
		ClientInterceptor f2 = Mockito.mock(ClientInterceptor.class);

		WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
		webServiceTemplate.setInterceptors(new ClientInterceptor[] { f1 });

		this.builder.addInterceptors(f2).configure(webServiceTemplate);

		assertThat(webServiceTemplate.getInterceptors()).containsExactlyInAnyOrder(f2,
				f1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setInterceptorsArrayNull() {
		this.builder.setInterceptors((ClientInterceptor[]) null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setInterceptorsCollectionNull() {
		this.builder.setInterceptors((Collection<? extends ClientInterceptor>) null)
				.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addInterceptorsArrayNull() {
		this.builder.addInterceptors((ClientInterceptor[]) null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addInterceptorsCollectionNull() {
		this.builder.addInterceptors((Collection<? extends ClientInterceptor>) null)
				.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setCustomizersArrayNull() {
		this.builder.setCustomizers((WebServiceTemplateCustomizer[]) null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setCustomizersCollectionNull() {
		this.builder
				.setCustomizers((Collection<? extends WebServiceTemplateCustomizer>) null)
				.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addCustomizersArrayNull() {
		this.builder
				.addCustomizers((Collection<? extends WebServiceTemplateCustomizer>) null)
				.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addCustomizersCollectionNull() {
		this.builder
				.addCustomizers((Collection<? extends WebServiceTemplateCustomizer>) null)
				.build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setWebServiceMessageSendersNull() {
		this.builder.setWebServiceMessageSenders(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addWebServiceMessageSendersNull() {
		this.builder.addWebServiceMessageSenders(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setWebServiceMessageSenderClassNull() {
		this.builder.setWebServiceMessageSender(
				(Class<? extends WebServiceMessageSender>) null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addWebServiceMessageSenderClassNull() {
		this.builder.addWebServiceMessageSender(
				(Class<? extends WebServiceMessageSender>) null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setWebServiceMessageSenderSupplierNull() {
		this.builder.setWebServiceMessageSender(
				(Supplier<? extends WebServiceMessageSender>) null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addWebServiceMessageSenderSupplierNull() {
		this.builder.addWebServiceMessageSender(
				(Supplier<? extends WebServiceMessageSender>) null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setWebServiceMessageFactoryNull() {
		this.builder.setWebServiceMessageFactory(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setUnmarshallerNull() {
		this.builder.setUnmarshaller(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setMarshallerNull() {
		this.builder.setMarshaller(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setTransformerFactoryClassNull() {
		this.builder.setTransformerFactoryClass(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setDefaultUriNull() {
		this.builder.setDefaultUri(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setDestinationProviderNull() {
		this.builder.setDestinationProvider(null).build();
	}

	private static class MockWebServiceTemplate extends WebServiceTemplate {

		private boolean checkConnectionForError;

		private boolean checkConnectionForFault;

		private Class<? extends TransformerFactory> transformerFactoryClass;

		boolean isCheckConnectionForError() {
			return this.checkConnectionForError;
		}

		@Override
		public void setCheckConnectionForError(boolean checkConnectionForError) {
			this.checkConnectionForError = checkConnectionForError;
		}

		boolean isCheckConnectionForFault() {
			return this.checkConnectionForFault;
		}

		@Override
		public void setCheckConnectionForFault(boolean checkConnectionForFault) {
			this.checkConnectionForFault = checkConnectionForFault;
		}

		Class<? extends TransformerFactory> getTransformerFactoryClass() {
			return this.transformerFactoryClass;
		}

		@Override
		public void setTransformerFactoryClass(
				Class<? extends TransformerFactory> transformerFactoryClass) {
			this.transformerFactoryClass = transformerFactoryClass;
		}

	}

}
