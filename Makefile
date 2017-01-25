JAVAC=javac
JAVA=java
JAR=jar

LIB_DIR=~/Work/M2/ARAProjet/LeaderElection/lib
LIBS=`find $(LIB_DIR) -name '*.jar'`
CLASSPATH=-cp $(C_DIR)

SRC_DIR=~/Work/M2/ARAProjet/LeaderElection/src
MANIFEST_DIR=$(SRC_DIR)/META-INF
MANIFEST=$(MANIFEST_DIR)/MANIFEST.MF
SRCS=*.java
OBJS=$(SRCS:.java=.class)
C_DIR=~/Work/M2/ARAProjet/obj

OUTPUT_JAR=ElectionLeader.jar

all: move-to-c-dir create-jar move-jar

move-jar:
	mv $(C_DIR)/$(OUTPUT_JAR) .

create-jar:
	(cd $(C_DIR) && $(JAR) cmvf META-INF/MANIFEST.MF $(OUTPUT_JAR) `find . -name '*.class'`)

extract-libs-jar:
	cp $(LIB_DIR)/*.jar $(C_DIR)
	(cd $(C_DIR) && find -name '*.jar' -exec $(JAR) xf {} \;)
	rm $(C_DIR)/*.jar

compile-class:
	(cd $(SRC_DIR) && $(JAVAC) $(CLASSPATH) $(SRCS))

move-to-c-dir: $(C_DIR) extract-libs-jar compile-class
	mv $(SRC_DIR)/$(OBJS) $(C_DIR)
	cp -r $(MANIFEST_DIR) $(C_DIR)

$(C_DIR):
	mkdir -p $(C_DIR)

clean:
	rm -r $(C_DIR)
	@find . -name *.class -delete -print
	@find . -name $(OUTPUT_JAR) -delete -print

run:
	java -jar $(OUTPUT_JAR) LeaderElection/configs/config-test.txt
