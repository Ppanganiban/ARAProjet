JAVAC=javac
JAVA=java
JAR=jar

CLASSPATH_DIR=~/Work/M2/ARAProjet/LeaderElection/lib
CLASSPATH=-classpath $(CLASSPATH_DIR)/peersim-doclet.jar -classpath $(CLASSPATH_DIR)/djep-1.0.0.jar -classpath $(CLASSPATH_DIR)/jep-2.3.0.jar -classpath $(CLASSPATH_DIR)/peersim-1.0.5.jar

SRC_DIR=~/Work/M2/ARAProjet/LeaderElection/src
MANIFEST_DIR=$(SRC_DIR)/META-INF
MANIFEST=$(MANIFEST_DIR)/MANIFEST.MF
SRCS=*.java
OBJS=$(SRCS:.java=.class)
C_DIR=obj

OUTPUT_JAR=ElectionLeader.jar

all: move-to-c-dir create-jar move-jar

move-jar:
	mv $(C_DIR)/$(OUTPUT_JAR) .

create-jar:
	(cd $(C_DIR) && $(JAR) cmvf META-INF/MANIFEST.MF $(OUTPUT_JAR) `find . -name '*.class'`)

extract-libs-jar:
	(cd $(C_DIR) && cp $(CLASSPATH_DIR)/djep-1.0.0.jar .)
	(cd $(C_DIR) && $(JAR) xf $(CLASSPATH_DIR)/djep-1.0.0.jar)
	(cd $(C_DIR) && rm djep-1.0.0.jar)

	(cd $(C_DIR) && cp $(CLASSPATH_DIR)/jep-2.3.0.jar .)
	(cd $(C_DIR) && $(JAR) xf $(CLASSPATH_DIR)/jep-2.3.0.jar)
	(cd $(C_DIR) && rm jep-2.3.0.jar)

	(cd $(C_DIR) && cp $(CLASSPATH_DIR)/peersim-doclet.jar .)
	(cd $(C_DIR) && $(JAR) xf $(CLASSPATH_DIR)/peersim-doclet.jar)
	(cd $(C_DIR) && rm peersim-doclet.jar)

	(cd $(C_DIR) && cp $(CLASSPATH_DIR)/peersim-1.0.5.jar .)
	(cd $(C_DIR) && $(JAR) xf $(CLASSPATH_DIR)/peersim-1.0.5.jar)
	(cd $(C_DIR) && rm peersim-1.0.5.jar)

compile-class:
	(cd $(SRC_DIR) && $(JAVAC) $(CLASSPATH) $(SRCS))

move-to-c-dir: $(C_DIR) compile-class extract-libs-jar
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
