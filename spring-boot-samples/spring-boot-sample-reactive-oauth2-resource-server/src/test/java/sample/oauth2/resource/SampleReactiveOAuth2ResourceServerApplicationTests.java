/*
 * Copyright 2012-2019 the original author or authors.
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

package sample.oauth2.resource;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleReactiveOAuth2ResourceServerApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	private static MockWebServer server = new MockWebServer();

	private static final String VALID_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJzdWJqZWN0Iiwic2NvcGUiOiJtZXNzYWdlOnJlYWQi"
			+ "LCJleHAiOjQ2ODM4MDUxNDF9.h-j6FKRFdnTdmAueTZCdep45e6DPwqM68ZQ8doIJ1exi9YxAlbWzOwId6Bd0L5YmCmp63gGQgsBUBLzwnZQ8kLUgU"
			+ "OBEC3UzSWGRqMskCY9_k9pX0iomX6IfF3N0PaYs0WPC4hO1s8wfZQ-6hKQ4KigFi13G9LMLdH58PRMK0pKEvs3gCbHJuEPw-K5ORlpdnleUTQIwIN"
			+ "afU57cmK3KocTeknPAM_L716sCuSYGvDl6xUTXO7oPdrXhS_EhxLP6KxrpI1uD4Ea_5OWTh7S0Wx5LLDfU6wBG1DowN20d374zepOIEkR-Jnmr_Ql"
			+ "R44vmRqS5ncrF-1R0EGcPX49U6A";

	@BeforeAll
	public static void setup() throws Exception {
		server.start();
		String url = server.url("/.well-known/jwks.json").toString();
		server.enqueue(mockResponse());
		System.setProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", url);
	}

	@AfterAll
	public static void shutdown() throws Exception {
		server.shutdown();
		System.clearProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
	}

	@Test
	void getWhenValidTokenShouldBeOk() {
		this.webTestClient.get().uri("/").headers((headers) -> headers.setBearerAuth(VALID_TOKEN)).exchange()
				.expectStatus().isOk().expectBody(String.class).isEqualTo("Hello, subject!");
	}

	@Test
	void getWhenNoTokenShouldBeUnauthorized() {
		this.webTestClient.get().uri("/").exchange().expectStatus().isUnauthorized().expectHeader()
				.valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
	}

	private static MockResponse mockResponse() {
		String body = "{\"keys\":[{\"p\":\"2p-ViY7DE9ZrdWQb544m0Jp7Cv03YCSljqfim9pD4ALhObX0OrAznOiowTjwBky9JGffMw"
				+ "DBVSfJSD9TSU7aH2sbbfi0bZLMdekKAuimudXwUqPDxrrg0BCyvCYgLmKjbVT3zcdylWSog93CNTxGDPzauu-oc0XPNKCXnaDpNvE\""
				+ ",\"kty\":\"RSA\",\"q\":\"sP_QYavrpBvSJ86uoKVGj2AGl78CSsAtpf1ybSY5TwUlorXSdqapRbY69Y271b0aMLzlleUn9ZTBO"
				+ "1dlKV2_dw_lPADHVia8z3pxL-8sUhIXLsgj4acchMk4c9YX-sFh07xENnyZ-_TXm3llPLuL67HUfBC2eKe800TmCYVWc9U\",\"d\""
				+ ":\"bn1nFxCQT4KLTHqo8mo9HvHD0cRNRNdWcKNnnEQkCF6tKbt-ILRyQGP8O40axLd7CoNVG9c9p_-g4-2kwCtLJNv_STLtwfpCY7"
				+ "VN5o6-ZIpfTjiW6duoPrLWq64Hm_4LOBQTiZfUPcLhsuJRHbWqakj-kV_YbUyC2Ocf_dd8IAQcSrAU2SCcDebhDCWwRUFvaa9V5eq0"
				+ "851S9goaA-AJz-JXyePH6ZFr8JxmWkWxYZ5kdcMD-sm9ZbxE0CaEk32l4fE4hR-L8x2dDtjWA-ahKCZ091z-gV3HWtR2JOjvxoNRjxUo"
				+ "3UxaGiFJHWNIl0EYUJZu1Cb-5wIlEI7wPx5mwQ\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"one\",\"qi\":\"qS0OK4"
				+ "8M2CIAA6_4Wdw4EbCaAfcTLf5Oy9t5BOF_PFUKqoSpZ6JsT5H0a_4zkjt-oI969v78OTlvBKbmEyKO-KeytzHBAA5CsLmVcz0THrMSg6o"
				+ "XZqu66MPnvWoZN9FEN5TklPOvBFm8Bg1QZ3k-YMVaM--DLvhaYR95_mqaz50\",\"dp\":\"Too2NozLGD1XrXyhabZvy1E0EuaVFj0UHQ"
				+ "PDLSpkZ_2g3BK6Art6T0xmE8RYtmqrKIEIdlI3IliAvyvAx_1D7zWTTRaj-xlZyqJFrnXWL7zj8UxT8PkB-r2E-ILZ3NAi1gxIWezlBTZ8"
				+ "M6NfObDFmbTc_3tJkN_raISo8z_ziIE\",\"dq\":\"U0yhSkY5yOsa9YcMoigGVBWSJLpNHtbg5NypjHrPv8OhWbkOSq7WvSstBkF"
				+ "k5AtyFvvfZLMLIkWWxxGzV0t6f1MoxBtttLrYYyCxwihiiGFhLbAdSuZ1wnxcqA9bC7UVECvrQmVTpsMs8UupfHKbQBpZ8OWAqrn"
				+ "uYNNtG4_4Bt0\",\"n\":\"lygtuZj0lJjqOqIWocF8Bb583QDdq-aaFg8PesOp2-EDda6GqCpL-_NZVOflNGX7XIgjsWHcPsQHs"
				+ "V9gWuOzSJ0iEuWvtQ6eGBP5M6m7pccLNZfwUse8Cb4Ngx3XiTlyuqM7pv0LPyppZusfEHVEdeelou7Dy9k0OQ_nJTI3b2E1WBoHC5"
				+ "8CJ453lo4gcBm1efURN3LIVc1V9NQY_ESBKVdwqYyoJPEanURLVGRd6cQKn6YrCbbIRHjqAyqOE-z3KmgDJnPriljfR5XhSGyM9eq"
				+ "D9Xpy6zu_MAeMJJfSArp857zLPk-Wf5VP9STAcjyfdBIybMKnwBYr2qHMT675hQ\"}]}";
		return new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setResponseCode(200).setBody(body);
	}

}
