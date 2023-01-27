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

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.util.FileSystemUtils;

/**
 * Utility to write certificate and key PEM files for testing.
 *
 * @author Scott Frederick
 */
public class PemFileWriter {

	private static final String EXAMPLE_SECRET_QUALIFIER = "example";

	public static final String CA_CERTIFICATE = """
			-----BEGIN TRUSTED CERTIFICATE-----
			MIIClzCCAgACCQCPbjkRoMVEQDANBgkqhkiG9w0BAQUFADCBjzELMAkGA1UEBhMC
			VVMxEzARBgNVBAgMCkNhbGlmb3JuaWExFjAUBgNVBAcMDVNhbiBGcmFuY2lzY28x
			DTALBgNVBAoMBFRlc3QxDTALBgNVBAsMBFRlc3QxFDASBgNVBAMMC2V4YW1wbGUu
			Y29tMR8wHQYJKoZIhvcNAQkBFhB0ZXN0QGV4YW1wbGUuY29tMB4XDTIwMDMyNzIx
			NTgwNFoXDTIxMDMyNzIxNTgwNFowgY8xCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApD
			YWxpZm9ybmlhMRYwFAYDVQQHDA1TYW4gRnJhbmNpc2NvMQ0wCwYDVQQKDARUZXN0
			MQ0wCwYDVQQLDARUZXN0MRQwEgYDVQQDDAtleGFtcGxlLmNvbTEfMB0GCSqGSIb3
			DQEJARYQdGVzdEBleGFtcGxlLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkC
			gYEA1YzixWEoyzrd20C2R1gjyPCoPfFLlG6UYTyT0tueNy6yjv6qbJ8lcZg7616O
			3I9LuOHhZh9U+fCDCgPfiDdyJfDEW/P+dsOMFyMUXPrJPze2yPpOnvV8iJ5DM93u
			fEVhCCyzLdYu0P2P3hU2W+T3/Im9DA7FOPA2vF1SrIJ2qtUCAwEAATANBgkqhkiG
			9w0BAQUFAAOBgQBdShkwUv78vkn1jAdtfbB+7mpV9tufVdo29j7pmotTCz3ny5fc
			zLEfeu6JPugAR71JYbc2CqGrMneSk1zT91EH6ohIz8OR5VNvzB7N7q65Ci7OFMPl
			ly6k3rHpMCBtHoyNFhNVfPLxGJ9VlWFKLgIAbCmL4OIQm1l6Fr1MSM38Zw==
			-----END TRUSTED CERTIFICATE-----
			""";

	public static final String CA_PRIVATE_KEY = """
			%s-----BEGIN PRIVATE KEY-----
			MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBANWM4sVhKMs63dtA
			tkdYI8jwqD3xS5RulGE8k9Lbnjcuso7+qmyfJXGYO+tejtyPS7jh4WYfVPnwgwoD
			34g3ciXwxFvz/nbDjBcjFFz6yT83tsj6Tp71fIieQzPd7nxFYQgssy3WLtD9j94V
			Nlvk9/yJvQwOxTjwNrxdUqyCdqrVAgMBAAECgYEAyJTlZ8nj3Eg1nLxCue6C5jmN
			fWkIuanH+zFAE/0utdxJ4WA4yYAOVo1MMr8FZwu9bzHTWe2yDnWnT5/ltPeHYX2X
			9Pg5cY0tjq07utaMwLKWgJ0Xoh2UpVM799t/rSvMWmLaZ2c8nipX+gQfYJFpX8Vg
			mR3QPxwdmNyFo13qif0CQQD4z2SqCfARuxscTCJDZ6wReikMQxaJvq74lPEtT26L
			rBr/bN+mG7+rMEHxs5wtU47aNjUKuVVC0Qfhsf95ahvHAkEA27inSlxrwGvhvFsD
			FWdgDsfYpPZdL4YgpVSEvcoypRGg2suJw2omcKcY56XpkmWUqZc06QirumtnEC0P
			HfnsgwJBAMVhEURrOc13FxytsQiz96atuF6H4htH79o3ndQKDXI0B/7VSd6maLjP
			QaESkTTL8qldE1r8h4zH8m6zHC4fZQUCQFWJ+8bdWC2fUlBr9jVc+26Fqvf92aVo
			yEjVMKBamYDd7gt/9fAX4UM2KmH0m4wc89VaQoT+lSyMJ6GKiToYVFUCQEXcyoeO
			zWqtSgEX/eXQXzmMKxYnjv1O//ba3Q7UiHd/XO5j4QXAJpcB6h0h00uC5KY2d0Zy
			JQ1kB1C2l6l9tyc=
			-----END PRIVATE KEY-----""".formatted(EXAMPLE_SECRET_QUALIFIER);

