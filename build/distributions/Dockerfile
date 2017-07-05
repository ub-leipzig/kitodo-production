FROM tomcat:8-jre8-alpine

ENV KITODO_WEBAPP kitodo.war
ENV KITODO_BASE /etc/kitodo

# Kitodo webapp
COPY dist/${KITODO_WEBAPP} ${CATALINA_HOME}/webapps/kitodo.war
RUN mkdir -p ${KITODO_BASE}

COPY dist/*.xml ${KITODO_BASE}/
COPY dist/*.properties ${KITODO_BASE}/