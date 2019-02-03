package main

import (
	"context"

	"cloud.google.com/go/firestore"
	"github.com/sirupsen/logrus"
)

type Repo interface {
}

type repo struct {
	log      *logrus.Logger
	fbClient *firestore.Client
}

func NewRepo(fb *firestore.Client, logger *logrus.Logger) *repo {
	return &repo{
		log:      logger,
		fbClient: fb,
	}
}

func (r *repo) CreateUser(phone string, day int) {

	_, _, err := r.fbClient.Collection("signups").Add(context.Background(), map[string]interface{}{
		"dayofweek": day,
		"number":    phone,
	})
	if err != nil {
		r.log.Fatalf("Failed adding alovelace: %v", err)
	}
}
