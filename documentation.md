# Sales Project — DevOps Documentation

**Project Title:** Sales Project (Salesdemo)

**Repository root:** `d:\salesdemo`

---

## Introduction

This document describes the Sales Project (Salesdemo) and the CI/CD workflow implemented around it. It is intended for developers, DevOps engineers, and maintainers who need to understand, run, or extend the pipeline. The goals are:

- Provide a clear overview of what the project does and why the CI/CD pipeline exists.
- Explain core DevOps concepts used in this project (CI/CD, Docker, Jenkins, Docker Hub, Kubernetes).
- Give a step-by-step guide to set up, run, and troubleshoot the pipeline using the files in this repository.
- Include code snippets, commands, and placeholders for screenshots so you can reproduce the setup and audit the pipeline.

How to use this document:

- Read the **DevOps Concepts** section if you need background on the technologies.
- Follow **Setup & Installation** to prepare your machine, install Jenkins, Docker Desktop, and configure credentials.
- Use **Running and Verifying the Pipeline** and **Useful Commands** when testing or debugging the CI/CD flow.

This document assumes you have basic familiarity with Git, the command line (PowerShell on Windows), and administrative access to install software.
 
## Methodology

This section describes the practical methodology used to develop, build, test, package, and deploy the Sales Project. The methodology follows common DevOps practices to ensure fast feedback, safe deployments, and reproducible artifacts.

1. Development & Source Control

    - Branching: Use a feature-branch workflow. Developers create branches from `main` (e.g., `feature/add-sales-endpoint`) and open Pull Requests (PRs) for review.
    - Code review: Require at least one reviewer per PR; run automated checks before merging.
    - Commit hygiene: Keep commits atomic and include meaningful messages. Use signed commits if your team requires it.

2. Automated Tests & Quality Gates

    - Local tests: Developers run unit tests locally with the Maven wrapper before pushing: `./mvnw test` (Windows: `.\mvnw.cmd test`).
    - CI tests: Jenkins runs `mvnw -B clean test` on each PR or push to `main`. Failing tests block the pipeline.
    - Optional quality gates: Add static analysis (SpotBugs, PMD) and code coverage checks in the pipeline before merging.

3. Build & Artifact Creation

    - Maven package: After tests pass, the pipeline runs `mvnw -B -DskipTests package` to produce the JAR artifact.
    - Artifact immutability: Use the commit SHA as an immutable identifier for images and artifacts.

4. Containerization & Image Management

    - Multi-stage Docker builds: `Dockerfile` uses a build stage to produce the JAR and a runtime stage to keep the image small.
    - Image tagging: Tag images with both `sha-<shortSHA>` and `latest`. For example: `vivekkashyap043/salesdemo:sha-03e39f7` and `vivekkashyap043/salesdemo:latest`.
    - Registry: Push images to Docker Hub. Consider a private registry for production.
    - Image scanning: Integrate image scanning for CVEs (Trivy/Clair) as part of the pipeline before pushing to production.

5. CI/CD Pipeline (Jenkins)

    - Stages: `Checkout` → `Build & Test` → `Package` → `Docker Build` → `Push to Registry` → `Deploy to Kubernetes`.
    - Credentials: Store Docker credentials and `kubeconfig` in Jenkins Credentials and use `withCredentials` to avoid leaking secrets.
    - Idempotency: Pipeline steps are idempotent—re-running a failed stage should be safe.

6. Deployment Strategy

    - Rolling updates: Use Kubernetes Deployments to perform rolling updates; Jenkins uses `kubectl set image` to change the Deployment image and `kubectl rollout status` to wait for completion.
    - Health checks: Add `livenessProbe` and `readinessProbe` to `deployment.yml` so Kubernetes only routes traffic to healthy pods.
    - Blue/Green or Canary (optional): For higher safety, migrate to Blue/Green or Canary deployments via labels/feature flags or a service mesh.

7. Rollback Procedures

    - Automatic rollback: If `kubectl rollout status` fails, Jenkins should mark the build as failed; manual rollback can be performed with `kubectl rollout undo deployment/salesdemo-deployment -n salesdemo`.
    - Historic images: Keep previous image tags in the registry so rollbacks can reference specific SHAs.

8. Environment Parity

    - Local parity: Use Docker Desktop with Kubernetes enabled to mirror staging environment locally where possible.
    - Configuration: Keep environment-specific configuration outside images (ConfigMaps/Secrets) and inject at runtime.

9. Monitoring, Logging & Alerts

    - Logs: Use centralized logging (e.g., Elasticsearch/Fluentd/Kibana or Grafana Loki) to aggregate pod logs.
    - Metrics & alerts: Expose Prometheus metrics from the app and configure alerts for error rates, latency, and pod restarts.

10. Security & Secrets

    - Secrets: Store credentials in Jenkins Credentials and Kubernetes Secrets; use RBAC to limit access.
    - Least privilege: Limit Docker Hub tokens and kubeconfig permissions to only what the CI needs.
    - Dependency scanning: Run dependency vulnerability scans on build artifacts (e.g., OWASP Dependency-Check).

