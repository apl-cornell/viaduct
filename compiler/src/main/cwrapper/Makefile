
INCLUDES = -I/usr/lib/jvm/default-java/include -I/usr/lib/jvm/default-java/include/linux

FILES = libsnarkwrapper.cpp libsnarkwrapper_wrap.cpp

libsnarkwrapper.so: libsnarkwrapper_wrap.o
	g++ -shared -o $@ $^

libsnarkwrapper_wrap.o: libsnarkwrapper_wrap.cpp
	g++ -c -fpic $(INCLUDES) libsnarkwrapper_wrap.cpp

libsnarkwrapper_wrap.cpp: libsnarkwrapper.i
	swig -java -outdir ../java/edu/cornell/cs/apl/viaduct/libsnarkwrapper -package edu.cornell.cs.apl.viaduct.libsnarkwrapper -c++ -o $@ libsnarkwrapper.i

clean:
	rm libsnarkwrapper_wrap.cpp *.o *.so ../java/edu/cornell/cs/apl/viaduct/libsnarkwrapper/*

install: libsnarkwrapper.so
	cp libsnarkwrapper.so /usr/lib

