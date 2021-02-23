<!-- ![](http://ww1.sinaimg.cn/large/007vhU0ely1g47xglqna6j30qy09rtb5.jpg) -->
![](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/swagger-kubernetes-1001.jpg)

# Swagger Kubernetes

## 一、简介

Swagger Kubernetes 是能够将 Kubernetes 环境下 Spring 项目的 Swagger 文档聚合，只要 Spring 项目中引用了 Swagger 工具暴露 Swagger API，就可以将其所有的这类项目 Swagger 接口聚合到 Swagger Kubernetes 项目当中。

Swagger Kubernetes 是拥有在 Kubernetes 环境中服务发现功能，能够自动服务发现那些暴露 Swagger API 的服务，然后生成 Markdown 格式的文档展示在页面上，通过反向代理可以直接调用对应服务接口进行调试工作。

由于方便，已经将该项目以 Docker 镜像的方式存放到 Docker Hub 仓库。

- hub地址：https://hub.docker.com/r/mydlqclub/swagger-kubernetes
- Docker镜像： mydlqclub/swagger-kubernetes

<!-- ![](http://ww1.sinaimg.cn/large/007vhU0ely1g3qeczucrij30qe0k174p.jpg) -->
![](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/swagger-kubernetes-1002.jpg?x-oss-process=style/shuiyin)

## 二、架构图

<!-- ![](http://ww1.sinaimg.cn/large/007vhU0ely1g49t2mpc6tj30rs0bugmi.jpg) -->
![](https://mydlq-club.oss-cn-beijing.aliyuncs.com/images/swagger-kubernetes-1003.jpg?x-oss-process=style/shuiyin)

## 三、注意事项

注意：在swagger2配置文件中，请不要配置归属组名“groupName”参数，否则将无法将其加入聚合列表

## 四、如何使用

Swagger Kubernetes 是应用在 Kubernetes 环境下，监控服务所在 Namespace 的各个 Spring 应用 Swagger API 接口，所以需要将此应用部署到 Kubernetes 环境下。

下面将演示如何在 Kubernetes 集群部署 Swagger Kubernetes。

### 1、创建 ServiceAccount

**swagger-kubernetes-ac.yaml**

> 请提前修改里面的全部 Namespace 的值为你自己的 Namespace 名称

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: swagger-kubernetes
  namespace: mydlqcloud
---
kind: Role
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
  name: swagger-kubernetes-role
  namespace: mydlqcloud
rules:
  - apiGroups: [""]
    resources: ["services","endpoints"]
    verbs: ["get","list","watch"]
---
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: rbac-role-binding
  namespace: mydlqcloud
subjects:
  - kind: ServiceAccount
    name: swagger-kubernetes
    namespace: mydlqcloud
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: swagger-kubernetes-role
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
  namespace: mydlqcloud
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
  namespace: mydlqcloud
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
      serviceAccountName: swagger-kubernetes
      containers:
        - name: swagger-kubernetes
          image: mydlqclub/swagger-kubernetes:latest
          #国内使用 aliyun 镜像仓库
          #image: registry.cn-beijing.aliyuncs.com/mydlq/swagger-kubernetes:latest
          imagePullPolicy: IfNotPresent
          ports:
            - name: server
              containerPort: 8080
          resources:
            limits:
              cpu: 2000m
              memory: 512Mi
            requests:
              cpu: 500m
              memory: 512Mi
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

## 五、可配置环境变量参数

一般情况用默认配置即可，有些特殊情况需要自定义设置，可以做如下配置：

变量名 | 默认值 |描述
---|----|---
KUBERNETES_CONNECT_URL | https://kubernetes.default.svc.cluster.local |Kubernetes API 地址
KUBERNETES_CONNECT_TOKEN | 应用 Pod 中设置的 ServiceAccount 关联的 Token| 连接 Kubernetes API-Server 的 Token，应用会根据此 Token 而拥有不同的权限
DISCOVERY_NAMESPACE | Service 所在的 Namespace | Swagger 聚合文档的 Kubernetes Namespace
DISCOVERY_PORT_TYPE | ClusterIP | Swagger-kubernetes 监控应用 Service 端口类型，支持 ClusterIP 和 NodePort
DISCOVERY_INITIAL_INTERVAL | 60 | 服务发现的更新间隔，推荐60秒
SWGGER_API_PATH | /v2/api-docs | 应用 Swagger API 地址
IGNORE_SERVICES | - | 默认的忽略列表，例如设置为"service1,service2,......"
ACTUATOR_PORT | 8080 | SpringBoot management 端口设置 
ACTUATOR_TYPE | * | SpringBoot Actuator 暴露的参数，可以设置为 health,info,env,metrics,prometheus....


