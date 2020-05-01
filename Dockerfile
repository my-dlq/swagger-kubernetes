FROM openjdk:8u212-b04-jre-slim
VOLUME /tmp
ADD target/*.jar swagger-kubernetes.jar
RUN sh -c 'touch /swagger-kubernetes.jar'
ENV JVM_OPTS="-Xss256k -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_OPTS=""
ENV APP_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS  $JVM_OPTS  -jar /app.jar $APP_OPTS" ]