11. Observability & Post-deploy Verification

    - Smoke tests: After deployment, run quick smoke/integration tests against the service endpoint to verify basic functionality.
    - Synthetic transactions: Use periodic synthetic tests to validate availability.

12. Continuous Improvement

    - Blameless postmortems: For any incidents, conduct postmortems, capture root causes, and add pipeline or test coverage to prevent regressions.
    - Metrics-driven work: Track pipeline duration, failure rates, and mean time to recovery (MTTR) and improve accordingly.

This methodology provides a repeatable, safe, and auditable flow from code change to production deployment; adapt steps (e.g., add staging environments) as your team and risk profile evolve.

## 1. Executive Summary

- **Project:** Sales Project (Salesdemo)
- **Purpose:** Demonstration of a complete CI/CD pipeline using GitHub, Jenkins, Docker, Docker Hub, and Kubernetes. The pipeline builds, tests, packages a Spring Boot application, produces Docker images, pushes them to Docker Hub, and deploys updates to a Kubernetes cluster.
- **Primary artifacts:** `Dockerfile`, `Jenkinsfile`, `k8s/*.yml`, Spring Boot application in `src/`.

[Insert screenshot: Project root structure]

---

## 2. DevOps Concepts (Detailed)

### 2.1 What is Continuous Integration (CI)?

Continuous Integration is a software development practice where developers frequently merge their code changes into a shared repository. Every merge triggers an automated build and test run, so integration issues are found early.

- **Benefits:** faster feedback, fewer integration bugs, consistent builds.
- **In this project:** Jenkins acts as the CI server — it checks out code, runs Maven tests, and prepares artifacts.

### 2.2 What is Continuous Delivery and Continuous Deployment (CD)?

Continuous Delivery is the practice of ensuring that code is always in a deployable state and can be released to production at any time. Continuous Deployment goes further by automatically releasing every change that passes the pipeline.

- **Benefits:** frequent releases, faster time-to-market, lower risk per release.
- **In this project:** Jenkins automates packaging the app into a Docker image and updating the Kubernetes Deployment (continuous deployment behavior).

### 2.3 What is Docker?

Docker is a containerization platform that packages applications with all their dependencies into portable images. Images are immutable snapshots; containers are running instances of images.

- **Image:** built using a `Dockerfile` (contains app, runtime, and dependencies).
- **Registry:** Docker Hub stores images so clusters/hosts can pull them.
- **In this project:** the `Dockerfile` builds a runnable JAR and the image is pushed to Docker Hub by Jenkins.

### 2.4 What is Jenkins?

Jenkins is an automation server used to implement CI/CD pipelines. Pipeline definitions live in a `Jenkinsfile` and can be executed on Jenkins agents.

- **Credentials management:** Jenkins stores secrets (Docker credentials, kubeconfig) in the Credentials Store.
- **Pipeline:** Declarative pipeline used here defines stages for build, test, image, push, and deploy.

### 2.5 What is Kubernetes?

Kubernetes is a container orchestration platform that manages deployment, scaling, networking, and health of containerized applications.

- **Deployment:** manages pod replicas and rolling updates.
- **Service:** exposes pods internally or externally.
- **Namespace:** logical partition for cluster resources.
- **In this project:** `k8s/` contains manifests for namespace, deployment, and service; Jenkins updates the Deployment image for rollouts.

---

## 3. Project Overview

This repository demonstrates a full pipeline: build a Spring Boot app, run unit tests, create Docker images, push images to Docker Hub, and update a Kubernetes Deployment using Jenkins automation.

Key locations in the repo:

- `pom.xml` — Maven build and dependencies
- `mvnw`, `mvnw.cmd`, `.mvn/` — Maven wrapper for reproducible CI builds
- `Dockerfile` — Multi-stage build for compiling and producing a runtime image
- `Jenkinsfile` — Declarative Jenkins pipeline
- `k8s/` — `namespace.yml`, `deployment.yml`, `service.yml`
- `src/main/java/com/salesdemo/` — application source code

---

## 4. Project Flow (High-level)

1. Developer pushes code to GitHub (main branch).
2. GitHub webhook (or Jenkins polling) triggers the Jenkins pipeline.
3. Jenkins checks out the repository.
4. Jenkins runs `mvnw` to execute unit tests, then packages the application.
5. Jenkins builds a Docker image (tagged with commit SHA and `latest`).
6. Jenkins logs into Docker Hub and pushes both tags.
7. Jenkins runs `kubectl set image` against the `salesdemo-deployment` in the `salesdemo` namespace and waits for rollout.
8. Kubernetes pulls the new image and performs a rolling update.

[Insert screenshot: Deployment pipeline stages in Jenkins]

---

## 5. Important Files — Explanation & Key Snippets

### 5.1 `Dockerfile` (multi-stage build)

Purpose: compile the Spring Boot app with Maven and produce a slim runtime image.

Key excerpt:

