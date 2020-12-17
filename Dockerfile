FROM docker.io/library/maven:3-eclipse-temurin-17 as builder

WORKDIR /build
COPY . ./
RUN mvn -B -s settings.xml package

FROM docker.io/library/eclipse-temurin:17

WORKDIR /opt/app
COPY --from=builder /build/target/kubernetes-operator.jar ./

CMD [ "java", "-jar", "kubernetes-operator.jar" ]
