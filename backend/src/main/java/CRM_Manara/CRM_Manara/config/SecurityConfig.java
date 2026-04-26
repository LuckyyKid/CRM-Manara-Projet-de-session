package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.service.userService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String DEFAULT_ALLOWED_ORIGINS = "https://manaracrm.netlify.app,https://crm-manara-projet-de-session.vercel.app,https://*.netlify.app";

    @Autowired
    private userService userService;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.cors.allowed-origins:" + DEFAULT_ALLOWED_ORIGINS + "}")
    private String allowedOrigins;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService);
        authProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(resolveAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/oauth2/**", configuration);
        source.registerCorsConfiguration("/login/oauth2/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PathPatternRequestMatcher.pathPattern("/api/**"))
                .authenticationProvider(authenticationProvider())
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/login", "/api/signUp/**", "/api/chatbot/**", "/api/public/**", "/api/stripe/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/parent/**").hasRole("PARENT")
                        .requestMatchers("/api/animateur/**").hasRole("ANIMATEUR")
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true}");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            logger.warn("Unauthorized API request: method={} path={} origin={} session={}",
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    request.getHeader("Origin"),
                                    request.getRequestedSessionId());
                            response.sendError(401);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            logger.warn("Forbidden API request: method={} path={} user={} session={}",
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName(),
                                    request.getRequestedSessionId());
                            response.sendError(403);
                        })
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider());

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/logout"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index",
                                "/about",
                                "/login",
                                "/register",
                                "/signUp",
                                "/css/**",
                                "/images/**",
                                "/avatars/**",
                                "/api/login",
                                "/api/signUp/**",
                                "/api/chatbot/**",
                                "/api/stripe/webhook",
                                "/ws/**",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/parent/**").hasRole("PARENT")
                        .requestMatchers("/animateur/**").hasRole("ANIMATEUR")
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(userService))
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            String redirectUrl;
                            if (exception.getMessage() != null
                                    && exception.getMessage().toLowerCase().contains("approbation")) {
                                redirectUrl = frontendBaseUrl + "/login?pending";
                                logger.warn("OAuth failure pending approval for {} -> {}",
                                        request.getRequestURI(),
                                        redirectUrl);
                                response.sendRedirect(redirectUrl);
                                return;
                            }
                            redirectUrl = frontendBaseUrl + "/login?oauthError";
                            logger.warn("OAuth failure on {} -> {} ({})",
                                    request.getRequestURI(),
                                    redirectUrl,
                                    exception.getMessage());
                            response.sendRedirect(redirectUrl);
                        })
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(request -> "/logout".equals(request.getServletPath())
                                && ("POST".equals(request.getMethod()) || "GET".equals(request.getMethod())))
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (!request.getServletPath().startsWith("/api/")) {
                                response.sendRedirect(frontendBaseUrl + "/login?logout");
                                return;
                            }

                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true}");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            logger.warn("Unauthorized web request: method={} path={} origin={} session={}",
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    request.getHeader("Origin"),
                                    request.getRequestedSessionId());
                            response.sendError(401);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            logger.warn("Forbidden web request: method={} path={} user={} session={}",
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName(),
                                    request.getRequestedSessionId());
                            response.sendError(403);
                        })
                );

        return http.build();
    }

    private List<String> resolveAllowedOriginPatterns() {
        Set<String> patterns = new LinkedHashSet<>();
        addOrigins(patterns, allowedOrigins);
        addOrigins(patterns, frontendBaseUrl);
        return new ArrayList<>(patterns);
    }

    private void addOrigins(Set<String> patterns, String source) {
        if (source == null || source.isBlank()) {
            return;
        }

        for (String value : source.split(",")) {
            String normalized = value.trim();
            if (!normalized.isBlank()) {
                patterns.add(normalized);
            }
        }
    }
}
