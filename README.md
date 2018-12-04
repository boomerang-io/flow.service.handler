# Boomerang Flow Controller Service

This service handles and translates the requests that go to Kubernetes.

It uses the Kubernetes Java Client to interact with Kubernetes.

When writing new controller integrations, it is recommended to look through the Docs to find the exact Client method to use and then look at the API code to see how it works for advance configurations such as the Watcher API.

## References

- Kubernetes Java Client: https://github.com/kubernetes-client/java
- Kubernetes Java Client Examples: https://github.com/kubernetes-client/java/blob/master/examples/src/main/java/io/kubernetes/client/examples
- Kubernetes Java Client API: https://github.com/kubernetes-client/java/tree/master/kubernetes/src/main/java/io/kubernetes/client/apis
- Kubernetes Java Client Docs: https://github.com/kubernetes-client/java/tree/master/kubernetes/docs
