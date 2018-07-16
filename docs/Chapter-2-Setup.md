

### Development mode

* download jdk 8 and set up JAVA_HOME and PATH=$PATH:$JAVA_HOME/bin
* git clone git@github.com:grouplens/samantha.git
* cd samantha/server && mkdir -p data/learning data/models data/indexed data/backup
* ./activator -jvm-debug 9999 -Dhttp.port=9100 -J-Xmx4g run
* go to browser and access: http://localhost:9100

### Deployment mode

* set up JAVA_HOME and PATH=$PATH:$JAVA_HOME/bin
* go to your deployment path (denoted as $deployment_path)
* git clone git@github.com:grouplens/samantha.git
* cd samantha/server && mkdir -p data/learning data/models data/indexed data/backup
* ./activator clean stage
* cd target/universal/stage && ln -s $deployment_path/samantha/server/data data
* bin/samantha-server -Dhttp.port=9100 -J-Xmx4g &
* disown
* go to browser and access: http://localhost:9100
