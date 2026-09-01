package com.campusqueue.security;

import com.campusqueue.entity.User;
import com.campusqueue.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails implementation wrapping CampusQueue User entity.
 * Contains user id, email, role, and display name.
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String name, String email, String passwordHash, UserRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static CustomUserDetails fromEntity(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
