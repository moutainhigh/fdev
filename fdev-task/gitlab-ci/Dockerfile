FROM 10.191.60.105/mbpe-public-docker-local/library/fdev-base:0.2
ADD fdev-task-0.0.1-SNAPSHOT.jar  /ebank/app.jar
ADD start.sh /ebank/start.sh
ENTRYPOINT ["sh" , "/ebank/start.sh"]