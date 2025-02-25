package com.wirerest.api.peer.controller;

import com.wirerest.api.AppError;
import com.wirerest.api.converters.PeerUpdateRequestFromDTOConverter;
import com.wirerest.api.converters.WgPeerDTOFromWgPeerConverter;
import com.wirerest.api.peer.PeerUpdateRequestDTO;
import com.wirerest.api.peer.WgPeerDTO;
import com.wirerest.wireguard.peer.WgPeer;
import com.wirerest.wireguard.peer.WgPeerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping(value = "v1/peers")
@Validated
public class UpdatePeerController {

	private final WgPeerService wgPeerService;

	private final PeerUpdateRequestFromDTOConverter updateRequestConverter = new PeerUpdateRequestFromDTOConverter();
	private final WgPeerDTOFromWgPeerConverter peerDTOConverter = new WgPeerDTOFromWgPeerConverter();

	public UpdatePeerController(WgPeerService wgPeerService) {
		this.wgPeerService = wgPeerService;
	}

	@Operation(summary = "Update peer by public key", description = "Update peer by public key. " +
			"Do not provide fields that you do not want to change.", tags = {
					"Peers"
	}, security = @SecurityRequirement(name = "Token"), responses = {
			@ApiResponse(responseCode = "200", description = "OK", content = {
					@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WgPeerDTO.class)), examples = {
							@ExampleObject(name = "Peer", ref = "#/components/examples/peer"),
					})
			}),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = AppError.class, name = "BadRequestExample"), examples = {
							@ExampleObject(name = "Invalid key format", ref = "#/components/examples/InvalidPubKey400")
					})
			}),
			@ApiResponse(responseCode = "409", description = "Conflict", content = {
					@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WgPeerDTO.class)), examples = {
							@ExampleObject(name = "Peer exists", ref = "#/components/examples/peerAlreadyExists409"),
							@ExampleObject(name = "Ip already used", ref = "#/components/examples/alreadyUsed409")
					})
			})
	})
	@Parameter(name = "publicKey",//
			description = "Current public key of the peer",//
			required = true,//
			schema = @Schema(type = "string"))
	@Parameter(name = "newPublicKey", //
			description = "New public key of the peer. Warning: If you change the public key, latest handshake and transfer data will be lost. ",//
			schema = @Schema(type = "string"))
	@Parameter(name = "presharedKey",//
			description = "Preshared key or empty if no psk required (Empty if not provided)",//
			allowEmptyValue = true,//
			schema = @Schema(type = "string"))
	@Parameter(name = "endpoint",//
			description = "Socket IP:port ",//
			allowEmptyValue = true,//
			schema = @Schema(type = "string"))
	@Parameter(name = "allowedIps", //
			description = "New ips of the peer (Exists will be replaced) Example: 10.0.0.11/32",//
			array = @ArraySchema(schema = @Schema(implementation = String.class), uniqueItems = true//
			//
			// ,schema = @Schema(implementation = String.class)//
			),//
			allowEmptyValue = true)
	// @Parameter(description = "List of allowed IP addresses", schema = @Schema(type = "array", example = "[\"192.168.1.1\", \"10.0.0.1\"]"))

	@Parameter(name = "persistentKeepalive", //
			description = "New persistent keepalive interval in seconds (0 if not provided)",//
			schema = @Schema(implementation = Integer.class,//
					defaultValue = "0",//
					example = "0",//
					minimum = "0",//
					maximum = "65535")//
	)
	@Parameter(name = "peerUpdateRequestDTO", hidden = true)
	@RequestMapping(method = RequestMethod.PATCH)
	public WgPeerDTO updatePeer(
			@Valid PeerUpdateRequestDTO peerUpdateRequestDTO) {
		WgPeer wgPeer = wgPeerService.updatePeer(
				Objects.requireNonNull(updateRequestConverter.convert(peerUpdateRequestDTO)));
		return peerDTOConverter.convert(wgPeer);
	}

}
