package com.jigmnnaga.global.auth;

import java.security.Principal;

public record StompPrincipal(Long memberId) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}
