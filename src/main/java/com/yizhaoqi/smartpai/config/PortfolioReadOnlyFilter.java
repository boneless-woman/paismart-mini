package com.yizhaoqi.smartpai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Set;

@Component
@Profile("portfolio-demo")
public class PortfolioReadOnlyFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_POSTS = Set.of(
            "/api/v1/users/login", "/api/v1/users/logout", "/api/v1/auth/refresh"
    );
    private final ObjectMapper objectMapper;

    public PortfolioReadOnlyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("GET".equals(request.getMethod()) && path.equals("/api/v1/users/conversation")) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "code", 200,
                    "message", "演示会话不保存历史记录",
                    "data", List.of()
            ));
            return;
        }
        boolean adminOrWriteArea = path.startsWith("/api/v1/admin/")
                || path.startsWith("/api/v1/upload/")
                || path.startsWith("/api/v1/recharge")
                || path.startsWith("/api/v1/internal/")
                || path.equals("/api/v1/users/register");
        boolean modifyingApi = path.startsWith("/api/")
                && !"GET".equals(request.getMethod())
                && !ALLOWED_POSTS.contains(path);
        if (adminOrWriteArea || modifyingApi) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "code", 403,
                    "message", "DEMO_READ_ONLY",
                    "detail", "作品集演示环境为只读模式"
            ));
            return;
        }
        chain.doFilter(request, response);
    }
}
