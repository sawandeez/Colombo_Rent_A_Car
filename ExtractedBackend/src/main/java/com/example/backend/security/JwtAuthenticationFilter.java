package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Avoid re-processing framework error dispatches.
        return "/error".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt = extractToken(authHeader);

        if (jwt == null || jwt.isEmpty()) {
            log.debug("JWT_FILTER - No token found for {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            log.debug("JWT_FILTER - Extracted username from token: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.debug("JWT_FILTER - Loaded UserDetails for: {}, Authorities: {}", userEmail, userDetails.getAuthorities());

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT_FILTER - SecurityContext authentication set for user: {} with authorities: {}",
                            userEmail, userDetails.getAuthorities());
                } else {
                    log.debug("JWT_FILTER - Token validation failed for user: {}", userEmail);
                }
            } else if (userEmail == null) {
                log.debug("JWT_FILTER - Could not extract username from token for {} {}", request.getMethod(), request.getRequestURI());
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.debug("JWT_FILTER - Exception during JWT processing for {} {}: {}",
                    request.getMethod(), request.getRequestURI(), ex.getMessage());
            // Ignore invalid JWT and continue; protected endpoints will return 401 via security config.
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String trimmedHeader = authHeader.trim();
        if (trimmedHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmedHeader.substring(7).trim();
        }

        // Backward compatibility for clients sending raw JWT without Bearer prefix.
        return trimmedHeader;
    }
}
