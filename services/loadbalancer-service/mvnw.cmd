@echo off
set "DIR=%~dp0"
java "-Dmaven.multiModuleProjectDirectory=%DIR%." -classpath "%DIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*