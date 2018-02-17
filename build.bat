@ECHO OFF

if not exist build mkdir build
javac -d build tee/Tee.java tee/TeeTests.java
