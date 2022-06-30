/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.actuator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartFile;

/**
 * The Sample rest controller endpoint.
 *
 * @author Guirong Hu
 */
@Component
@RestControllerEndpoint(id = "rest")
public class SampleRestControllerEndpoint {

	@GetMapping("/exception")
	public String exception() {
		throw new CustomException();
	}

	@PostMapping("/upload")
	public String upload(@RequestParam("file") MultipartFile file) throws IOException {
		return StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8);
	}

	@RestControllerAdvice
	static class CustomExceptionHandler {

		@ExceptionHandler(CustomException.class)
		ResponseEntity<String> handleCustomException(CustomException e) {
			return new ResponseEntity<>("this is a custom exception body", HttpStatus.I_AM_A_TEAPOT);
		}

	}

	static class CustomException extends RuntimeException {

	}

}
