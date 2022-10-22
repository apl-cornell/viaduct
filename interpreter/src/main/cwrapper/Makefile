
INCLUDES = -I/usr/lib/jvm/default-java/include -I/usr/lib/jvm/default-java/include/linux

FILES = libsnarkwrapper.cpp libsnarkwrapper_wrap.cpp

PACKAGE = io.github.aplcornell.viaduct.libsnarkwrapper
PACKAGE_DIR = $(subst .,/,$(PACKAGE))

libsnarkwrapper.so: libsnarkwrapper_wrap.o
	g++ -shared -o $@ $^ -L/usr/local/lib -lff -lgmp -lgmpxx

libsnarkwrapper_wrap.o: libsnarkwrapper_wrap.cpp
	g++ -c -fpic $(INCLUDES) libsnarkwrapper_wrap.cpp

libsnarkwrapper_wrap.cpp: libsnarkwrapper.i libsnarkwrapper.hpp utils.hpp
	mkdir -p ../java/$(PACKAGE_DIR) && swig -java -outdir ../java/$(PACKAGE_DIR) -package $(PACKAGE) -c++ -o $@ libsnarkwrapper.i


clean:
	rm libsnarkwrapper_wrap.cpp *.o *.so ../java/(PACKAGE_DIR)/*

install: libsnarkwrapper.so
	cp libsnarkwrapper.so /usr/lib

