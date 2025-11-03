# 빌드 스테이지 (Maven + JDK 포함)
FROM maven:3.9-eclipse-temurin-17-alpine AS build
# 작업 디렉토리를 /app으로 설정, 이후 모든 명령이 /app에서 실행됨
WORKDIR /app
# pom.xml 복사 및 의존성 다운로드
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:go-offline -B
# 소스 코드 복사
COPY src ./src
# 애플리케이션 빌드 (테스트 스킵)
RUN mvn clean package -DskipTests
#================================================================
# 실행 스테이지 (JRE만 포함 - 멀티스테이지의 핵심!)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 1단계에서 빌드된 jar만 복사 (빌드 도구는 제외)
# --from=build, 이전 스테이지에서 빌드된 jar 복사
COPY --from=build /app/target/springboot-0.0.1-SNAPSHOT.jar app.jar
# 포트 노출
EXPOSE 8080
# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
