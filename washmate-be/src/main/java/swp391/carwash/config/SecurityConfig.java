package swp391.carwash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF bảo mật (bắt buộc phải tắt khi làm REST API test Postman)
                .csrf(csrf -> csrf.disable())

                // 2. Cấu hình CORS mở cho Front-end và Swagger gọi vào
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    return config;
                }))

                // 3. Đưa Session về dạng STATELESS không lưu trạng thái rác trên Server
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Cấu hình phân quyền endpoint mở rộng
                .authorizeHttpRequests(auth -> auth
                        // Cho phép xem tài liệu Swagger UI thoải mái
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Mở hoàn toàn cụm API v1 để chạy mượt mà trên Postman không bị chặn
                        .requestMatchers("/api/v1/**").permitAll()

                        // Tất cả các request ngoài luồng trên thì mới bắt nhập token
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}