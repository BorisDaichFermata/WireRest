package com.wirerest.api.openAPI.examples;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.models.examples.Example;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AllArgsConstructor
public class IdentifiedExample extends Example {
	@JsonIgnore
	private final String key;
}
