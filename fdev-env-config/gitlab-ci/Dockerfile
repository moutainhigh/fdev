FROM 10.191.60.105/mbpe-public-docker-local/library/frelease-base:latest
ADD fdev-env-config-0.0.1-SNAPSHOT.jar  /ebank/app.jar
ADD start.sh /ebank/start.sh
ADD archetype.sh /ebank/archetype.sh
RUN chmod +x -R /ebank

ENTRYPOINT ["sh" , "/ebank/start.sh"]