```dockerfile
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/*.jar
COPY --from=builder /app/${JAR_FILE} /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Notes:

- The Maven wrapper (`mvnw`) avoids requiring a specific Maven installation on CI agents.
- Multi-stage builds keep the final image minimal by excluding build-time dependencies.

### 5.2 `Jenkinsfile` (pipeline overview)

Purpose: define the CI/CD pipeline run by Jenkins.

Key environment and commands (Windows agent in this repo):

```groovy
environment {
  IMAGE = "vivekkashyap043/salesdemo"
  TAG = "${env.GIT_COMMIT.substring(0,7)}"
  K8S_NAMESPACE = "salesdemo"
}

// Build Docker Image stage
bat "docker build -t %IMAGE%:%TAG% ."
bat "docker tag %IMAGE%:%TAG% %IMAGE%:latest"

// Push Image stage
withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
  bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
  bat "docker push %IMAGE%:%TAG%"
  bat "docker push %IMAGE%:latest"
}

// Deploy stage
withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KUBECONFIG')]) {
  bat "kubectl --kubeconfig=%KUBECONFIG% -n %K8S_NAMESPACE% set image deployment/salesdemo-deployment salesdemo=%IMAGE%:%TAG%"
  bat "kubectl --kubeconfig=%KUBECONFIG% -n %K8S_NAMESPACE% rollout status deployment/salesdemo-deployment --timeout=120s"
}
```

Notes:

- `withCredentials` securely injects credentials into the pipeline.
- `TAG` uses a short SHA to create immutable image references.

### 5.3 Kubernetes manifests (concepts)

- `k8s/namespace.yml` — defines the `salesdemo` namespace.
- `k8s/deployment.yml` — defines `salesdemo-deployment` using a container named `salesdemo` (Jenkins will replace the image field).
- `k8s/service.yml` — exposes the deployment (Service type depends on file contents — ClusterIP/NodePort/LoadBalancer).

[Insert screenshot: k8s manifests overview]

### 5.4 Java Application (brief)

Source path: `src/main/java/com/salesdemo`

- `SalesdemoApplication.java` — Spring Boot entry point.
- `controller/SalesController.java` — REST endpoints for sales operations.
- `service/SaleService.java` — business logic.
- `repository/SalesRepo.java` — persistence interface (Spring Data).
- `model/Sales.java` — domain model.

Typical request flow:

1. Client → `SalesController` endpoint
2. Controller → `SaleService` for business rules
3. Service → `SalesRepo` to persist/retrieve data

[Insert screenshot: SalesController code snippet]

---

## 6. Setup & Installation (Detailed)

The steps below expand on the `README.md` and add context-specific notes for this project. They assume a Windows environment (PowerShell) and local Docker Desktop + Kubernetes.

### 6.1 Prerequisites

- Windows with administrative privileges
- Docker Desktop (with Kubernetes enabled)
- Git CLI
- Jenkins server (local or reachable from GitHub)
- kubectl CLI
- ngrok (optional, for exposing local Jenkins to GitHub webhooks)
- Docker Hub account

### 6.2 Docker & Docker Desktop (quick)

1. Install Docker Desktop: https://www.docker.com/get-started
2. Enable Kubernetes in Docker Desktop settings (this creates a single-node cluster).
3. Verify:

```powershell
docker --version
kubectl version --client
kubectl get nodes
```

### 6.3 Jenkins setup (summary and repo-specific details)

1. Install Jenkins (Windows MSI or container). Official guide: https://www.jenkins.io/doc/
2. Open Jenkins at `http://localhost:8090/` (default in README) and complete initial setup.
3. Install required plugins: **Git**, **Pipeline**, **Docker Pipeline** (additional recommended plugins: Credentials Binding, Kubernetes CLI Plugin).

Credentials to create in Jenkins (manage credentials → system → global):

- `dockerhub-creds` — Kind: Username with password. Username is your Docker Hub user; password is an access token (recommended).
- `kubeconfig-file` — Kind: Secret file. Upload the kubeconfig file from your environment (e.g., `C:\Users\<user>\.kube\config`) so Jenkins can run `kubectl`.

Create a Pipeline job (`salesdemo-ci`):

- Definition: Pipeline script from SCM
- SCM: Git
- Repository URL: your GitHub repo (e.g., `https://github.com/Vivekkashyap043/salesdemo.git`)
- Branches to build: `*/main`
- Script Path: `Jenkinsfile`

[Insert screenshot: Jenkins job config]

### 6.4 GitHub webhook & ngrok (if needed)

If GitHub cannot reach your local Jenkins, create an HTTPS tunnel using `ngrok`:

```powershell
ngrok http 8090
```

Copy the HTTPS URL (for example `https://abcd1234.ngrok.io`) and add a webhook in your GitHub repo settings:

- **Payload URL:** `https://<ngrok-url>/github-webhook/`
- **Content Type:** `application/json`
- **Events:** `Push` (or select `Let me select events` and choose `Push`)

Push a commit to test the webhook; Jenkins should receive the event and start the pipeline.

[Insert screenshot: GitHub webhook config]

### 6.5 Docker Hub setup

