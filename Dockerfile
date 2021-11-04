# FINAL IMAGE
FROM openjdk:11-jre-slim-buster

LABEL maintainer="Aiden ZHANG Wenyi <im.endangeredfish@gmail.com>"
ENV SHINY_USER shinyproxy
ENV PROXY_TEMPLATEPATH /opt/shinyproxy/resources
ENV SERVER_ERROR_WHITELABEL_ENABLED false
ENV TZ Asia/Hong_Kong

RUN useradd -c 'shinyproxy user' -m -d /home/$SHINY_USER -s /bin/nologin $SHINY_USER
COPY --from=builder --chown=$SHINY_USER:$SHINY_USER /opt/shinyproxy/ /opt/shinyproxy/

WORKDIR /opt/shinyproxy
USER $SHINY_USER

# Start ShinyProxy with some extra parameters for faster startup (https://spring.io/blog/2018/11/08/spring-boot-in-a-container#tweaks)
CMD ["java", "-noverify",  "-jar", "/opt/shinyproxy/shinyproxy.jar", "--spring.jmx.enabled=false"]
USER root
