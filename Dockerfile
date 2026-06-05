FROM eclipse-temurin:21-jdk-noble

ARG TIMEZONE="set the time zone at build time"
ENV TIMEZONE=${TIMEZONE}
ARG APP="set the app at build time"
ENV APP=${APP}
ARG USERNAME="set the username as build time"
ENV USERNAME=${USERNAME}
ARG CURRENT_GID="set the gid"
ENV CURRENT_GID=${CURRENT_GID}
ARG CURRENT_UID="set the uid"
ENV CURRENT_UID=${CURRENT_UID}

RUN getent group ${CURRENT_GID} >/dev/null 2>&1 \
    && groupmod -n ${USERNAME} "$(getent group ${CURRENT_GID} | cut -d: -f1)" \
    || groupadd -g ${CURRENT_GID} ${USERNAME}
RUN getent passwd ${CURRENT_UID} >/dev/null 2>&1 \
    && usermod -l ${USERNAME} "$(getent passwd ${CURRENT_UID} | cut -d: -f1)" \
    || useradd -m -u ${CURRENT_UID} -g ${CURRENT_GID} ${USERNAME}

RUN ln -sf /usr/share/zoneinfo/${TIMEZONE} /etc/localtime
RUN mkdir -p -m 0755 /opt/${APP}/bin
RUN mkdir -p -m 0755 /opt/${APP}/logs
RUN mkdir -p -m 0755 /opt/${APP}/ssl
COPY ./ssl /opt/${APP}/ssl
ADD ./build/libs/${APP}.jar /opt/${APP}/bin/${APP}.jar
RUN chown -R ${USERNAME}:${USERNAME} /opt/${APP}

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && apt-get purge -y --auto-remove \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

WORKDIR /opt/${APP}

USER ${USERNAME}

EXPOSE 8443
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -k -f https://localhost:8443/api/account/totals || exit 1

CMD java -Duser.timezone=${TIMEZONE} \
    -Xmx2048m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /opt/${APP}/bin/${APP}.jar
