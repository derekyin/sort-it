package main

import (
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/gorilla/mux"
	"github.com/sfreiberg/gotwilio"
	"github.com/sirupsen/logrus"
)

type Server struct {
	router *mux.Router
	logger *logrus.Logger
	twilio *gotwilio.Twilio
	repo   repo
}

func NewServer(router *mux.Router, logger *logrus.Logger, twilio *gotwilio.Twilio, repo repo) *Server {
	return &Server{
		router: router,
		logger: logger,
		twilio: twilio,
		repo:   repo,
	}
}

func (s *Server) Route() {

	s.router.HandleFunc("/ping", s.Ping)
	s.router.HandleFunc("/signup/{phonenumber}/{days}", s.Signup).Methods(http.MethodPost)
	s.router.HandleFunc("/getall", s.Getall)
	s.logger.Info(http.ListenAndServe(":8000", s.router))
}

func (s *Server) Ping(w http.ResponseWriter, r *http.Request) {

	fmt.Fprintf(w, "OK")
}

func (s *Server) Signup(w http.ResponseWriter, r *http.Request) {

	phonenumber := mux.Vars(r)["phonenumber"]

	days, err := s.parseID(mux.Vars(r), "days")
	if err != nil {
		s.logger.Println("project not found")
		return
	}

	fmt.Println(phonenumber, days)

	from := "+13658040255"
	to := fmt.Sprintf("%s", phonenumber)

	message := fmt.Sprintf("You have signed up for %d day ahead for your recycling reminders", days)

	a, _, err := s.twilio.SendSMS(from, to, message, "", "")
	if err != nil {
		s.logger.Println(err)
	}

	s.logger.Println(a)

	s.repo.CreateUser(phonenumber, days)

	fmt.Fprintf(w, "Signed up!")
}

func (s *Server) parseID(params map[string]string, idType string) (int, error) {

	id, err := strconv.Atoi(params[idType])
	if err != nil {
		return -1, err
	}

	return id, nil
}

func (s *Server) Getall(w http.ResponseWriter, r *http.Request) {
	users := s.repo.GetAll()

	weekday := time.Now().Weekday()

	for _, i := range users {
		if i.DayOfWeek == int64(weekday) {
			s.SendText(i.PhoneNumber)
		}
	}
}

func (s *Server) SendText(phone string) {

	from := "+13658040255"
	to := fmt.Sprintf("+1%s", phone)

	message := fmt.Sprintf("Reminder to take out your trash!")

	a, _, err := s.twilio.SendSMS(from, to, message, "", "")
	if err != nil {
		s.logger.Println(err)
	}

	s.logger.Println(a)
}
