package com.wirerest.api.openAPI.schemas.samples;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Request DTO for creating a new peer.") 
public class PeerCreationRequestSchema {
    @Valid
    @Schema(description = "Public key of the peer.", required = true)
    private String publicKey;
    @Valid
    @Nullable
    @Schema(description = "Preshared key for the peer (optional).")
    private String presharedKey;
    @Valid
    @Nullable
    @Schema(description = "Private key of the peer (optional).", hidden = true) 
    private String privateKey;
    @Nullable
    @Schema(description = "Set of allowed IPs for the peer (optional).") 
    private Set<String> allowedIps;
    @Nullable
    @Min(0)
    @Max(65535)
    @Schema(description = "Persistent keepalive interval for the peer (optional).", minimum = "0", maximum = "65535")
    private Integer persistentKeepalive;

}
