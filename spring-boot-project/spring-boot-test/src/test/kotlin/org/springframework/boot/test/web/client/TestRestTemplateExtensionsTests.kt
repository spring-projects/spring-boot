/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.web.client

import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.util.ReflectionUtils
import org.springframework.web.client.RestOperations
import java.net.URI
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.kotlinFunction

/**
 * Mock object based tests for [TestRestTemplate] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class TestRestTemplateExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var template: TestRestTemplate

	@Test
	fun `getForObject with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val var1 = "var1"
		val var2 = "var2"
		template.getForObject<Foo>(url, var1, var2)
		template.restTemplate
		verify(template, times(1)).getForObject(url, Foo::class.java, var1, var2)
	}

	@Test
	fun `getForObject with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.getForObject<Foo>(url, vars)
		verify(template, times(1)).getForObject(url, Foo::class.java, vars)
	}

	@Test
	fun `getForObject with reified type parameters and URI`() {
		val url = URI("https://spring.io")
		template.getForObject<Foo>(url)
		verify(template, times(1)).getForObject(url, Foo::class.java)
	}

	@Test
	fun `getForEntity with reified type parameters and URI`() {
		val url = URI("https://spring.io")
		template.getForEntity<Foo>(url)
		verify(template, times(1)).getForEntity(url, Foo::class.java)
	}

	@Test
	fun `getForEntity with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val var1 = "var1"
		val var2 = "var2"
		template.getForEntity<Foo>(url, var1, var2)
		verify(template, times(1)).getForEntity(url, Foo::class.java, var1, var2)
	}

	@Test
	fun `getForEntity with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.getForEntity<Foo>(url, vars)
		verify(template, times(1)).getForEntity(url, Foo::class.java, vars)
	}

	@Test
	fun `patchForObject with reified type parameters, String, Any and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		template.patchForObject<Foo>(url, body, var1, var2)
		verify(template, times(1)).patchForObject(url, body, Foo::class.java, var1, var2)
	}

	@Test
	fun `patchForObject with reified type parameters, String, Any and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.patchForObject<Foo>(url, body, vars)
		verify(template, times(1)).patchForObject(url, body, Foo::class.java, vars)
	}

	@Test
	fun `patchForObject with reified type parameters, String and Any`() {
		val url = "https://spring.io"
		val body: Any = "body"
		template.patchForObject<Foo>(url, body)
		verify(template, times(1)).patchForObject(url, body, Foo::class.java)
	}

	@Test
	fun `patchForObject with reified type parameters`() {
		val url = "https://spring.io"
		template.patchForObject<Foo>(url)
		verify(template, times(1)).patchForObject(url, null, Foo::class.java)
	}

	@Test
	fun `postForObject with reified type parameters, String, Any and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		template.postForObject<Foo>(url, body, var1, var2)
		verify(template, times(1)).postForObject(url, body, Foo::class.java, var1, var2)
	}

	@Test
	fun `postForObject with reified type parameters, String, Any and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.postForObject<Foo>(url, body, vars)
		verify(template, times(1)).postForObject(url, body, Foo::class.java, vars)
	}

	@Test
	fun `postForObject with reified type parameters, String and Any`() {
		val url = "https://spring.io"
		val body: Any = "body"
		template.postForObject<Foo>(url, body)
		verify(template, times(1)).postForObject(url, body, Foo::class.java)
	}

	@Test
	fun `postForObject with reified type parameters`() {
		val url = "https://spring.io"
		template.postForObject<Foo>(url)
		verify(template, times(1)).postForObject(url, null, Foo::class.java)
	}

	@Test
	fun `postForEntity with reified type parameters, String, Any and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		template.postForEntity<Foo>(url, body, var1, var2)
		verify(template, times(1)).postForEntity(url, body, Foo::class.java, var1, var2)
	}

	@Test
	fun `postForEntity with reified type parameters, String, Any and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.postForEntity<Foo>(url, body, vars)
		verify(template, times(1)).postForEntity(url, body, Foo::class.java, vars)
	}

	@Test
	fun `postForEntity with reified type parameters, String and Any`() {
		val url = "https://spring.io"
		val body: Any = "body"
		template.postForEntity<Foo>(url, body)
		verify(template, times(1)).postForEntity(url, body, Foo::class.java)
	}

	@Test
	fun `postForEntity with reified type parameters`() {
		val url = "https://spring.io"
		template.postForEntity<Foo>(url)
		verify(template, times(1)).postForEntity(url, null, Foo::class.java)
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity and varargs`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mock<HttpEntity<Foo>>()
		val var1 = "var1"
		val var2 = "var2"
		template.exchange<List<Foo>>(url, method, entity, var1, var2)
		verify(template, times(1)).exchange(url, method, entity,
				object : ParameterizedTypeReference<List<Foo>>() {}, var1, var2)
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity and Map`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mock<HttpEntity<Foo>>()
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.exchange<List<Foo>>(url, method, entity, vars)
		verify(template, times(1)).exchange(url, method, entity,
				object : ParameterizedTypeReference<List<Foo>>() {}, vars)
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mock<HttpEntity<Foo>>()
		template.exchange<List<Foo>>(url, method, entity)
		verify(template, times(1)).exchange(url, method, entity,
				object : ParameterizedTypeReference<List<Foo>>() {})
	}

	@Test
	fun `exchange with reified type parameters and HttpEntity`() {
		val entity = mock<RequestEntity<Foo>>()
		template.exchange<List<Foo>>(entity)
		verify(template, times(1)).exchange(entity,
				object : ParameterizedTypeReference<List<Foo>>() {})
	}

	@Test
	fun `exchange with reified type parameters, String and HttpMethod`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		template.exchange<List<Foo>>(url, method)
		verify(template, times(1)).exchange(url, method, null,
				object : ParameterizedTypeReference<List<Foo>>() {})
	}

	@Test
	fun `RestOperations are available`() {
		val extensions = Class.forName(
				"org.springframework.boot.test.web.client.TestRestTemplateExtensionsKt")
		ReflectionUtils.doWithMethods(RestOperations::class.java) { method ->
			arrayOf(ParameterizedTypeReference::class, Class::class).forEach { kClass ->
				if (method.parameterTypes.contains(kClass.java)) {
					val parameters = mutableListOf<Class<*>>(TestRestTemplate::class.java)
							.apply { addAll(method.parameterTypes.filter { it != kClass.java }) }
					val f = extensions.getDeclaredMethod(method.name,
							*parameters.toTypedArray()).kotlinFunction!!
					Assert.assertEquals(1, f.typeParameters.size)
					Assert.assertEquals(listOf(Any::class.createType()),
							f.typeParameters[0].upperBounds)
				}
			}
		}
	}

	class Foo

}