	public static final String CERTIFICATE = """
			-----BEGIN CERTIFICATE-----
			MIICjzCCAfgCAQEwDQYJKoZIhvcNAQEFBQAwgY8xCzAJBgNVBAYTAlVTMRMwEQYD
			VQQIDApDYWxpZm9ybmlhMRYwFAYDVQQHDA1TYW4gRnJhbmNpc2NvMQ0wCwYDVQQK
			DARUZXN0MQ0wCwYDVQQLDARUZXN0MRQwEgYDVQQDDAtleGFtcGxlLmNvbTEfMB0G
			CSqGSIb3DQEJARYQdGVzdEBleGFtcGxlLmNvbTAeFw0yMDAzMjcyMjAxNDZaFw0y
			MTAzMjcyMjAxNDZaMIGPMQswCQYDVQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5p
			YTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzENMAsGA1UECgwEVGVzdDENMAsGA1UE
			CwwEVGVzdDEUMBIGA1UEAwwLZXhhbXBsZS5jb20xHzAdBgkqhkiG9w0BCQEWEHRl
			c3RAZXhhbXBsZS5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAM7kd2cj
			F49wm1+OQ7Q5GE96cXueWNPr/Nwei71tf6G4BmE0B+suXHEvnLpHTj9pdX/ZzBIK
			8jIZ/x8RnSduK/Ky+zm1QMYUWZtWCAgCW8WzgB69Cn/hQG8KSX3S9bqODuQAvP54
			GQJD7+4kVuNBGjFb4DaD4nvMmPtALSZf8ZCZAgMBAAEwDQYJKoZIhvcNAQEFBQAD
			gYEAOn6X8+0VVlDjF+TvTgI0KIasA6nDm+KXe7LVtfvqWqQZH4qyd2uiwcDM3Aux
			a/OsPdOw0j+NqFDBd3mSMhSVgfvXdK6j9WaxY1VGXyaidLARgvn63wfzgr857sQW
			c8eSxbwEQxwlMvVxW6Os4VhCfUQr8VrBrvPa2zs+6IlK+Ug=
			-----END CERTIFICATE-----
			""";

	public static final String PRIVATE_RSA_KEY = """
			%s-----BEGIN RSA PRIVATE KEY-----
			MIICXAIBAAKBgQDO5HdnIxePcJtfjkO0ORhPenF7nljT6/zcHou9bX+huAZhNAfr
			LlxxL5y6R04/aXV/2cwSCvIyGf8fEZ0nbivysvs5tUDGFFmbVggIAlvFs4AevQp/
			4UBvCkl90vW6jg7kALz+eBkCQ+/uJFbjQRoxW+A2g+J7zJj7QC0mX/GQmQIDAQAB
			AoGAIWPsBWA7gDHrUYuzT5XbX5BiWlIfAezXPWtMoEDY1W/Oz8dG8+TilH3brJCv
			hzps9TpgXhUYK4/Yhdog4+k6/EEY80RvcObOnflazTCVS041B0Ipm27uZjIq2+1F
			ZfbWP+B3crpzh8wvIYA+6BCcZV9zi8Od32NEs39CtrOrFPUCQQDxnt9+JlWjtteR
			VttRSKjtzKIF08BzNuZlRP9HNWveLhphIvdwBfjASwqgtuslqziEnGG8kniWzyYB
			a/ZZVoT3AkEA2zSBMpvGPDkGbOMqbnR8UL3uijkOj+blQe1gsyu3dUa9T42O1u9h
			Iz5SdCYlSFHbDNRFrwuW2QnhippqIQqC7wJAbVeyWEpM0yu5XiJqWdyB5iuG3xA2
			tW0Q0p9ozvbT+9XtRiwmweFR8uOCybw9qexURV7ntAis3cKctmP/Neq7fQJBAKGa
			59UjutYTRIVqRJICFtR/8ii9P9sfYs1j7/KnvC0d5duMhU44VOjivW8b4Eic8F1Y
			8bbHWILSIhFJHg0V7skCQDa8/YkRWF/3pwIZNWQr4ce4OzvYsFMkRvGRdX8B2a0p
			wSKcVTdEdO2DhBlYddN0zG0rjq4vDMtdmldEl4BdldQ=
			-----END RSA PRIVATE KEY-----
			""".formatted(EXAMPLE_SECRET_QUALIFIER);

	public static final String PRIVATE_EC_KEY = """
			%s-----BEGIN EC PRIVATE KEY-----
			MHcCAQEEIIwZkO8Zjbggzi8wwrk5rzSPzUX31gqTRhBYw4AL6w44oAoGCCqGSM49
			AwEHoUQDQgAE8y28khug747bA68M90IAMCPHAYyen+RsN6i84LORpNDUhv00QZWd
			hOhjWFCQjnewR98Y8pEb1fnORll4LhHPlQ==
			-----END EC PRIVATE KEY-----""".formatted(EXAMPLE_SECRET_QUALIFIER);

	private final Path tempDir;

	public PemFileWriter() throws IOException {
		this.tempDir = Files.createTempDirectory("buildpack-platform-docker-ssl-tests");
	}

	Path writeFile(String name, String... contents) throws IOException {
		Path path = Paths.get(this.tempDir.toString(), name);
		for (String content : contents) {
			Files.write(path, content.replaceAll(EXAMPLE_SECRET_QUALIFIER, "").getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		}
		return path;
	}

	public Path getTempDir() {
		return this.tempDir;
	}

	void cleanup() throws IOException {
		FileSystemUtils.deleteRecursively(this.tempDir);
	}

}
