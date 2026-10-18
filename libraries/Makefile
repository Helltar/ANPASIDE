JDK8_HOME ?= /usr/lib/jvm/java-8-openjdk
WTK_HOME ?= $(CURDIR)/../../midletpascal-rtl/toolchains/sun-wtk
BUILD_DIR ?= $(CURDIR)/build
ASSETS_DIR ?= $(CURDIR)/../app/src/main/assets/libs

JAVAC := $(JDK8_HOME)/bin/javac
JAVAP := $(JDK8_HOME)/bin/javap
PREVERIFY := $(WTK_HOME)/bin/preverify1.1

RAW_DIR := $(BUILD_DIR)/raw
CLASSES_DIR := $(BUILD_DIR)/classes

JME_BOOTCLASSPATH := $(WTK_HOME)/lib/cldcapi10.jar:$(WTK_HOME)/lib/midpapi10.jar
LIBRARIES := $(sort $(wildcard src/Lib_*.java))
STUBS := build-support/M.java build-support/FW.java

.PHONY: all install clean

all: install

install: $(LIBRARIES) $(STUBS)
	rm -rf "$(BUILD_DIR)"
	mkdir -p "$(RAW_DIR)" "$(CLASSES_DIR)" "$(ASSETS_DIR)"
	"$(JAVAC)" -g:none -source 1.3 -target 1.1 -encoding UTF-8 \
		-bootclasspath "$(JME_BOOTCLASSPATH)" -extdirs "" \
		-d "$(RAW_DIR)" $(LIBRARIES) $(STUBS)
	"$(PREVERIFY)" -cldc1.0 -nofp -nofinalize -nonative \
		-classpath "$(JME_BOOTCLASSPATH):$(RAW_DIR)" \
		-d "$(CLASSES_DIR)" "$(RAW_DIR)"
	rm -f "$(CLASSES_DIR)/M.class" "$(CLASSES_DIR)/FW.class"
	@for class_file in "$(CLASSES_DIR)"/Lib_*.class; do \
		"$(JAVAP)" -verbose "$$class_file" | grep -q 'major version: 45' \
			|| { echo "$$class_file is not a java 1.1 class" >&2; exit 1; }; \
		"$(JAVAP)" -verbose "$$class_file" | grep -q 'StackMap:' \
			|| { echo "$$class_file is not preverified" >&2; exit 1; }; \
	done
	cp "$(CLASSES_DIR)"/Lib_*.class "$(ASSETS_DIR)/"

clean:
	rm -rf "$(BUILD_DIR)"
