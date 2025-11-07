    package org.lovesoa.config;

    import org.lovesoa.security.jwt.JwtAuthenticationFilter;
    import org.lovesoa.security.utils.JwtAccessDeniedHandler;
    import org.lovesoa.security.utils.JwtAuthenticationEntryPoint;
    import org.lovesoa.security.utils.UserDetailsServiceImpl;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
    import org.springframework.boot.web.servlet.DispatcherType;
    import org.springframework.boot.web.servlet.FilterRegistrationBean;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.http.HttpMethod;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.config.Customizer;
    import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.security.web.access.intercept.AuthorizationFilter;
    import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
    import org.springframework.security.web.context.SecurityContextHolderFilter;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.cors.CorsConfigurationSource;
    import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

    import java.util.List;

    @Configuration
    @EnableWebSecurity
    public class SecurityConfig {

        @Autowired
        private UserDetailsServiceImpl userDetailsService;
        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private JwtAuthenticationEntryPoint authenticationEntryPoint;

        @Autowired
        private JwtAccessDeniedHandler accessDeniedHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .cors(Customizer.withDefaults())
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .securityContext(sc -> sc.securityContextRepository(new org.springframework.security.web.context.RequestAttributeSecurityContextRepository()))
                    // JWT ПОСЛЕ SecurityContextHolderFilter и ДО AuthorizationFilter:
                    .addFilterAfter(jwtAuthenticationFilter, SecurityContextHolderFilter.class)
                    .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler)
                    )
                    .authorizeHttpRequests(a -> a
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/auth/register", "/auth/login").permitAll()
                            .anyRequest().authenticated()
                    );

            return http.build();
        }

        @Bean
        public AuthenticationManager authManager(HttpSecurity http) throws Exception {
            AuthenticationManagerBuilder authenticationManagerBuilder =
                    http.getSharedObject(AuthenticationManagerBuilder.class);
            authenticationManagerBuilder
                    .userDetailsService(userDetailsService)
                    .passwordEncoder(passwordEncoder());
            return authenticationManagerBuilder.build();
        }

        @Bean
        public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterServletRegistration(JwtAuthenticationFilter f) {
            FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(f);
            reg.setEnabled(false);
            return reg;
        }


        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration cfg = new CorsConfiguration();
            // Vite по умолчанию на 5173
            cfg.setAllowedOriginPatterns(List.of("https://localhost:*", "https://127.0.0.1:*"));
            cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
            cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With"));
            cfg.setExposedHeaders(List.of("Authorization")); // если нужно
            cfg.setAllowCredentials(false); // для Bearer обычно false
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", cfg);
            return source;
        }
    }
