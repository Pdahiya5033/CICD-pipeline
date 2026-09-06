Secure CI/CD — Spring Boot WAR + Tomcat

End-to-end GitHub-ready CI/CD project for building a Spring Boot application packaged as a WAR, creating a multi-stage Docker image with Tomcat, scanning it, and deploying it to Kubernetes running locally on your laptop.

The current setup is designed for:

Docker / Docker Desktop installed on your laptop

Local Kubernetes cluster, such as Docker Desktop Kubernetes

GitHub Actions self-hosted runner installed on the same laptop

Local Docker image: springboot_sampleimage:latest

No Docker Hub/ACR/ECR/GCR registry required

CI/CD Pipeline

GitHub
   |
   v
Self-hosted GitHub Actions Runner
   |
   +--> Maven Build + Unit Tests
   |
   +--> SonarQube
   |
   +--> Semgrep SAST
   |
   +--> OWASP Dependency-Check
   |
   v
Docker Multi-stage Build
   |
   +--> Maven Build Stage
   |
   +--> Tomcat Runtime Stage
   |
   v
springboot_sampleimage:latest
   |
   +--> Trivy Image Scan
   |
   v
Local Kubernetes
   |
   +--> Rolling Deployment
   +--> Readiness/Liveness Probes
   +--> Security Context
   +--> NetworkPolicy
   |
   v
OWASP ZAP DAST

Project Structure

.
├── .github/
│   ├── workflows/
│   │   └── cicd.yml
│   └── dependabot.yml
│
├── app/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/cicddemo/
│       │   ├── CicdDemoApplication.java
│       │   └── HelloController.java
│       └── test/java/com/example/cicddemo/
│           └── HelloControllerTest.java
│
├── docker/
│   └── Dockerfile
│
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── networkpolicy.yaml
│
├── sonar-project.properties
├── SECURITY.md
├── .gitignore
├── .dockerignore
└── README.md

.github is a hidden directory on Linux. If you don't see it in the file manager, press Ctrl + H.

Application

The application is a Spring Boot application packaged as a WAR:

secure-cicd-demo.war

The WAR is deployed into Tomcat as:

/usr/local/tomcat/webapps/secure-cicd-demo.war

The application endpoints are:

GET /secure-cicd-demo/api/health
GET /secure-cicd-demo/api/hello

Local Build

From the repository root:

cd app
mvn clean verify

The generated WAR will be:

app/target/secure-cicd-demo.war

Multi-stage Docker Build

The Dockerfile uses two stages.

Stage 1 — Build

Maven + Java 21
       |
       v
Spring Boot source
       |
       v
secure-cicd-demo.war

Stage 2 — Runtime

Tomcat + Java 21
       |
       +--> secure-cicd-demo.war
       |
       v
Tomcat application

Build it from the repository root:

docker build   -f docker/Dockerfile   -t springboot_sampleimage:latest .

Verify:

docker images springboot_sampleimage

Run locally:

docker run --rm -p 8080:8080 springboot_sampleimage:latest

Test:

curl http://localhost:8080/secure-cicd-demo/api/health

Expected response:

{"status":"UP"}

GitHub Actions — Local Docker Setup

The workflow is:

.github/workflows/cicd.yml

The workflow uses:

runs-on: self-hosted

This is important because the Docker image must be created in the Docker engine on your laptop.

The pipeline builds:

springboot_sampleimage:latest

There is no registry push in this local setup.

Why a self-hosted runner is required

A normal GitHub-hosted runner runs on a temporary GitHub machine:

GitHub-hosted runner
       |
       X
       |
Your laptop Docker

Therefore, an image built by ubuntu-latest would not automatically be available to your laptop's Kubernetes cluster.

With a self-hosted runner:

GitHub
   |
   v
Self-hosted runner on laptop
   |
   +--> Docker Engine
   |       |
   |       +--> springboot_sampleimage:latest
   |
   +--> kubectl
           |
           +--> Local Kubernetes

GitHub Variables

For the local setup, configure these repository variables under:

GitHub
→ Settings
→ Secrets and variables
→ Actions
→ Variables

IMAGE_NAME

Recommended value:

springboot_sampleimage:latest

The workflow already has this default:

IMAGE_NAME: ${{ vars.IMAGE_NAME || 'springboot_sampleimage:latest' }}

So you can also leave the variable unset and use the default.

K8S_NAMESPACE

Recommended value:

