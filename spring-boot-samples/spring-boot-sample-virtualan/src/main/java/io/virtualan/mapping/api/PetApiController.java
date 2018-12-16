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

package io.virtualan.mapping.api;

import javax.validation.Valid;

import io.virtualan.annotation.ApiVirtual;
import io.virtualan.annotation.VirtualService;
import io.virtualan.mapping.to.Pet;
import io.virtualan.mapping.to.PetList;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@VirtualService
@RestController
@RequestMapping("/pets")
public class PetApiController {

	@ApiVirtual
	@PostMapping(value = "/", produces = {
			MediaType.APPLICATION_JSON_VALUE }, consumes = {
					MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Void> addPet(@Valid @RequestBody Pet pet) {
		return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

	}

	@ApiVirtual
	@DeleteMapping(value = "/{petId}", produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Void> deletePet(@PathVariable("petId") Long petId) {
		return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

	}

	@ApiVirtual
	@GetMapping(value = "/", produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PetList> findAllPets() {
		return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

	}

	@ApiVirtual
	@GetMapping(value = "/{petId}", produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Pet> getPetById(@PathVariable("petId") Long petId) {
		return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

	}

	@ApiVirtual
	@PutMapping(value = "/{petId}", produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Pet> updatePet(@PathVariable("petId") Long petId,
			@Valid @RequestBody Pet pet) {
		return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

	}

}
