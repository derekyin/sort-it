FROM golang:1.11-alpine

RUN apk add --no-cache git

COPY . /go/src/github.com/derekyin/sort-it
WORKDIR  /go/src/github.com/derekyin/sort-it

RUN go get github.com/pilu/fresh

EXPOSE 8000

CMD mfresh -c runner.conf
