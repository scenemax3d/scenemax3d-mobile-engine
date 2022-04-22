

############################
In the bash profile put the following:

export CLASSPATH=".:/usr/local/lib/antlr-4.5.3-complete.jar:$CLASSPATH"
alias antlr4='java -Xmx500M -cp "/usr/local/lib/antlr-4.5.3-complete.jar:$CLASSPATH" org.antlr.v4.Tool'
alias grun='java org.antlr.v4.gui.TestRig'


so now you have antlr4 and grun ready to use:
in the grammar file folder do the following:
antlr4 -no-listener -visitor SceneMax.g4
then:
ensure there is a build folder
javac -d ./build SceneMax*.java
enter build folder and type:
jar cvf scenemax_parser.jar *
then you will have a ready to use JAR file

So to summarize the build process:
  antlr4 -no-listener -visitor SceneMax.g4
  javac SceneMax*.java
  javac -d ./build SceneMax*.java
  cd build
  jar cvf scenemax_parser.jar *


//////////////////// CODE TO FOR TESTING //////////////////

type some code for example:
 ninja is a model from xxx;
 redninja is a model from yyy;
 adi is a ninja;
 asi is a redninja;
 adi.rotate(x+10, y+20, z+30);

 press CTRL+D

 if everything is right compile it:

