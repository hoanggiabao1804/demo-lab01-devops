# Istio manifests

Shared Istio values and helper scripts stay in this folder.

Environment-specific manifests are split into:

- `dev/`
- `staging/`

Apply one environment at a time:

```sh
kubectl apply -k k8s/deploy/istio/dev
kubectl apply -k k8s/deploy/istio/staging
```