1. Create a Docker Hub account: https://hub.docker.com/
2. Create a repository (e.g., `vivekkashyap043/salesdemo`).
3. Generate an Access Token (recommended) and use it in Jenkins credentials (`dockerhub-creds`).

### 6.6 Apply Kubernetes resources (first-time)

Run once to prepare namespace and baseline deployment/service:

```powershell
kubectl apply -f k8s/namespace.yml
kubectl apply -n salesdemo -f k8s/deployment.yml
kubectl apply -n salesdemo -f k8s/service.yml
```

Verify resources:

```powershell
kubectl get deployments -n salesdemo
kubectl get pods -n salesdemo
kubectl get svc -n salesdemo
```

Notes:

- Ensure the Deployment's container name is `salesdemo` so `kubectl set image` can target it.

---

## 7. Running and Verifying the Pipeline

1. Commit and push code to the `main` branch.
2. Jenkins receives webhook and runs the pipeline stages defined in `Jenkinsfile`.
3. Watch Jenkins console output for build, test, docker build, push, and deploy steps.
4. Confirm Docker images on Docker Hub (tagged by commit SHA and `latest`).
5. Verify Kubernetes rollout:

```powershell
kubectl get pods -n salesdemo
kubectl rollout status deployment/salesdemo-deployment -n salesdemo
kubectl describe deployment salesdemo-deployment -n salesdemo
```

If failures occur, check Jenkins console logs and `kubectl describe` / `kubectl logs` for pod-level errors.

---

## 8. Useful Commands (copyable)

Maven (local):

```powershell
.\mvnw.cmd -B clean test
.\mvnw.cmd -B -DskipTests package
```

Docker build & push (local):

```powershell
docker build -t <dockeruser>/salesdemo:<tag> .
docker tag <dockeruser>/salesdemo:<tag> <dockeruser>/salesdemo:latest
docker login -u <dockeruser> -p <token>
docker push <dockeruser>/salesdemo:<tag>
docker push <dockeruser>/salesdemo:latest
```

Kubernetes update (manual):

```powershell
kubectl -n salesdemo set image deployment/salesdemo-deployment salesdemo=<dockeruser>/salesdemo:<tag>
kubectl -n salesdemo rollout status deployment/salesdemo-deployment --timeout=120s
```

Apply k8s files (first-time):

```powershell
kubectl apply -f k8s/namespace.yml
kubectl apply -n salesdemo -f k8s/deployment.yml
kubectl apply -n salesdemo -f k8s/service.yml
```

---

## 9. Security & Best Practices

- Use Docker Hub access tokens (not account passwords) for CI systems.
- Store `kubeconfig` and other secrets in Jenkins Credentials (Secret File / Username+Password / Secret Text) and avoid embedding them in code.
- Use immutable image tags (commit SHAs) to promote reproducible, auditable deployments.
- Add `livenessProbe` and `readinessProbe` in Kubernetes manifests to ensure safe rollouts.
- Limit token scopes and rotate credentials regularly.

---

## 10. Troubleshooting Checklist

- **Build/test failures:** run `mvnw` locally to inspect failing tests and stack traces.
- **Docker build fails:** verify `target/*.jar` exists after Maven package step and that `Dockerfile` paths match repository layout.
- **Jenkins push issues:** validate Docker login credentials and network access to Docker Hub.
- **kubectl errors in Jenkins:** confirm uploaded `kubeconfig-file` is valid and the Jenkins agent can reach the Kubernetes API.
- **Image pull errors on cluster:** check image name and tag; ensure Docker Hub rate limits/permissions are not blocking pulls.

---

## 11. Next Steps & Enhancements

- Add integration and end-to-end tests to the pipeline.
- Use CI agents with Docker-in-Docker or Docker socket access for more reliable builds.
- Protect branches and require PR checks before merging to `main`.
- Store Helm charts or use a GitOps approach (ArgoCD/Flux) for declarative deployments.
- Configure imagePullSecrets if Docker Hub images are private.

---

## 12. Placeholders for Screenshots

- [Screenshot: Project root and file list]
- [Screenshot: Jenkins job pipeline view]
- [Screenshot: Jenkins credentials (`dockerhub-creds` and `kubeconfig-file`)]
- [Screenshot: ngrok URL and GitHub webhook setup page]
- [Screenshot: Docker Hub repository page showing pushed tags]
- [Screenshot: `kubectl get pods` showing new rollout]

---

## 13. Results

After implementing the pipeline described in this document, you should be able to demonstrate the following concrete results:

- **Automated CI/CD flow:** Commits to the repository trigger Jenkins to run tests, build artifacts, create Docker images, push to Docker Hub, and update Kubernetes — with minimal manual steps.
- **Reproducible artifacts:** Builds use the Maven wrapper and multi-stage Dockerfile, producing consistent JARs and images tagged by commit SHA for traceability.
- **Faster feedback loop:** Automated tests in CI provide quick feedback to developers, reducing integration defects.
- **Safe deployment:** Kubernetes rolling updates with liveness/readiness probes and `kubectl rollout status` reduce downtime and enable controlled rollouts.
- **Rollback capability:** Historic image tags and `kubectl rollout undo` allow fast recovery from faulty releases.
- **Auditability:** Image tags and Jenkins build logs give an audit trail from commit → build → deploy.

