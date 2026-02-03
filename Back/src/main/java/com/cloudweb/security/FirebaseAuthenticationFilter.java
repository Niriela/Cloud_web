package com.cloudweb.security;

import com.cloudweb.entity.User;
import com.cloudweb.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (token.startsWith("offline:")) {
                String payload = token.substring("offline:".length());
                String[] parts = payload.split(":");
                String idText = parts.length > 0 ? parts[0] : "";
                try {
                    Long userId = Long.parseLong(idText);
                    if (parts.length > 1) {
                        long expiresAt = Long.parseLong(parts[1]);
                        if (System.currentTimeMillis() > expiresAt) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Offline session expired\"}");
                            return;
                        }
                    }
                    Optional<User> user = userRepository.findById(userId);
                    if (user.isPresent()) {
                        FirebaseUserPrincipal principal = new FirebaseUserPrincipal(
                                "offline:" + userId,
                                user.get().getEmail()
                        );
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, token, List.of());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    // fall through to invalid token response
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Invalid offline token\"}");
                return;
            }
            try {
                FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
                FirebaseUserPrincipal principal = new FirebaseUserPrincipal(
                        decoded.getUid(),
                        decoded.getEmail()
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, token, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (FirebaseAuthException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Invalid Firebase token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
