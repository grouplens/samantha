### What is Samantha

* A generic recommender and predictor server for both offline machine learning and recommendation modeling and fast online production serving.
* MIT licence, oriented to production use (online field experiments in research and typical industrial use)

### What Samantha Can Do

* Full-fledged, self-contained server that can be used in production right away with one configuration file, including the following components
* Data management, including offline and online, in (indexing) and out (post-processing), through configurable backends of most relational databases (e.g. MySQL, PostresSQL, SQLServer etc.), ElasticSearch or Redis.
* Model management, including online updating, building, loading, dumping and serving.
* Data processing pipeline based on a data expanding and feature extraction framework
* State-of-the-art models: collaborative filtering, matrix factorization, knn, trees, boosting and bandits/reinforcement learning
* Experimental framework for randomized A/B and bucket testing
* Feedback (for online learning/optimization) and evaluation (for experimenting) loops among application front-end, application back-end server and Samantha
* Abstracted model parameter server (through extensible variable and index spaces)
* Generic oracle-based optimization framework/solver with classic solvers
* Flexible model dependency, e.g. model ensemble, stacking, boosting
* Schedulers for regular model rebuilding or backup
* Integration with other state-of-the-art systems including XGBoost and TensorFlow.
* Control and customize all these components through one centralized configuration file

### The Targeted Users of Samantha

* Individuals or organizations who want to deploy a data-driven predictive system with minimum effort. They might need it to support answering relevant research questions involving an intelligent predictive part in their system or just to have an initial try to see the effects of such a predictive component. 
* Individuals or organizations who are working on comparing and developing new machine learning or recommendation models or algorithms, especially those who care about deploying their models/algorithms into production and evaluate them in front of end users

### Documentation

[Introduction](docs/Chapter-1-Introduction.pdf)

[Setup](docs/Chapter-2-Setup.md)

### Citation of the Tool

* Qian Zhao. 2018. User-Centric Design and Evaluation of Online Interactive Recommender Systems. Ph.D. Thesis. University of Minnesota.

### Note

* Samantha is a project developed by <a href="https://qzhao2018.github.io/zhao/">Qian Zhao</a>, Ph.D. at GroupLens Research lab (graduated on May 2018) and originated from his research projects there. Samantha might be integrated with <a href="http://lenskit.org/" target="_blank">Lenskit</a> in future.

### Publications Based On Samantha

* Qian Zhao, Yue Shi, Liangjie Hong. GB-CENT: Gradient Boosted Categorical Embedding and Numerical Trees. In <i>Proceedings of the 26th International World Wide Web conference (WWW 2017)</i>, ACM, 2017. (see branch <a href="https://github.com/grouplens/samantha/blob/qian/gbcent/docs/README.md">qian/gbcent, docs/README.md</a> for details)

* Qian Zhao, Jilin Chen, Minmin Chen, Sagar Jain, Alex Beutel, Francois Belletti, Ed Chi. 2018. Categorical-Attributes-Based Item Classification for Recommender Systems. In <i>Proceedings of The 12th ACM Conference on Recommender Systems (RecSys’18)</i>. ACM, New York, NY, USA. (see branch <a href="https://github.com/grouplens/samantha/tree/qian/hsm/tools/tensorflow/src">qian/hsm</a> for details)

* Qian Zhao, Martijn Willemsen, Gediminas Adomavicius, F. Maxwell Harper, Joseph A. Konsta. 2019. From Preference Into Decision Making: Modeling User Interactions in Recommender Systems. In <i>Proceedings of The 13th ACM Conference on Recommender Systems (RecSys’19)</i>. ACM, New York, NY, USA. (see branch <a href="https://github.com/grouplens/samantha/tree/qian/interaction/tools/tensorflow/src">qian/interaction</a> for details)

* Qian Zhao, F. Maxwell Harper, Gediminas Adomavicius, Joseph Konstan. Explicit or Implicit Feedback? Engagement or Satisfaction? A Field Experiment on Machine-Learning-Based Recommender Systems. In <i>Proceedings of the 33rd ACM/SIGAPP Symposium On Applied Computing, Track of Recommender Systems: Theory, User Interactions and Applications (SAC 2018)</i>, ACM, 2018. (see <a href="https://github.com/grouplens/samantha/tree/master/server/app/org/grouplens/samantha/server/reinforce">Reinforce-State</a>, <a href="https://github.com/grouplens/samantha/blob/master/server/app/org/grouplens/samantha/server/predictor/LinearUCBPredictorConfig.java">Bandit-\*</a>, <a href="https://github.com/grouplens/samantha/blob/master/server/app/org/grouplens/samantha/server/predictor/SVDFeaturePredictorConfig.java">MF-\*</a>)