Measure success by tracking pipeline run time, failure rate, deployment success rate, and mean time to recovery (MTTR) after incidents.

---

## 14. Conclusion

This Sales Project repository provides a compact, practical example of a modern CI/CD pipeline using Jenkins, Docker, Docker Hub, and Kubernetes. It demonstrates best practices such as:

- Using the Maven wrapper for reproducible builds.
- Building small runtime images via multi-stage Dockerfiles.
- Storing credentials securely in Jenkins and avoiding secrets in code.
- Tagging images with immutable identifiers (commit SHAs) and keeping `latest` for convenience.
- Automating rolling updates and verifying rollouts with `kubectl`.

The documentation here is intended to be a living artifact: update it when pipeline stages change, when credentials or registry configuration are revised, and when you add monitoring or release strategies (canary/blue-green). If you want, I can:

- Insert screenshots into the placeholders you provide.
- Generate a printable `.docx` with formatted headings and embedded images.
- Add a table of contents and cross-reference links for navigation.

Please let me know which of these you'd like next and provide screenshots (or tell me where to capture them), and I will update the document accordingly.


## Appendix A — README.md (included for quick reference)

Below is the repository `README.md` content that documents Jenkins / Docker Hub / Kubernetes setup steps as provided in the project.

```
````markdown
# Jenkins + Docker Hub + Kubernetes Setup

**Source:** 

This README consolidates the setup steps for configuring Jenkins to build, push Docker images to Docker Hub, and deploy to Kubernetes (Docker Desktop). Save this as `README.md`.

---

## Step 1 — Open Jenkins

**URL:** `http://localhost:8090/`

---

## Step 2 — Add Docker Hub Credentials in Jenkins

**Navigate to:**  
`Manage Jenkins → Credentials → System → Global credentials → Add Credentials`

**Fill the form exactly like this:**

- **Kind:** Username with password  
- **Username:** `vivekkashyap043`  
- **Password:** (your Docker Hub password or access token)  
- **ID:** `dockerhub-creds`  
- **Description:** Docker Hub credentials

Click **Save**.

> These credentials are stored securely inside Jenkins.  
> **Do not hardcode passwords in the Jenkinsfile.**

---

## Step 3 — Add Kubernetes kubeconfig File to Jenkins

1. Open **Jenkins → Manage Jenkins**  
2. Go to **Credentials → System → Global credentials**  
3. Click **Add Credentials**  
4. Fill the form:
   - **Kind:** Secret file  
   - **File:** Upload `C:\Users\Vivek\.kube\config`  
   - **ID:** `kubeconfig-file`  
   - **Description:** Docker Desktop Kubernetes config  
5. Click **Save**

---

## Step 4 — Install Required Plugins (one-time)

Open **Manage Jenkins → Manage Plugins** and ensure these plugins are installed:

- GitHub plugin  
- Pipeline  
- Git plugin  
- GitHub Integration  
- Docker Pipeline (optional but recommended)

If missing, install them and restart Jenkins.

---

## Step 5 — Create a Pipeline Job that uses the Jenkinsfile

Even if your `Jenkinsfile` is in GitHub, create a Jenkins job that points to the repo:

1. Jenkins → **New Item**  
2. Enter name: `salesdemo-ci`  
3. Select **Pipeline**  
4. Click **OK**

In job configuration:

- **Definition:** Pipeline script from SCM  
- **SCM:** Git  
- **Repository URL:** `https://github.com/Vivekkashyap043/salesdemo.git`  
- **Branches to build:** `*/main`  
- **Script Path:** `Jenkinsfile`  
- **Build Triggers:** Check **GitHub hook trigger for GITScm polling**

Click **Save**.

---

## Step 6 — Create GitHub Webhook using ngrok

Run ngrok to expose Jenkins to GitHub:

```bash
ngrok http 8090
```

Copy the generated HTTPS URL (example):  
`https://autogenous-unconvolutely-jinny.ngrok-free.dev`

Set GitHub webhook **Payload URL** to:

```
https://<your-ngrok-url>/github-webhook/
```

(Select `application/json` and push events.)

---

## Step 7 — Create namespace and deploy to Kubernetes (first-time only)

**Apply namespace:**

```bash
kubectl apply -f k8s/namespace.yml
```

**Deploy your app (first time only):**

```bash
kubectl apply -n salesdemo -f k8s/deployment.yml
kubectl apply -n salesdemo -f k8s/service.yml
```

**Verify resources:**

```bash
kubectl get deployments -n salesdemo
kubectl get pods -n salesdemo
kubectl get svc -n salesdemo
```

---

## Notes & Tips

- The Jenkins pipeline (Jenkinsfile) typically performs *updates* on an existing Deployment using:

```bash
kubectl set image deployment/salesdemo-deployment salesdemo=<image>:<tag>
kubectl rollout status deployment/salesdemo-deployment
```

