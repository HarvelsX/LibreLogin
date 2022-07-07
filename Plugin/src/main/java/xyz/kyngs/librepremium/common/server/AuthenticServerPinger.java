package xyz.kyngs.librepremium.common.server;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import xyz.kyngs.librepremium.api.server.ServerPing;
import xyz.kyngs.librepremium.api.server.ServerPinger;
import xyz.kyngs.librepremium.common.AuthenticLibrePremium;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AuthenticServerPinger<S> implements ServerPinger<S> {
    private final LoadingCache<S, Optional<ServerPing>> pingCache;
    private final AuthenticLibrePremium<?, S> plugin;

    public AuthenticServerPinger(AuthenticLibrePremium<?, S> plugin) {
        this.pingCache = Caffeine.newBuilder()
                .refreshAfterWrite(15, TimeUnit.SECONDS)
                .build(server -> Optional.ofNullable(plugin.getPlatformHandle().ping(server)));
        this.plugin = plugin;
    }

    @Override
    public ServerPing getLatestPing(S server) {
        return pingCache.get(server).orElse(null);
    }

    public void load() {
        var handle = plugin.getPlatformHandle();
        handle.getServers().parallelStream()
                .filter(server -> {
                    var name = handle.getServerName(server);

                    return plugin.getConfiguration().getLimbo().contains(name) || plugin.getConfiguration().getPassThrough().contains(name);
                })
                .forEach(this::getLatestPing);
    }
}
