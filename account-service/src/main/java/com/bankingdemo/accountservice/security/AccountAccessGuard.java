package com.bankingdemo.accountservice.security;

import com.bankingdemo.common.exception.ForbiddenResourceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Enforces the extra business rule from requirement doc section 5.2:
 * CUSTOMER may only operate on the account whose ownerUserId matches
 * their own token subject ("sub"). TELLER/ADMIN bypass this check.
 */
@Component
public class AccountAccessGuard {

    public void checkAccess(Authentication authentication, String ownerUserId) {
        if (hasAnyRole(authentication, "ADMIN", "TELLER", "SERVICE")) {
            return;
        }

        String subject = extractSubject(authentication);
        if (subject == null || !subject.equals(ownerUserId)) {
            throw new ForbiddenResourceException("Không có quyền thao tác trên tài khoản này");
        }
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (String role : roles) {
                if (authority.getAuthority().equals("ROLE_" + role)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractSubject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }
}