So you must create the namespace/deployment/service **once** manually before relying on Jenkins to update images.

- Use a Docker Hub **access token** instead of your account password for CI systems (add as Jenkins credential).

- When running Jenkins as a Windows Service, configure the service to run under a user account that has access to Docker Desktop and `kubectl` (Services → Jenkins → Log On).

---

## Quick Checklist

- [ ] Jenkins reachable at `http://localhost:8090/`  
- [ ] Docker Desktop running with Kubernetes enabled  
- [ ] `dockerhub-creds` stored in Jenkins (username + token)  
- [ ] `kubeconfig-file` uploaded to Jenkins as Secret File  
- [ ] Jenkins job `salesdemo-ci` configured and pointing to GitHub repo  
- [ ] ngrok tunnel running (for GitHub webhooks)  
- [ ] Kubernetes resources applied at least once (namespace, deployment, service)

---

````

---

If you'd like additional sections (security review, CI metrics, class-level documentation for each Java file, or automated screenshot insertion), tell me which items to add and I will update `documentation.md`.
Sales Project — DevOps Documentation

Project Title: Sales Project (Salesdemo)
Repository root: `d:\salesdemo`

---

1. Executive Summary

- Project: Sales Project (Salesdemo)
- Purpose: Demonstration of a complete CI/CD pipeline using GitHub, Jenkins, Docker, Docker Hub, and Kubernetes. The pipeline builds, tests, packages a Spring Boot app, produces Docker images, pushes to Docker Hub, and deploys updates to a Kubernetes cluster.
- Artifacts: `Dockerfile`, `Jenkinsfile`, `k8s/*.yml`, Java Spring Boot project in `src/`.

[Insert screenshot: Project root structure]

---

2. DevOps Concepts (Overview)

- CI (Continuous Integration): Practice of merging developer changes frequently into a central repository where automated builds and tests run. In this project, Jenkins triggers builds on GitHub changes and runs Maven tests.

- CD (Continuous Delivery/Deployment): Automated steps to deliver application artifacts to environments. Here, Jenkins builds the Docker image, pushes it to Docker Hub, and updates a Kubernetes deployment (image rollouts) — enabling continuous deployment.

- Docker: Containerization platform that packages the application and its dependencies into images that run the same across environments.

- Docker Hub: A public container registry used to host and share Docker images. Jenkins pushes built images to Docker Hub.

- Jenkins: An automation server used to define the CI/CD pipeline (pipeline is in `Jenkinsfile`). Jenkins executes build/test/image/push/deploy stages.

- Kubernetes: Container orchestration platform used here to run the application on a cluster. Resources are defined in `k8s/deployment.yml`, `k8s/service.yml`, and `k8s/namespace.yml`.

---

3. Project Structure and Flow

Repository layout (important files):

- `pom.xml` — Maven project file (build & dependencies)
- `mvnw`, `mvnw.cmd`, `.mvn/` — Maven wrapper for reproducible builds
- `Dockerfile` — Builds the app (multi-stage: build then runtime image)
- `Jenkinsfile` — Jenkins Pipeline: checkout → test → package → docker build → push → kubectl set image + rollout
- `k8s/namespace.yml` — Namespace manifest
- `k8s/deployment.yml` — Kubernetes Deployment (app container image placeholder)
- `k8s/service.yml` — Kubernetes Service for exposing the app
- `src/main/java/com/salesdemo/` — Spring Boot application sources

Flow (high level):

1. Developer pushes code to GitHub (main branch).
2. GitHub webhook notifies Jenkins (via ngrok or direct network exposure).
3. Jenkins `Checkout` stage clones repo.
4. `Build & Test` stage runs `mvnw -B clean test` and `mvnw -B -DskipTests package`.
5. If the build succeeds, Jenkins builds a Docker image (tagged by commit hash and `latest`).
6. Jenkins logs into Docker Hub and pushes the image tag and `latest`.
7. Jenkins updates the Kubernetes Deployment (`kubectl set image ...`) using a provided kubeconfig secret, waits for rollout completion.
8. Kubernetes pulls the image from Docker Hub and rolls out the new pods.

[Insert screenshot: Deployment pipeline stages in Jenkins]

---

4. Important Files (explanations & snippets)

4.1 Dockerfile (explained)

The provided `Dockerfile` is multi-stage:

- Stage 1 (builder): Uses JDK image, copies the Maven wrapper and sources, runs `./mvnw -B -DskipTests package` to produce the jar.
- Stage 2: Uses a JRE base image and copies the built jar from the builder stage; exposes port `8081`.

Key parts (from `Dockerfile`):

