/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.web.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import java.net.URI

/**
 * Extension for [TestRestTemplate.getForObject] providing a `getForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.getForObject(url: String, vararg uriVariables: Any): T? =
		getForObject(url, T::class.java, *uriVariables)

/**
 * Extension for [TestRestTemplate.getForObject] providing a `getForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.getForObject(url: String, uriVariables: Map<String, Any?>): T? =
		getForObject(url, T::class.java, uriVariables)

/**
 * Extension for [TestRestTemplate.getForObject] providing a `getForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.getForObject(url: URI): T? =
		getForObject(url, T::class.java)

/**
 * Extension for [TestRestTemplate.getForEntity] providing a `getForEntity<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.getForEntity(url: URI): ResponseEntity<T> =
		getForEntity(url, T::class.java)

/**
 * Extension for [TestRestTemplate.getForEntity] providing a `getForEntity<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.getForEntity(url: String, vararg uriVariables: Any): ResponseEntity<T> =
		getForEntity(url, T::class.java, *uriVariables)

/**
 * Extension for [TestRestTemplate.getForEntity] providing a `getForEntity<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.getForEntity(url: String, uriVariables: Map<String, *>): ResponseEntity<T> =
		getForEntity(url, T::class.java, uriVariables)

/**
 * Extension for [TestRestTemplate.patchForObject] providing a `patchForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.patchForObject(url: String, request: Any? = null,
															 vararg uriVariables: Any): T? =
		patchForObject(url, request, T::class.java, *uriVariables)

/**
 * Extension for [TestRestTemplate.patchForObject] providing a `patchForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.patchForObject(url: String, request: Any? = null,
															 uriVariables: Map<String, *>): T? =
		patchForObject(url, request, T::class.java, uriVariables)

/**
 * Extension for [TestRestTemplate.patchForObject] providing a `patchForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.patchForObject(url: URI, request: Any? = null): T? =
		patchForObject(url, request, T::class.java)

/**
 * Extension for [TestRestTemplate.postForObject] providing a `postForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.postForObject(url: String, request: Any? = null,
															vararg uriVariables: Any): T? =
		postForObject(url, request, T::class.java, *uriVariables)

/**
 * Extension for [TestRestTemplate.postForObject] providing a `postForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.postForObject(url: String, request: Any? = null,
															uriVariables: Map<String, *>): T? =
		postForObject(url, request, T::class.java, uriVariables)

/**
 * Extension for [TestRestTemplate.postForObject] providing a `postForObject<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.postForObject(url: URI, request: Any? = null): T? =
		postForObject(url, request, T::class.java)

/**
 * Extension for [TestRestTemplate.postForEntity] providing a `postForEntity<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.postForEntity(url: String, request: Any? = null,
															vararg uriVariables: Any): ResponseEntity<T> =
		postForEntity(url, request, T::class.java, *uriVariables)

/**
 * Extension for [TestRestTemplate.postForEntity] providing a `postForEntity<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.postForEntity(url: String, request: Any? = null,
															uriVariables: Map<String, *>): ResponseEntity<T> =
		postForEntity(url, request, T::class.java, uriVariables)

/**
 * Extension for [TestRestTemplate.postForEntity] providing a `postForEntity<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. Like the original Java method, this
 * extension is subject to type erasure. Use [exchange] if you need to retain actual
 * generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.postForEntity(url: URI, request: Any? = null): ResponseEntity<T> =
		postForEntity(url, request, T::class.java)

/**
 * Extension for [TestRestTemplate.exchange] providing an `exchange<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. This extension is not subject to
 * type erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.exchange(url: String, method: HttpMethod,
		requestEntity: HttpEntity<*>? = null, vararg uriVariables: Any): ResponseEntity<T> =
		exchange(url, method, requestEntity, object : ParameterizedTypeReference<T>() {}, *uriVariables)

/**
 * Extension for [TestRestTemplate.exchange] providing an `exchange<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. This extension is not subject to
 * type erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.exchange(url: String, method: HttpMethod,
		requestEntity: HttpEntity<*>? = null, uriVariables: Map<String, *>): ResponseEntity<T> =
		exchange(url, method, requestEntity, object : ParameterizedTypeReference<T>() {}, uriVariables)

/**
 * Extension for [TestRestTemplate.exchange] providing an `exchange<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. This extension is not subject to
 * type erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.exchange(url: URI, method: HttpMethod,
		requestEntity: HttpEntity<*>? = null): ResponseEntity<T> =
		exchange(url, method, requestEntity, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [TestRestTemplate.exchange] providing an `exchange<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. This extension is not subject to
 * type erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
@Throws(RestClientException::class)
inline fun <reified T : Any> TestRestTemplate.exchange(requestEntity: RequestEntity<*>): ResponseEntity<T> =
		exchange(requestEntity, object : ParameterizedTypeReference<T>() {})
