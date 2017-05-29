### For the paper 
* Qian Zhao, Yue Shi, Liangjie Hong. GB-CENT: Gradient Boosted Categorical Embedding and Numerical Trees. In Proceedings of the 26th International World Wide Web conference (WWW 2017), ACM, 2017.

### Preparation

* git clone https://github.com/grouplens/samantha.git
* cd samantha
* git checkout qian/gbcent
* cd server && mkdir -p data/learning data/models data/csvData data/backup
* ./activator -Dhttp.port=9100 -J-Xmx4g (with this you've set up a Samantha server instance)
* start using Samantha server as following
* go to your browser and visit: http://localhost:9100/, it should list a list of HTTP APIs that you can use. It doesn't have the details of how to use the APIs. For now, read samantha/docs/SamanthaDoc.pdf
* edit engine configurations in conf/*.conf where conf/application.conf is the main entry point. samantha.engines.enabled controls which engines are enabled by default. 
* if you only use gbcent, then the default enabled engine "gbcent" is good enough. You can edit gbcent.conf to change its feature extraction process, loss function, optimization methods etc.
* after configuring the engine, you can go to docs/Demo.ipynb to learn how to interact with Samantha server through HTTP (e.g. python requests package)
* if you enable redhat.conf and movielens.conf, you need to install xgboost4j because these two engines depend on it: https://xgboost.readthedocs.io/en/latest/jvm/index.html
* after installing xgboost4j, you also need to make sure that you clone the submodule extension for xgboost: https://github.com/grouplens/samantha/tree/qian/gbcent/server/extension
* lastly, to enable engines depending on xgboost, uncomment the section for xgboost in built.sbt
* to see how to interact with engine redhat.conf, see samantha/docs/WWW17.ipynb