```
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/*.jar
COPY --from=builder /app/${JAR_FILE} /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Notes:
- Using the Maven wrapper (`mvnw`) ensures consistent Maven version without requiring global Maven install on CI agent.
- Multi-stage build keeps the final image small by not carrying build tools into runtime image.

4.2 Jenkinsfile (explained)

The pipeline stages:

- `Checkout` — clone repository.
- `Build & Test (Windows)` — runs `mvnw.cmd` to test and package.
- `Build Docker Image` — `docker build` and `docker tag`.
- `Push Image to Docker Hub` — uses Jenkins credentials (`dockerhub-creds`) to login and push images.
- `Deploy to Kubernetes` — uses `kubeconfig-file` credential to run `kubectl set image ...` and `kubectl rollout status ...`.

Key snippet (from `Jenkinsfile`):

```
environment {
    IMAGE = "vivekkashyap043/salesdemo"
    TAG = "${env.GIT_COMMIT.substring(0,7)}"
    K8S_NAMESPACE = "salesdemo"
}

stage('Build Docker Image') {
    steps {
        bat "docker build -t %IMAGE%:%TAG% ."
        bat "docker tag %IMAGE%:%TAG% %IMAGE%:latest"
    }
}

stage('Push Image to Docker Hub') {
    steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
            bat "docker push %IMAGE%:%TAG%"
            bat "docker push %IMAGE%:latest"
        }
    }
}

stage('Deploy to Kubernetes') {
    steps {
        withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KUBECONFIG')]) {
            bat """
            kubectl --kubeconfig=%KUBECONFIG% -n %K8S_NAMESPACE% \
            set image deployment/salesdemo-deployment salesdemo=%IMAGE%:%TAG%
            """

            bat """
            kubectl --kubeconfig=%KUBECONFIG% -n %K8S_NAMESPACE% \
            rollout status deployment/salesdemo-deployment --timeout=120s
            """
        }
    }
}
```

Notes:
- `withCredentials` prevents secrets from being stored in the pipeline code.
- `TAG` is derived from the commit hash so each image is uniquely addressable.
- `kubectl` commands use the uploaded kubeconfig file from Jenkins credentials.

4.3 Kubernetes manifests (explained)

- `k8s/namespace.yml` — creates the `salesdemo` namespace.
- `k8s/deployment.yml` — defines a Deployment named `salesdemo-deployment` using container named `salesdemo` (image will be replaced by Jenkins `kubectl set image ...`).
- `k8s/service.yml` — exposes the deployment (e.g., ClusterIP or NodePort depending on file content).

[Insert screenshot: k8s manifests overview]

4.4 Java project (brief overview)

Main sources: `src/main/java/com/salesdemo`

- `SalesdemoApplication.java` — Spring Boot main application class.
- `controller/SalesController.java` — REST controller exposing endpoints for sales operations.
- `model/Sales.java` — domain model.
- `repository/SalesRepo.java` — Spring Data repository interface.
- `service/SaleService.java` — business logic / service layer.

Example (high-level) request flow:

1. Client → `SalesController` endpoint (e.g., POST /sales)
2. Controller validates and forwards to `SaleService`.
3. `SaleService` uses `SalesRepo` to persist/retrieve data from configured datasource (in `application.properties`).

[Insert screenshot: SalesController code snippet]

---

5. Setup & Installation (Step-by-step)

The following steps assume Windows environment (PowerShell), Docker Desktop with Kubernetes enabled, Jenkins running on the host (or separate VM), and a Docker Hub account.

Pre-requisites:
- Windows system with administrative privileges
- Docker Desktop (with Kubernetes enabled)
- Git
- Jenkins (installed or running locally; port in README is `8090`)
- kubectl CLI
- ngrok (for webhook tunneling when Jenkins is behind NAT)
- Docker Hub account

5.1 Docker & Docker Desktop

- Install Docker Desktop for Windows: https://www.docker.com/get-started
- Enable Kubernetes in Docker Desktop settings (this will create a single-node k8s cluster).
- Verify Docker is running:

```powershell
docker --version
```

- Verify Kubernetes (kubectl):

```powershell
kubectl version --client
kubectl config current-context
kubectl get nodes
```

5.2 Jenkins setup (minimal)

- Install Jenkins (Windows service or container). Official guide: https://www.jenkins.io/doc/
- Open Jenkins at `http://localhost:8090/` (as in README).
- Install required plugins: Git, Pipeline, Docker Pipeline.

Credentials to create in Jenkins:

- `dockerhub-creds` — Kind: Username with password. Username is your Docker Hub username; password should be your Docker Hub token.
- `kubeconfig-file` — Kind: Secret file. Upload the local kubeconfig (usually `C:\Users\<user>\.kube\config`) so Jenkins can talk to Kubernetes.

Create pipeline job `salesdemo-ci`:
- Definition: Pipeline script from SCM
- SCM: Git
- Repository: `https://github.com/<your-repo>.git` (or your GitHub URL)
- Branches to build: `*/main`
- Script Path: `Jenkinsfile`

[Insert screenshot: Jenkins job config]

5.3 GitHub webhook & ngrok (for local Jenkins)

If Jenkins is not publicly accessible, use `ngrok` to expose it.

