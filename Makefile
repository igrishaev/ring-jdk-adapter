
repl:
	lein with-profile +dev,+test repl

.PHONY: test
test:
	lein test
