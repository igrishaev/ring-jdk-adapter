
repl:
	lein with-profile +dev,+test repl

.PHONY: test
test:
	lein test

release:
	lein release

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

install:
	lein install
