package org.springframework.boot.docs.io.restclient.globalconfiguration.inetaddressfiltering

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.boot.http.client.InetAddressFilter
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class MyService {

	private val restClient: RestClient

	init {
		val onlyExternalAddresses = InetAddressFilter.externalAddresses()
		val settings = HttpClientSettings.defaults().withInetAddressFilter(onlyExternalAddresses)
		val requestFactory: ClientHttpRequestFactory = ClientHttpRequestFactoryBuilder.jdk().build(settings)
		restClient = RestClient.builder().requestFactory(requestFactory).baseUrl("https://example.org").build()
	}

	fun someRestCall(name: String?): Details {
		return restClient.get().uri("/{name}/details", name)
			.retrieve().body(Details::class.java)!!
	}

}
