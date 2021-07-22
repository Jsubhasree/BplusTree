.SUFFIXES: .java .class
.java.class:
		javac -g $*.java

CLASSES = \
		bplustree.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) *.class