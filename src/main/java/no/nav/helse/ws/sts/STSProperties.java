package no.nav.helse.ws.sts;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class STSProperties {

    @NotNull
    private final URI url;

    @NotNull
    private final String username;

    @NotNull
    private final String password;

    public STSProperties(@NotNull URI url, @NotNull String username, @NotNull String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @NotNull
    public URI getUrl() {
        return url;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @NotNull
    public String getPassword() {
        return password;
    }
}