Install ngrok (https://ngrok.com/) and run:

```powershell
ngrok http 8090
```

Copy the generated `https://...` URL and create a GitHub webhook in your repository settings:

- Payload URL: `https://<ngrok-url>/github-webhook/`
- Content type: `application/json`
- Events: `Push` (or select `Let me select individual events` and choose `Push`)

Test by pushing to GitHub — Jenkins should receive the webhook.

[Insert screenshot: GitHub webhook config]

5.4 Docker Hub setup

- Create an account on Docker Hub: https://hub.docker.com/
- Create a repository `vivekkashyap043/salesdemo` (or use your username/repo naming pattern).
- Create an Access Token (recommended) and use it in Jenkins `dockerhub-creds` instead of your password.

5.5 Kubernetes resources apply (first-time only)

Run these once to provision namespace and base deployment and service:

```powershell
kubectl apply -f k8s/namespace.yml
kubectl apply -n salesdemo -f k8s/deployment.yml
kubectl apply -n salesdemo -f k8s/service.yml
```

Verify:

```powershell
kubectl get deployments -n salesdemo
kubectl get pods -n salesdemo
kubectl get svc -n salesdemo
```

Notes:
- The deployment created by these files should have the container name `salesdemo` so Jenkins `kubectl set image` can find and replace it.

---

6. Running the Pipeline — manual steps and expected output

1. Commit & push code changes to GitHub (main branch).
2. Webhook triggers Jenkins job or Jenkins polls GitHub.
3. Jenkins runs pipeline stages (see `Jenkinsfile`).
4. On success: new Docker image tags pushed to Docker Hub, and Kubernetes deployment updated to use the new image.
5. Verify the rolling update:

```powershell
kubectl get pods -n salesdemo
kubectl rollout status deployment/salesdemo-deployment -n salesdemo
kubectl describe deployment salesdemo-deployment -n salesdemo
```

If something fails, check Jenkins console logs and the `docker build` and `kubectl` outputs.

---

7. Useful Commands (copyable)

Maven (run locally):

```powershell
.\mvnw.cmd -B clean test
.\mvnw.cmd -B -DskipTests package
```

Docker build & push (locally):

```powershell
docker build -t <dockeruser>/salesdemo:<tag> .
docker tag <dockeruser>/salesdemo:<tag> <dockeruser>/salesdemo:latest
docker login -u <dockeruser> -p <token>
docker push <dockeruser>/salesdemo:<tag>
docker push <dockeruser>/salesdemo:latest
```

Kubernetes update (from Jenkins or local shell):

```powershell
kubectl -n salesdemo set image deployment/salesdemo-deployment salesdemo=<dockeruser>/salesdemo:<tag>
kubectl -n salesdemo rollout status deployment/salesdemo-deployment --timeout=120s
```

Apply k8s files:

```powershell
kubectl apply -f k8s/namespace.yml
kubectl apply -n salesdemo -f k8s/deployment.yml
kubectl apply -n salesdemo -f k8s/service.yml
```

---

8. Security & Best Practices

- Use Docker Hub access tokens (not passwords) for CI systems.
- Keep `kubeconfig` in Jenkins credentials (Secret File) and avoid embedding it in pipeline code.
- Limit the scope of tokens/credentials and rotate regularly.
- Use immutable image tags (commit SHA) to avoid ambiguous deployments.
- Add health checks and readinessProbes in the Kubernetes `deployment.yml` for safer rollouts.

---

9. Troubleshooting Checklist

- Build fails: run tests locally with `mvnw` and inspect failing tests.
- Docker build fails: check `Dockerfile` paths and that `./mvnw` produced `target/*.jar`.
- Jenkins cannot push to Docker Hub: validate `docker login` credentials and network access.
- Jenkins cannot run kubectl: ensure `kubeconfig-file` is correct and kubeconfig references a reachable cluster.
- Kubernetes image pull errors: check image name, tags, and whether the cluster can access Docker Hub (public images usually fine).

---

10. Next Steps & Optional Enhancements

- Add automated integration tests to the pipeline.
- Add GitHub Actions or another CI as a parallel pipeline.
- Secure Docker Hub repository (private) and configure Kubernetes imagePullSecrets.
- Add Canary deployments or use a helm chart for more flexible deployments.

---

11. Placeholders for Screenshots (please add images into the docx where indicated)

- [Screenshot: Project root and file list]
- [Screenshot: Jenkins job pipeline view]
- [Screenshot: Jenkins credentials pages for dockerhub-creds and kubeconfig-file]
- [Screenshot: ngrok URL and GitHub webhook setup page]
- [Screenshot: Docker Hub repository page showing pushed tags]
- [Screenshot: kubectl get pods showing new rollout]

---

Appendix A — Key files (for reference)

- `Dockerfile` (root) — multi-stage build (see above)
- `Jenkinsfile` (root) — pipeline that builds, pushes, deploys (see above)
- `k8s/` — `namespace.yml`, `deployment.yml`, `service.yml`
- Source code: `src/main/java/com/salesdemo` (application, controllers, models, repos, services)

---

If you want, I can:
- Convert this Markdown into a fully formatted Word `.docx` (with headings, bold, and image placeholders) using `python-docx` and commit it here — tell me if you want me to run that conversion now.
- Insert screenshots if you provide them.

Please review the document and tell me any additions or formatting preferences.
