* Checklist
1. make sure java/javac uses jdk 1.8

* Install Redis if it's used
1. sudo apt-get install redis-server (if on ubuntu and having access)
2. go to https://redis.io/topics/quickstart and install as it documents
3. config and start redis-server with a configuration (to change data directory etc.)

* Install Elasticsearch if it's used

* Deployment after development and configuration.
1. go to your deployment_path
2. git clone git@github.com:grouplens/samantha.git
3. change the engine conf files in samantha/server/conf if necessary (e.g. the cronExpression of schedulers etc.)
4. cd samantha/server && mkdir -p data/learning data/models data/csvData data/backup
5. ./activator clean stage
6. cd target/universal/stage && ln -s $deployment_path/samantha/server/data data
7. bin/samantha-server -Dhttp.port=9100 -J-Xmx4g &
8. disown
