all:    publish

clean:
	rm -fr quickstart-build.tar.gz

build:	clean
	tar -cf quickstart-build.tar.gz mysite teletraan

publish: build quickstart.py
	python quickstart.py
        



