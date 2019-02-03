package main

import (
	"context"

	"cloud.google.com/go/firestore"
	"github.com/sirupsen/logrus"
	"google.golang.org/api/iterator"
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

func (r *repo) GetAll() []User {

	users := make([]User, 0)
	iter := r.fbClient.Collection("signups").Documents(context.Background())
	for {
		doc, err := iter.Next()
		if err == iterator.Done {
			break
		}
		if err != nil {
			r.log.Fatalf("Failed to iterate: %v", err)
		}

		users = append(users, User{
			PhoneNumber: doc.Data()["number"].(string),
			DayOfWeek:   doc.Data()["dayofweek"].(int64),
		})
	}
	return users
}
