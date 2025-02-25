package com.wirerest.wireguard.test;

import com.wirerest.network.Subnet;
import com.wirerest.network.SubnetV6;
import com.wirerest.wireguard.WgTool;
import com.wirerest.wireguard.iface.WgInterface;
import com.wirerest.wireguard.parser.WgShowDump;
import com.wirerest.wireguard.peer.WgPeer;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Profile("dev")
@Component
public class FakeWgTool extends WgTool {
    private final ConcurrentHashMap<String, WgPeer> peers = new ConcurrentHashMap<>();
    private final WgInterface wgInterface;
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private int keyCounter;
    Random random = new Random();

    public FakeWgTool() {
        super(1);
        wgInterface = new WgInterface(generatePrivateKey(), genPubKey(), 16666, 0);
        List.of(
                WgPeer.publicKey(genPubKey()).build(),
                WgPeer.publicKey(genPubKey()).presharedKey(generatePresharedKey()).build(),
                WgPeer.publicKey(genPubKey()).presharedKey(generatePresharedKey()).endpoint("10.0.0.1").build(),
                WgPeer.publicKey(genPubKey()).presharedKey(generatePresharedKey()).allowedIPv4Subnets(Set.of(Subnet.valueOf("10.0.0.2/32"))).build(),
                WgPeer.publicKey(genPubKey()).presharedKey(generatePresharedKey()).allowedIPv4Subnets(Set.of(Subnet.valueOf("10.0.0.3/32"))).build(),
                WgPeer.publicKey(genPubKey()).presharedKey(generatePresharedKey()).allowedIPv4Subnets(
                        Set.of(Subnet.valueOf("10.0.0.4/32"), Subnet.valueOf("10.0.0.5/32"))).build(),
                WgPeer.publicKey(genPubKey()).presharedKey(generatePresharedKey()).allowedIPv4Subnets(Set.of(Subnet.valueOf("10.0.0.6/32")))
                        .latestHandshake(100000).transferRx(12345).transferTx(54321).build(),
                WgPeer.publicKey(genPubKey()).presharedKey(generatePresharedKey()).allowedIps(Set.of(Subnet.valueOf("10.0.0.7/32"), SubnetV6.valueOf("::1/128")))
                        .latestHandshake(200000).transferRx(12345).transferTx(54321).build()
        ).forEach(peer -> peers.put(peer.getPublicKey(), peer));

        for (int i = 0; i < 600; i++) {
            String pubkey = genPubKey();
            peers.put(pubkey, WgPeer.publicKey(pubkey).presharedKey(generatePresharedKey())
                    .transferRx(random.nextInt(10000000))
                    .transferTx(random.nextInt(2400000))
                    .build());
        }
        keyCounter = peers.size() + 1;
    }

    @SneakyThrows
    @Override
    public WgShowDump showDump(String interfaceName) {
        for (int i = 0; i < 100; i++) {
            WgPeer peer = peers.values().stream().skip(random.nextInt(peers.size())).findFirst().get();
            if (random.nextBoolean()) {
                peer = WgPeer.from(peer).transferRx(peer.getTransferRx() + random.nextInt(10000000)).build();
            } else {
                peer = WgPeer.from(peer).transferTx(peer.getTransferTx() + random.nextInt(2400000)).build();
            }
            peers.put(peer.getPublicKey(), peer);
        }
        return new WgShowDump(wgInterface, peers.values().stream().toList());
    }

    private String genPubKey() {
        return generatePublicKey(generatePrivateKey());
    }

    @Override
    public String generatePrivateKey() {
        String key = "FakePrvKeyFakePrvKey" + String.format("%11d", keyCounter++);
        return base64Encoder.encodeToString(key.getBytes());
    }

    @SneakyThrows
    @Override
    synchronized public void addPeer(String interfaceName, WgPeer newPeer) {
        WgPeer.Builder peerBuilder = WgPeer.from(peers.getOrDefault(newPeer.getPublicKey(), newPeer));
        if (newPeer.getPresharedKey() != null) peerBuilder.presharedKey(newPeer.getPresharedKey());
        peerBuilder.allowedIPv4Subnets(newPeer.getAllowedSubnets().getIPv4Subnets());
        peerBuilder.allowedIPv6Subnets(newPeer.getAllowedSubnets().getIPv6Subnets());
        if (newPeer.getEndpoint() != null) peerBuilder.endpoint(newPeer.getEndpoint());
        peerBuilder.persistentKeepalive(newPeer.getPersistentKeepalive());
        peers.put(newPeer.getPublicKey(), peerBuilder.build());
    }

    @Override
    synchronized public void deletePeer(String interfaceName, String publicKey) {
        peers.remove(publicKey);
    }


    @Override
    public String generatePublicKey(String privateKey) {
        String key = "pub" + privateKey.substring(44 - 7) + "FakePubKey" + String.format("%11d", keyCounter++);
        return base64Encoder.encodeToString(key.getBytes());
    }

    @Override
    public String generatePresharedKey() {
        String key = "FakePskKeyFakePskKey" + String.format("%11d", keyCounter++);
        return base64Encoder.encodeToString(key.getBytes());
    }


}
