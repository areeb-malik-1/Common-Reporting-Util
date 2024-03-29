# Common-Reporting-Util
Common reporting utility for all verticals

# Sample Command
* mvn compile
* mvn exec:java -Dexec.mainClass=com.tiket.app.db.DBUtil -Dproject=app -Dvertical=all -Dtribe=all -Dmodule=all -Dplatform=all -Dtesttype=all -Denvironment=all -DrunID=1234 "-DemailIDs=email1@tiket.com, email2@tiket.com"
* mvn exec:java -Dexec.mainClass=com.tiket.api.db.DBUtil -Dproject=api -Dvertical=all -Dtribe=all -Dmodule=all -Dtesttype=all -Denvironment=all "-DemailIDs=email1@tiket.com, email2@tiket.com"

# Params
* project -> app | api
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
* environment -> all | staging | production | gk2
  * Selecting all means no filter based on environment
* runID -> particular run ID from testrail
* emailIDs -> comma separated email IDs
  * this is optional, if not provided emails will not be sent.

# Notes for Intellij Idea IDE
* Turn on the annotation processor from preferences
* Install Lombok plugin if not already installed
