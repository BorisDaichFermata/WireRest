package com.wireguard.api.peer;

import com.wireguard.api.dto.RequiredWgKey;
import com.wireguard.api.dto.WgKey;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.units.qual.N;

import java.util.Set;

@AllArgsConstructor
@Data
public class PeerCreationRequestDTO {
    @Valid
    private final RequiredWgKey publicKey;
    @Valid
    @Nullable
    private final WgKey presharedKey;
    @Valid
    @Nullable
    private final WgKey privateKey;
    @Nullable
    private final Set<String> allowedIps;
    @Nullable
    @Min(0)
    @Max(65535)
    private final Integer persistentKeepalive;



}
