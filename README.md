# Swagger Kubernetes

## 一、简介

Swagger Kubernetes 是能够将 Kubernetes 环境下 Spring 项目的 Swagger 文档聚合，只要 Spring 项目中引用了 Swagger 工具暴露 Swagger API，就可以将其所有的这类项目 Swagger 接口聚合到 Swagger Kubernetes 项目当中。

Swagger Kubernetes 是拥有在 Kubernetes 环境中服务发现功能，能够自动服务发现那些暴露 Swagger API 的服务，然后生成 Markdown 格式的文档展示在页面上，还是通过反向代理可以直接调用对应服务接口进行调试工作。

![](http://ww1.sinaimg.cn/large/007vhU0ely1g3qeczucrij30qe0k174p.jpg)

## 二、如何将 Swagger Kubernetes 部署到 Kubernetes

### 1、创建 ServiceAccount

**swagger-kubernetes-ac.yaml**

> 请提前修改里面的 Namespace

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: swagger-kubernetes
  namespace: mydlqcloud
---
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: swagger-kubernetes
rules:
- apiGroups: [""]
  resources: ["services","endpoints"]
  verbs: ["get", "watch", "list"]
---
kind: ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: swagger-kubernetes
  namespace: mydlqcloud
subjects:
- kind: ServiceAccount
  name: swagger-kubernetes
  namespace: mydlqcloud
roleRef:
  kind: ClusterRole
  name: swagger-kubernetes
  apiGroup: rbac.authorization.k8s.io
```

**创建 ServiceAccount**

```bash
$ kubectl apply -f swagger-kubernetes-ac.yaml
```

### 2、创建 Swagger kubernetes 服务

**swagger-kubernetes-deploy.yaml**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: swagger-kubernetes
  labels:
    app: swagger-kubernetes
spec:
  ports:
  - name: tcp
    port: 8080
    nodePort: 32255
    targetPort: 8080
  type: NodePort
  selector:
    app: swagger-kubernetes
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: swagger-kubernetes
  labels:
    app: swagger-kubernetes
spec:
  selector:
    matchLabels:
      app: swagger-kubernetes
  template:
    metadata:
      labels:
        app: swagger-kubernetes
    spec:
      serviceAccountName: swagger-kubernetes    #这里引用创建的服务账户，否则可能没有读取服务所在 Namespace 的权限
      containers:
      - name: swagger-kubernetes
        image: mydlqclub/swagger-kubernetes:1.0.0
        ports:
        - containerPort: 8080
```

**创建 ServiceAccount**

-n：指定启动的 namespace，执行前请先修改此值

```bash
$ kubectl apply -f swagger-kubernetes-deploy.yaml -n mydlqcloud
```

### 3、查看创建的资源

```bash
$ kubectl get pod,service -n mydlqcloud | grep swagger-kubernetes

pod/swagger-kubernetes-5577dc9d8d-6sz4f       1/1     Running   0     
service/swagger-kubernetes        NodePort   10.10.204.142   <none>        8080:32255/TCP  
```

### 4、访问 Swagger Kubernetes

输入地址： http://Kuberntes集群地址:32255 访问 Swagger Kubernetes

![](http://ww1.sinaimg.cn/large/007vhU0ely1g47ly6vu4uj30v60clgm4.jpg)

## 三、可配置环境变量参数

变量名 | 默认值 |描述
---|----|---
KUBERNETES_CONNECT_URL | https://kubernetes.default.svc.cluster.local |Kubernetes API 地址
KUBERNETES_CONNECT_TOKEN | Service Pod ServiceAccount 所拥有的权限 | KUBERNETES_CONNECT_TOKEN
DISCOVERY_NAMESPACE | Service 所在的 Namespace | Swagger 聚合文档的 Kubernetes Namespace
DISCOVERY_INITIAL_INTERVAL | 60 | 服务发现的更新间隔，推荐60秒
SWGGER_API_PATH | /v2/api-docs | 应用 Swagger API 地址
IGNORE_SERVICES | - | 默认的忽略列表，例如设置为"service1,service2,......"
ACTUATOR_PORT | 8080 | SpringBoot management 端口设置 
ACTUATOR_TYPE | * | SpringBoot Actuator 暴露的参数，可以设置为 health,info,env,metrics,prometheus....

