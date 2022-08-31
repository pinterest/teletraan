FROM 998131032990.dkr.ecr.us-east-1.amazonaws.com/golang:1.14-focal

# Trigger new build. change number: 13
# gcc for cgo
RUN apt-get update && apt-get install -y --no-install-recommends \
        g++ \
        gcc \
        libc6-dev \
        make \
        curl \
        git \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ADD docker $GOPATH
RUN $(cd src/pinterest.com/testing/cmpdeploy && go mod init && go mod vendor)
RUN go install pinterest.com/testing/cmpdeploy
ENTRYPOINT $GOPATH/bin/cmpdeploy
EXPOSE 8000
