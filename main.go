package main

import (
	// "fmt"
	"os"

	"github.com/gorilla/mux"
	"github.com/joho/godotenv"
	"github.com/sfreiberg/gotwilio"
	"github.com/sirupsen/logrus"
)

const (
	ACCOUNTID_ENV = "ACCOUNTID"
	AUTHTOKEN_ENV = "AUTHTOKEN"
)

func main() {

	var log = logrus.New()

	err := godotenv.Load()
	if err != nil {
		log.Fatal("can't load dotenv")
	}

	accountSid := os.Getenv(ACCOUNTID_ENV)
	authToken := os.Getenv(AUTHTOKEN_ENV)

	twilio := gotwilio.NewTwilioClient(accountSid, authToken)

	log.SetFormatter(&logrus.JSONFormatter{})
	log.WithFields(logrus.Fields{}).Info("started")

	mux := mux.NewRouter()
	s := NewServer(mux, log, twilio)
	s.Route()

}
