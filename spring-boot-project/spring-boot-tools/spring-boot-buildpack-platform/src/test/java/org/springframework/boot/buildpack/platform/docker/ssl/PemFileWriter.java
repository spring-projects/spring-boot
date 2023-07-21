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
 * @author Moritz Halbritter
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

	public static final String PRIVATE_DSA_KEY = EXAMPLE_SECRET_QUALIFIER + "-----BEGIN PRIVATE KEY-----\n"
			+ "MIICXAIBADCCAjUGByqGSM44BAEwggIoAoIBAQCPeTXZuarpv6vtiHrPSVG28y7F\n"
			+ "njuvNxjo6sSWHz79NgbnQ1GpxBgzObgJ58KuHFObp0dbhdARrbi0eYd1SYRpXKwO\n"
			+ "jxSzNggooi/6JxEKPWKpk0U0CaD+aWxGWPhL3SCBnDcJoBBXsZWtzQAjPbpUhLYp\n"
			+ "H51kjviDRIZ3l5zsBLQ0pqwudemYXeI9sCkvwRGMn/qdgYHnM423krcw17njSVkv\n"
			+ "aAmYchU5Feo9a4tGU8YzRY+AOzKkwuDycpAlbk4/ijsIOKHEUOThjBopo33fXqFD\n"
			+ "3ktm/wSQPtXPFiPhWNSHxgjpfyEc2B3KI8tuOAdl+CLjQr5ITAV2OTlgHNZnAh0A\n"
			+ "uvaWpoV499/e5/pnyXfHhe8ysjO65YDAvNVpXQKCAQAWplxYIEhQcE51AqOXVwQN\n"
			+ "NNo6NHjBVNTkpcAtJC7gT5bmHkvQkEq9rI837rHgnzGC0jyQQ8tkL4gAQWDt+coJ\n"
			+ "syB2p5wypifyRz6Rh5uixOdEvSCBVEy1W4AsNo0fqD7UielOD6BojjJCilx4xHjG\n"
			+ "jQUntxyaOrsLC+EsRGiWOefTznTbEBplqiuH9kxoJts+xy9LVZmDS7TtsC98kOmk\n"
			+ "ltOlXVNb6/xF1PYZ9j897buHOSXC8iTgdzEpbaiH7B5HSPh++1/et1SEMWsiMt7l\n"
			+ "U92vAhErDR8C2jCXMiT+J67ai51LKSLZuovjntnhA6Y8UoELxoi34u1DFuHvF9ve\n"
			+ "BB4CHHBQgJ3ST6U8rIxoTqGe42TiVckPf1PoSiJy8GY=\n" + "-----END PRIVATE KEY-----\n";

	public static final String PKCS8_PRIVATE_EC_NIST_P256_KEY = EXAMPLE_SECRET_QUALIFIER
			+ "-----BEGIN PRIVATE KEY-----\n" + "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgd6SePFfpaTKFd1Gm\n"
			+ "+WeHZNkORkot5hx6X9elPdICL9ygCgYIKoZIzj0DAQehRANCAASnMAMgeFBv9ks0\n"
			+ "d0jP+utQ3mohwmxY93xljfaBofdg1IeHgDd4I4pBzPxEnvXrU3kcz+SgPZyH1ybl\n" + "P6mSXDXu\n"
			+ "-----END PRIVATE KEY-----\n";

	public static final String PKCS8_PRIVATE_EC_NIST_P384_KEY = EXAMPLE_SECRET_QUALIFIER
			+ "-----BEGIN PRIVATE KEY-----\n" + "MIG/AgEAMBAGByqGSM49AgEGBSuBBAAiBIGnMIGkAgEBBDCexXiWKrtrqV1+d1Tv\n"
			+ "t1n5huuw2A+204mQHRuPL9UC8l0XniJjx/PVELCciyJM/7+gBwYFK4EEACKhZANi\n"
			+ "AASHEELZSdrHiSXqU1B+/jrOCr6yjxCMqQsetTb0q5WZdCXOhggGXfbzlRynqphQ\n"
			+ "i4G7azBUklgLaXfxN5eFk6C+E38SYOR7iippcQsSR2ZsCiTk7rnur4b40gQ7IgLA\n" + "/sU=\n"
			+ "-----END PRIVATE KEY-----\n";

	public static final String PKCS8_PRIVATE_EC_PRIME256V1_KEY = EXAMPLE_SECRET_QUALIFIER
			+ "-----BEGIN PRIVATE KEY-----\n" + "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQg4dVuddgQ6enDvPPw\n"
			+ "Dd1mmS6FMm/kzTJjDVsltrNmRuSgCgYIKoZIzj0DAQehRANCAAR1WMrRADEaVj9m\n"
			+ "uoUfPhUefJK+lS89NHikQ0ZdkHkybyVKLFMLe1hCynhzpKQmnpgud3E10F0P2PZQ\n" + "L9RCEpGf\n"
			+ "-----END PRIVATE KEY-----\n";

	public static final String PKCS8_PRIVATE_EC_SECP256R1_KEY = EXAMPLE_SECRET_QUALIFIER
			+ "-----BEGIN PRIVATE KEY-----\n" + "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgU9+v5hUNnTKix8fe\n"
			+ "Pfz+NfXFlGxQZMReSCT2Id9PfKagCgYIKoZIzj0DAQehRANCAATeJg+YS4BrJ35A\n"
			+ "KgRlZ59yKLDpmENCMoaYUuWbQ9hqHzdybQGzQsrNJqgH0nzWghPwP4nFaLPN+pgB\n" + "bqiRgbjG\n"
			+ "-----END PRIVATE KEY-----\n";

	public static final String PKCS8_PRIVATE_RSA_KEY = EXAMPLE_SECRET_QUALIFIER + "-----BEGIN PRIVATE KEY-----\n"
			+ "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDR0KfxUw7MF/8R\n"
			+ "B5/YXOM7yLnoHYb/M/6dyoulMbtEdKKhQhU28o5FiDkHcEG9PJQLgqrRgAjl3VmC\n"
			+ "C9omtfZJQ2EpfkTttkJjnKOOroXhYE51/CYSckapBYCVh8GkjUEJuEfnp07cTfYZ\n"
			+ "FqViIgIWPZyjkzl3w4girS7kCuzNdDntVJVx5F/EsFwMA8n3C0QazHQoM5s00Fer\n"
			+ "6aTwd6AW0JD5QkADavpfzZ554e4HrVGwHlM28WKQQkFzzGu44FFXyVuEF3HeyVPu\n"
			+ "g8GRHAc8UU7ijVgJB5TmbvRGYowIErD5i4VvGLuOv9mgR3aVyN0SdJ1N7aJnXpeS\n"
			+ "QjAgf03jAgMBAAECggEBAIhQyzwj3WJGWOZkkLqOpufJotcmj/Wwf0VfOdkq9WMl\n"
			+ "cB/bAlN/xWVxerPVgDCFch4EWBzi1WUaqbOvJZ2u7QNubmr56aiTmJCFTVI/GyZx\n"
			+ "XqiTGN01N6lKtN7xo6LYTyAUhUsBTWAemrx0FSErvTVb9C/mUBj6hbEZ2XQ5kN5t\n"
			+ "7qYX4Lu0zyn7s1kX5SLtm5I+YRq7HSwB6wLy+DSroO71izZ/VPwME3SwT5SN+c87\n"
			+ "3dkklR7fumNd9dOpSWKrLPnq4aMko00rvIGc63xD1HrEpXUkB5v24YEn7HwCLEH7\n"
			+ "b8jrp79j2nCvvR47inpf+BR8FIWAHEOUUqCEzjQkdiECgYEA6ifjMM0f02KPeIs7\n"
			+ "zXd1lI7CUmJmzkcklCIpEbKWf/t/PHv3QgqIkJzERzRaJ8b+GhQ4zrSwAhrGUmI8\n"
			+ "kDkXIqe2/2ONgIOX2UOHYHyTDQZHnlXyDecvHUTqs2JQZCGBZkXyZ9i0j3BnTymC\n"
			+ "iZ8DvEa0nxsbP+U3rgzPQmXiQVMCgYEA5WN2Y/RndbriNsNrsHYRldbPO5nfV9rp\n"
			+ "cDzcQU66HRdK5VIdbXT9tlMYCJIZsSqE0tkOwTgEB/sFvF/tIHSCY5iO6hpIyk6g\n"
			+ "kkUzPcld4eM0dEPAge7SYUbakB9CMvA7MkDQSXQNFyZ0mH83+UikwT6uYHFh7+ox\n"
			+ "N1P+psDhXzECgYEA1gXLVQnIcy/9LxMkgDMWV8j8uMyUZysDthpbK3/uq+A2dhRg\n"
			+ "9g4msPd5OBQT65OpIjElk1n4HpRWfWqpLLHiAZ0GWPynk7W0D7P3gyuaRSdeQs0P\n"
			+ "x8FtgPVDCN9t13gAjHiWjnC26Py2kNbCKAQeJ/MAmQTvrUFX2VCACJKTcV0CgYAj\n"
			+ "xJWSUmrLfb+GQISLOG3Xim434e9keJsLyEGj4U29+YLRLTOvfJ2PD3fg5j8hU/rw\n"
			+ "Ea5uTHi8cdTcIa0M8X3fX8txD3YoLYh2JlouGTcNYOst8d6TpBSj3HN6I5Wj8beZ\n"
			+ "R2fy/CiKYpGtsbCdq0kdZNO18BgQW9kewncjs1GxEQKBgQCf8q34h6KuHpHSDh9h\n"
			+ "YkDTypk0FReWBAVJCzDNDUMhVLFivjcwtaMd2LiC3FMKZYodr52iKg60cj43vbYI\n"
			+ "frmFFxoL37rTmUocCTBKc0LhWj6MicI+rcvQYe1uwTrpWdFf1aZJMYRLRczeKtev\n" + "OWaE/9hVZ5+9pild1NukGpOydw==\n"
			+ "-----END PRIVATE KEY-----\n";

	public static final String PKCS8_PRIVATE_EC_ED25519_KEY = EXAMPLE_SECRET_QUALIFIER + "-----BEGIN PRIVATE KEY-----\n"
			+ "MC4CAQAwBQYDK2VwBCIEIJOKNTaIJQTVuEqZ+yvclnjnlWJG6F+K+VsNCOlWRda+\n" + "-----END PRIVATE KEY-----";

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
