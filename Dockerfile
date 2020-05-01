FROM openjdk:11.0.5-jre-slim
VOLUME /tmp
ADD target/*.jar swagger-kubernetes.jar
RUN sh -c 'touch /swagger-kubernetes.jar'
ENV JVM_OPTS="-Xss256k -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_OPTS=""
ENV APP_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JVM_OPTS $JAVA_OPTS -jar /swagger-kubernetes.jar $APP_OPTS" ]