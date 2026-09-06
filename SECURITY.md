# Security Practices

Never commit passwords, API tokens, kubeconfigs, private keys, registry credentials, or cloud service-principal secrets.

Use GitHub Actions Secrets for sensitive values and prefer OIDC/workload identity for cloud authentication.

Container controls:
- Multi-stage build
- Maven/JDK excluded from runtime image
- Tomcat runtime
- Non-root user
- Default Tomcat applications removed
- Health check
- Immutable commit-based image tags

Kubernetes controls:
- Service-account token automount disabled
- Non-root execution
- Privilege escalation disabled
- All Linux capabilities dropped
- RuntimeDefault seccomp
- Resource requests/limits
- Readiness/liveness probes
- NetworkPolicy
- Restricted Pod Security labels

Recommended production additions:
- SBOM
- Cosign image signing
- Admission-time signature verification
- Dependabot/dependency review
- Action SHA pinning
- External Secrets/Key Vault/Secrets Manager
- TLS ingress
- Audit logs and centralized observability