secure-cicd

The workflow defaults to:

K8S_NAMESPACE: ${{ vars.K8S_NAMESPACE || 'secure-cicd' }}

DAST_TARGET_URL

This depends on how you expose the local Kubernetes service.

For example, if the application is reachable from the runner at:

http://localhost:8080/secure-cicd-demo/

set:

DAST_TARGET_URL=http://localhost:8080/secure-cicd-demo/

Do not point DAST at systems you do not own or have permission to test.

GitHub Secrets

Configure these under:

GitHub
→ Settings
→ Secrets and variables
→ Actions
→ Secrets

SonarQube

SONAR_TOKEN
SONAR_HOST_URL

Semgrep

If using Semgrep Cloud:

SEMGREP_APP_TOKEN

The local Docker/Kubernetes workflow does not require:

REGISTRY_HOST
REGISTRY_USERNAME
REGISTRY_PASSWORD

because the image is not pushed to a registry.

SonarQube

The SonarQube configuration is in:

sonar-project.properties

The pipeline performs SonarQube analysis after the Maven build and tests.

SAST

The pipeline performs static security analysis using:

Semgrep

This analyzes the source code before the Docker image is built.

Dependency Security

OWASP Dependency-Check scans project dependencies for known vulnerabilities.

A report is uploaded as a GitHub Actions artifact:

dependency-check-report

Container Security

After the Docker image is built, Trivy scans:

springboot_sampleimage:latest

The pipeline is configured to fail for:

CRITICAL
HIGH

vulnerabilities where the scanner considers them applicable/fixable according to the configured options.

Kubernetes Deployment

The Kubernetes manifests are in:

k8s/

Apply manually:

kubectl apply -f k8s/

Check:

kubectl -n secure-cicd get pods
kubectl -n secure-cicd get svc

Check rollout:

kubectl -n secure-cicd rollout status deployment/secure-cicd-demo

Local Kubernetes Access

Because the Service is configured as:

type: ClusterIP

you can access it locally using port-forwarding:

kubectl -n secure-cicd port-forward svc/secure-cicd-demo 8080:80

Then:

curl http://localhost:8080/secure-cicd-demo/api/health

Expected:

{"status":"UP"}

Kubernetes Security

The deployment includes:

runAsNonRoot

runAsUser

allowPrivilegeEscalation: false

Linux capabilities dropped

seccompProfile: RuntimeDefault

service-account token automount disabled

resource requests and limits

readiness probe

liveness probe

rolling update strategy

NetworkPolicy

restricted Pod Security labels

Credentials and Secrets

No passwords, tokens, kubeconfigs, private keys, or registry credentials should be committed to Git.

Use:

GitHub Secrets

for sensitive CI/CD credentials.

For cloud environments, prefer:

GitHub OIDC
      |
      v
Cloud workload identity

instead of storing long-lived cloud access keys.

For production Kubernetes applications, prefer an external secret manager such as Azure Key Vault, AWS Secrets Manager, or Google Secret Manager.

DAST

OWASP ZAP runs after deployment.

For the local setup, the target must be reachable from the self-hosted runner.

Example:

http://localhost:8080/secure-cicd-demo/

The DAST stage should run only against your own application/environment.

End-to-End Execution

Once the self-hosted runner and local Kubernetes are configured:

1. git push
       |
       v
2. GitHub Actions starts
       |
       v
3. Maven build
       |
       v
4. Unit tests
       |
       v
5. SonarQube
       |
       v
6. Semgrep SAST
       |
       v
7. OWASP Dependency-Check
       |
       v
8. Docker multi-stage build
       |
       v
9. springboot_sampleimage:latest
       |
       v
10. Trivy scan
       |
       v
11. kubectl deploy
       |
       v
12. Kubernetes rollout
       |
       v
13. OWASP ZAP DAST

Production Hardening

This project is designed as a portfolio/learning implementation. For production, additionally:

Pin GitHub Actions to immutable commit SHAs.

Pin Docker base images by digest.

Generate an SBOM.

Sign images using Cosign.

Verify image signatures at Kubernetes admission.

Use a private registry for shared/production environments.

Use an external secret manager.

Use GitHub OIDC for cloud authentication.

Protect the main branch.

Require pull-request reviews.

Add staging and production environments.

Add production approval gates.

Add centralized logging, metrics and tracing.

Enable Kubernetes audit logging.

Add automated rollback and deployment health checks.
