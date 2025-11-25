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
