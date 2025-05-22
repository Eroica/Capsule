.PHONY: clean jnilib

ifndef JAVA_HOME
$(error JAVA_HOME is not set)
endif

CXXFLAGS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin -Dnullptr=0

gemtext: gemtext.tab.c lex.yy.c main.c
	gcc $^ -ll -o $@

gemtext.tab.c: gemtext.y
	bison -d $^

gemtext.tab.h: gemtext.y
	bison -d $^

lex.yy.c: gemtext.l
	flex $^

jnilib: libgemicom.jnilib

libgemicom.jnilib: gemicom.cpp gemtext.tab.c lex.yy.c
	gcc $(CXXFLAGS) -shared -o $@ $^

clean:
	rm gemtext gemtext.tab.c gemtext.tab.h lex.yy.c
