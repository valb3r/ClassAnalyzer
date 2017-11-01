# ClassAnalyzer
Creates neo4j graph layout of classes and their relationships from a jar-application mapping

# Sample usage:
Imagine you have microservices project which when compiled is composed of
multiple jars. You want to analyze cross-application and other kinds of 
class relationships. Project is composed of subprojects A and B, which had 
compiled its jars into following folders:
1. Project A:
- /opt/projectA/lib1
- /opt/projectA/lib2
2. Project B:
- /opt/projectB/libs

To analyze its relationships (in this project root folder) execute cmd:

    ./gradlew  -PappMap="/opt/projectA/lib1,/opt/projectA/lib2=ProjectA;/opt/projectB/libs=ProjectB" \
    -PclassInclude="com\..*|org\..*"  \ 
    -PmethodInclude="com\..*|org\..*" \
    -PdbFile="/home/user/project.graphb"

Script will generate class relationship data in /home/user/project.graphb