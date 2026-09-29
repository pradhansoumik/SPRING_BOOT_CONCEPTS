# Spring Security (Boot wiring)

**Scope:** OAuth2 / OpenID Connect, JWT authentication, role-based vs. attribute-based access control, and the Spring Boot 3 security DSL.

**Reference notes:** `https://github.com/pradhansoumik/MICROSERVICE-DESIGN-PATTERNS/tree/main/00-fundamentals/security`

**Runnable reference:** `00-fundamentals/security/security-sso-demo/`

---

## 1. OAuth2 / OpenID Connect flow

- **OAuth2** provides delegated authorization using tokens.
- **OpenID Connect (OIDC)** adds identity and login on top of OAuth2.
- In the SSO flow, the browser is redirected to Keycloak, then the application receives tokens after login.
- The OAuth2 client handles login; APIs act as resource servers and validate access tokens.

```text
Browser → Portal (OAuth2 Client) → Keycloak login
Browser ← Portal redirect ← Authorization code
Portal → Order API: Bearer access token
Order API: validate token, then authorize request
```

## 2. JWT authentication

A JWT is a token format, commonly used for the access token sent to an API.

```text
Authorization: Bearer <access_token>
```

The resource server validates the token (signature, issuer, and expiry) before authorization. The access token is for API calls; a refresh token is used by the client to obtain a new access token.

## 3. Role-based vs. attribute-based authorization

Authentication establishes who the caller is; authorization decides what the caller may do.

- **Role-based access control (RBAC):** allow access based on roles such as `USER` or `ADMIN`.
- **Attribute-based access control (ABAC):** allow access based on attributes of the caller or resource, such as whether the caller owns the requested order.

## 4. Spring Boot 3 declarative security DSL

Spring Security configuration is declared using a `SecurityFilterChain` bean rather than extending `WebSecurityConfigurerAdapter`.

**OAuth2 client (portal/BFF):**

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/css/**").permitAll()
            .anyRequest().authenticated())
        .oauth2Login(Customizer.withDefaults());
    return http.build();
}
```

**JWT resource server (API):**

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
}
```

The existing demo also maps Keycloak realm roles to Spring authorities. See its `SecurityConfig.java` files for the complete configuration.

---

## Quick recall

- OAuth2 = delegated authorization; OIDC = identity/login on OAuth2.
- OAuth2 client handles login; resource server validates API access tokens.
- JWT authentication validates the token; authorization checks roles or resource attributes.
- Spring Boot 3 uses a `SecurityFilterChain` bean for declarative HTTP security rules.
