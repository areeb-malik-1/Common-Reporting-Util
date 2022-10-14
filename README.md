# Common-Reporting-Util
Common reporting utility for all verticals

# Sample Command
* mvn compile
* mvn exec:java -Dexec.mainClass=com.tiket.db.DBUtil -Dvertical=all -Dtribe=all -Dmodule=all -Dplatform=all -Dtesttype=all -Denvironment=all -DrunID=1234

# Params
* vertical -> all | specific vertical name
  * Selecting all means no filter based on vertical
* tribe -> all | specific tribe name
  * Selecting all means no filter based on tribe
* module -> all | specific module name
  * Selecting all means no filter based on module
* platform -> all | ios | android
  * Selecting all means no filter based on platform
* testtype -> all | sanity | regression
  * Selecting all means no filter based on testtype
* environment -> all | staging | production
  * Selecting all means no filter based on environment
* runID -> particular run ID from testrail

# Notes for Intellij Idea IDE
* Turn on the annotation processor from preferences
* Install Lombok plugin if not already installed
