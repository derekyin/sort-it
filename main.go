package main

import (
	// "fmt"
	"context"
	"os"

	firebase "firebase.google.com/go"
	"github.com/gorilla/mux"
	"github.com/joho/godotenv"
	"github.com/sfreiberg/gotwilio"
	"github.com/sirupsen/logrus"
	"google.golang.org/api/option"
)

const (
	ACCOUNTID_ENV = "ACCOUNTID"
	AUTHTOKEN_ENV = "AUTHTOKEN"
)

func main() {

	var log = logrus.New()
	log.SetFormatter(&logrus.JSONFormatter{})
	log.WithFields(logrus.Fields{}).Info("started")

	err := godotenv.Load()
	if err != nil {
		log.Fatal("can't load dotenv")
	}

	accountSid := os.Getenv(ACCOUNTID_ENV)
	authToken := os.Getenv(AUTHTOKEN_ENV)

	sa := option.WithCredentialsFile("serviceaccount.json")
	app, err := firebase.NewApp(context.Background(), nil, sa)
	if err != nil {
		log.Fatalln(err)
	}

	client, err := app.Firestore(context.Background())
	if err != nil {
		log.Fatalln(err)
	}
	defer client.Close()

	twilio := gotwilio.NewTwilioClient(accountSid, authToken)

	mux := mux.NewRouter()

	repo := NewRepo(client, log)

	s := NewServer(mux, log, twilio, *repo)
	s.Route()

}
