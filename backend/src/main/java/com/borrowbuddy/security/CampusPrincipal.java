package com.borrowbuddy.security;

import com.borrowbuddy.domain.CampusUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.List;

public final class CampusPrincipal extends User {
    private final Long id;
    private final String campus;
    public CampusPrincipal(CampusUser user) {
        super(user.email, user.password, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        this.id = user.id; this.campus = user.campus;
    }
    public Long id() { return id; }
    public String campus() { return campus; }
}
