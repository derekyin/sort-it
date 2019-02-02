package main

import (
	"fmt"
	"net/http"

	"github.com/gorilla/mux"
	"github.com/sfreiberg/gotwilio"
	"github.com/sirupsen/logrus"
)

type Server struct {
	router *mux.Router
	logger *logrus.Logger
	twilio *gotwilio.Twilio
}

func NewServer(router *mux.Router, logger *logrus.Logger, twilio *gotwilio.Twilio) *Server {
	return &Server{
		router: router,
		logger: logger,
		twilio: twilio,
	}
}

func (s *Server) Route() {

	s.router.HandleFunc("/ping", s.Ping)
	s.router.HandleFunc("/signup/{phonenumber}/{days}", s.Signup).Methods(http.MethodPost)
	s.logger.Info(http.ListenAndServe(":8000", s.router))
}

func (s *Server) Ping(w http.ResponseWriter, r *http.Request) {

	fmt.Fprintf(w, "OK")
}

func (s *Server) Signup(w http.ResponseWriter, r *http.Request) {

	phonenumber := mux.Vars(r)["phonenumber"]
	days := mux.Vars(r)["days"]

	from := "+13658040255"
	to := fmt.Sprintf("+1%s", phonenumber)

	message := fmt.Sprintf("You have signed up for %s day ahead for your recycling reminders", days)

	a, _, err := s.twilio.SendSMS(from, to, message, "", "")
	if err != nil {
		s.logger.Println(err)
	}

	s.logger.Println(a)

	fmt.Fprintf(w, "Signed up!")
}
