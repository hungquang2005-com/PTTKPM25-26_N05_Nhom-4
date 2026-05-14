package com.hotel.hotelmanagement.config;

import com.hotel.hotelmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    // ============================================================
    // FILTER CHAIN 1 – ADMIN
    // ============================================================
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {

        http
            .securityMatcher("/admin", "/admin/**")

            .csrf(csrf -> csrf.disable())

            /*
             * FIX #2: Spring Security 6 yêu cầu sessionManagement và
             * maximumSessions phải được cấu hình riêng qua sessionManagement()
             * đúng kiểu lambda — không thể chain .maximumSessions() trực tiếp
             * từ SessionManagementConfigurer.
             * Trước đây cú pháp sai khiến filter chain bị misconfigure,
             * kết quả là mọi request (kể cả /admin/login POST) bị chặn → 403.
             */
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )

            .authenticationProvider(authProvider())

            .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập trang login (GET) và xử lý login (POST) không cần auth
                .requestMatchers("/admin/login", "/admin/login/**").permitAll()
                .anyRequest().hasRole("ADMIN")
            )

            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin", true)
                /*
                 * FIX #2: failureUrl phải trỏ đúng về trang login admin,
                 * KHÔNG dùng /login (trang login của user).
                 * Khi đăng nhập sai, Spring Security redirect về URL này
                 * và Thymeleaf hiển thị thông báo lỗi qua param ?error.
                 */
                .failureUrl("/admin/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout", "GET"))
                .logoutSuccessUrl("/admin/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            
            .headers(h -> h
                .frameOptions(fo -> fo.sameOrigin())
                // Ngăn trình duyệt cache trang admin sau khi đăng xuất
                // Khi nhấn nút ← back, trình duyệt sẽ gọi lại server thay vì dùng cache
                .cacheControl(cache -> {})
            );

        return http.build();
    }

    // ============================================================
    // FILTER CHAIN 2 – USER
    // ============================================================
    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {

        http
            .authenticationProvider(authProvider())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/rooms", "/rooms/**",
                    "/about", "/contact",
                    "/login", "/register",
                    "/css/**", "/js/**", "/images/**"
                ).permitAll()

                .requestMatchers("/booking/**", "/payment/**", "/invoice/**")
                    .authenticated()

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            
            .headers(h -> h
                .frameOptions(fo -> fo.sameOrigin())
                // Ngăn trình duyệt cache trang admin sau khi đăng xuất
                // Khi nhấn nút ← back, trình duyệt sẽ gọi lại server thay vì dùng cache
                .cacheControl(cache -> {})
            );

        return http.build();
    }
}