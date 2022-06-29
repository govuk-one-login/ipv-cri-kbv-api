FROM gradle:jdk11
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && unzip awscliv2.zip && ./aws/install
COPY . .
RUN mv run-tests.sh /run-tests.sh
ENTRYPOINT ["/run-tests.sh"]
