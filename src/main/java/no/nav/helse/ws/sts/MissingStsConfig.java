package no.nav.helse.ws.sts;

public class MissingStsConfig extends RuntimeException {
    public MissingStsConfig(String message) {
        super(message);
    }
}
