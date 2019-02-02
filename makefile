dev:
	docker build -f Dockerfile . -t sort-it

run:
	docker run -it -p 8000:8000 -v $(PWD):/go/src/github.com/derekyin/sort-it sort-it