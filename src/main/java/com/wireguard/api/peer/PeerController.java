package com.wireguard.api.peer;

import com.wireguard.api.AppError;
import com.wireguard.api.BadRequestException;
import com.wireguard.api.ResourceNotFoundException;
import com.wireguard.api.converters.*;
import com.wireguard.api.dto.*;
import com.wireguard.external.wireguard.ParsingException;
import com.wireguard.external.wireguard.PeerUpdateRequest;
import com.wireguard.external.wireguard.peer.CreatedPeer;
import com.wireguard.external.wireguard.peer.WgPeer;
import com.wireguard.external.wireguard.peer.WgPeerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Validated
public class PeerController {

    private final WgPeerService wgPeerService;

    private final PeerCreationRequestFromDTOConverter creationRequestConverter = new PeerCreationRequestFromDTOConverter();
    private final PageRequestFromDTOConverter pageRequestConverter = new PageRequestFromDTOConverter();
    private final PeerUpdateRequestFromDTOConverter updateRequestConverter = new PeerUpdateRequestFromDTOConverter();
    private final WgPeerDTOFromWgPeerConverter peerDTOConverter = new WgPeerDTOFromWgPeerConverter();
    private final PageDTOFromPageTypeChangeConverter<WgPeer, WgPeerDTO> pageDTOConverter = new PageDTOFromPageTypeChangeConverter<>(peerDTOConverter);


    public PeerController(WgPeerService wgPeerService) {
        this.wgPeerService = wgPeerService;
    }




    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = WgPeerDTO.class))
                    )
            }
            ),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppError.class)) }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppError.class)) }) })
    @GetMapping("/peers")
    @Parameter(name = "page", description = "Page number")
    @Parameter(name = "limit", description = "Page size (In case of 0, all peers will be returned)", schema = @Schema(defaultValue = "100"))
    @Parameter(name = "sort", description = "Sort key and direction separated by a dot. The keys are the same as in the answer. " +
            "Direction is optional and may have value DESC (High to low) and ASC (Low to high). Using with a large number of the peers (3000 or more) affects performance. ",
            example = "allowedSubnets.desc")
    @Parameter(name = "pageRequestDTO", hidden = true)
    public PageDTO<WgPeerDTO> getPeers(
            @Valid PageRequestDTO pageRequestDTO
    ) throws ParsingException {
        Page<WgPeer> peers;
        try {
            Pageable pageable = pageRequestConverter.convert(pageRequestDTO);
            peers = wgPeerService.getPeers(pageable);

        } catch (ParsingException e){
            throw new BadRequestException(e.getMessage());
        }
        return pageDTOConverter.convert(peers);
    }

    @Operation(summary = "Update peer by public key", description = "Update peer by public key. " +
            "Do not provide fields that you do not want to change.")
    @Parameter(name = "publicKey", description = "Current public key of the peer", required = true)
    @Parameter(name = "newPublicKey", description = "New public key of the peer. Warning: If you change the public key, latest handshake and transfer data will be lost. ")
    @Parameter(name = "presharedKey", description = "Preshared key or empty if no psk required (Empty if not provided)",
            allowEmptyValue = true)
    @Parameter(name = "endpoint", description = "Socket IP:port ",
            allowEmptyValue = true)
    @Parameter(name = "allowedIps", description = "New ips of the peer (Exists will be replaced)  Example: 10.0.0.11/32",
            array = @ArraySchema(arraySchema = @Schema(implementation = String.class), uniqueItems=true), allowEmptyValue = true)
    @Parameter(name = "persistentKeepalive", description = "New persistent keepalive interval in seconds (0 if not provided)", schema = @Schema(implementation = Integer.class, defaultValue = "0", example = "0", minimum = "0", maximum = "65535"))
    @Parameter(name = "peerUpdateRequestDTO", hidden = true)
    @RequestMapping(value = "/peer/update", method = RequestMethod.PATCH)
    public WgPeerDTO updatePeer(
            @Valid PeerUpdateRequestDTO peerUpdateRequestDTO
    ){
        WgPeer wgPeer = wgPeerService.updatePeer(
                Objects.requireNonNull(updateRequestConverter.convert(peerUpdateRequestDTO)));
        return peerDTOConverter.convert(wgPeer);
    }







    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WgPeerDTO.class)
                            )
                    }
            ),
            @ApiResponse(responseCode = "404", description = "Not Found",
                 content = { @Content(mediaType = "application/json",
                         schema = @Schema(implementation = AppError.class)) }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppError.class)) }) })
    @Parameter(name = "publicKey", description = "The public key of the peer to be found", required = true)
    @Parameter(name = "publicKeyDTO", hidden = true)
    @GetMapping("/peer")
    public WgPeerDTO getPeerByPublicKey(
            @Valid PublicKeyDTO publicKeyDTO) throws ParsingException {
        Optional<WgPeer> peer =  wgPeerService.getPeerByPublicKey(publicKeyDTO.getValue());
        if (peer.isPresent()){
            return peerDTOConverter.convert(peer.get());
        } else {
            throw new ResourceNotFoundException("Peer with public key %s not found".formatted(publicKeyDTO.getValue()));
        }
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CreatedPeerDTO.class)
                            )
                    }
            ),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppError.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad request (Invalid parameters values)",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppError.class)) })})
    @PostMapping("/peer/create")
    @Parameter(name = "publicKey", description = "Public key of the peer (Will be generated if not provided)")
    @Parameter(name = "presharedKey", description = "Preshared key or empty if no psk required (Will be generated if not provided)", allowEmptyValue = true)
    @Parameter(name = "privateKey", description = "Private key of the peer " +
            "(Will be generated if not provided. " +
            "If provided public key, empty string will be returned)")
    @Parameter(name = "allowedIps", description = "Ip of new peer in wireguard network interface, or empty if no" +
                " address is required (Will be generated if not provided). Example: 10.0.0.11/32", array = @ArraySchema(arraySchema = @Schema(implementation = String.class), uniqueItems=true),allowEmptyValue = true)
    @Parameter(name = "persistentKeepalive", description = "Persistent keepalive interval in seconds (0 if not provided)", schema = @Schema(implementation = Integer.class, defaultValue = "0", example = "0", minimum = "0", maximum = "65535"))
    @Parameter(name = "peerCreationRequestDTO", hidden = true)
    public ResponseEntity<CreatedPeerDTO> createPeer(
            @Valid PeerCreationRequestDTO peerCreationRequestDTO
    ) {
        CreatedPeer createdPeer = wgPeerService.createPeerGenerateNulls(
                Objects.requireNonNull(creationRequestConverter.convert(peerCreationRequestDTO))
        );
        return new ResponseEntity<>(CreatedPeerDTO.from(createdPeer), HttpStatus.CREATED);
    }



    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WgPeerDTO.class)
                            )
                    }
            ),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppError.class)) }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppError.class)) }) })


    @Parameter(name = "publicKey", description = "The public key of the peer to be deleted", required = true)
    @DeleteMapping("/peer/delete")
    public WgPeerDTO deletePeer(@Valid PublicKeyDTO publicKeyDTO) {
        WgPeer deletedPeer = wgPeerService.deletePeer(publicKeyDTO.getValue());
        return peerDTOConverter.convert(deletedPeer);
    }



}
