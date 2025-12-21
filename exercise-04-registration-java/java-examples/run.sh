# Terminal 1 - Start Temporal
temporal server start-dev

# Terminal 2 - Start Worker
mvn compile exec:java -Dexec.mainClass="helloworld.Worker"

# Terminal 3 - Run Client
mvn compile exec:java -Dexec.mainClass="helloworld.Client"