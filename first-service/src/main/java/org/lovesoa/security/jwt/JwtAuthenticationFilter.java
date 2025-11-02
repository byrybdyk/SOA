package org.lovesoa.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    @Override protected boolean shouldNotFilterAsyncDispatch() { return false; }
    @Override protected boolean shouldNotFilterErrorDispatch() { return false; }

    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        System.out.println("authHeader = " + authHeader);
        logger.info("Processing request: {}", request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No Bearer token found, passing request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            System.out.println("[JWT Filter] Token valid, username: " + username);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


            if (username != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                System.out.println("[JWT Filter] User found: " + userDetails.getUsername());
                System.out.println("[JWT Filter] Roles: " + userDetails.getAuthorities());

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    var ctx = SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(auth);
                    System.out.println("Before setContext: {}" + SecurityContextHolder.getContext().getAuthentication());

                    SecurityContextHolder.setContext(ctx);

                    System.out.println("After setContext: {}" + SecurityContextHolder.getContext().getAuthentication());
                }
                else {
                    System.out.println("[JWT Filter] Invalid token");
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            logger.error("Authentication error for request {}: {}", request.getRequestURI(), exception.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
