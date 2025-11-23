# Jenkins + Docker Hub + Kubernetes Setup

**Source:** fileciteturn0file0

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
