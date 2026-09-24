package org.example.portal.config;

import org.example.portal.repository.EmployeeRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.time.*;

@Configuration
public class SecurityConfig {
    @Bean org.springframework.security.core.session.SessionRegistry sessionRegistry() { return new org.springframework.security.core.session.SessionRegistryImpl(); }
    @Bean org.springframework.security.web.session.HttpSessionEventPublisher sessionEvents() { return new org.springframework.security.web.session.HttpSessionEventPublisher(); }
    @Bean Clock portalClock() { return Clock.system(ZoneId.of("Asia/Seoul")); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(EmployeeRepository employees) {
        return login -> {
            var e = employees.findByLoginId(login).orElseThrow(() -> new UsernameNotFoundException("계정을 확인하세요."));
            return User.withUsername(e.getLoginId()).password(e.getPassword()).roles(e.getRole().name()).disabled(e.isDeleted() || !e.isActive()).build();
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http,org.springframework.security.core.session.SessionRegistry sessionRegistry) throws Exception {
        return http.authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/css/**", "/error").permitAll()
                .requestMatchers("/groupware/tasks", "/groupware/tasks/**", "/groupware/documents", "/groupware/documents/**", "/groupware/amendments", "/groupware/amendments/**").hasRole("EMPLOYEE")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasRole("EMPLOYEE")
                .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").successHandler((request, response, auth) ->
                response.sendRedirect(request.getContextPath() + (auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN")) ? "/admin/main" : "/employee/main"))).permitAll())
            .sessionManagement(s -> s.maximumSessions(-1).sessionRegistry(sessionRegistry))
            .logout(l -> l.logoutSuccessUrl("/login?logout"))
            .build();
    }
}
