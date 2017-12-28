### Introduction

#### what is Samantha

* A generic recommender and predictor server. Machine learning is the core, but much more than that, particularly for the purpose of recommendation/ranking/candidate generation/evaluation.
* MIT licence, oriented to production use (online field experiments in research and typical industrial use)

#### what Samantha can do in simple words: full-fledged, self-contained server that can be used in production right away with one configuration file

* data management, including offline and online, in (indexing) and out (post-processing), bootstrap once for all
* model management, abstracted model operations
* data processing pipeline, data expanding and feature extraction framework
* state-of-the-art models: collaborative filtering, matrix factorization, knn, trees, boosting and bandits/reinforcement learning
* experimental framework for A/B and bucket testing
* feedback (for online learning/optimization) and evaluation (for experimenting) loops among application front-end, application back-end server and Samantha
* abstracted model parameter server, i.e. extensible variable and index spaces
* generic oracle-based optimization framework/solver with classic solvers
* flexible model dependency, e.g. model ensemble, stacking, boosting
* schedulers for regular model rebuilding or backup
* integration with other state-of-the-art systems, e.g. xgboost, TensorFlow etc.
* control and customize all these components through one centralized configuration file

#### why do I develop Samantha

* user-centered/human-centered research
* offline to online trend
* many options out there but no ideal

#### the targeted audience/users of Samantha (users in this doc refer to those who use Samantha; instead, end users is used to refer to the ultimate users of applications built using Samantha)

* individuals/organizations who want to deploy a data-driven predictive system with minimum effort. They might need it to support answering relevant research questions involving an intelligent predictive part in their system or just to have an initial try to see the effects of such a predictive component. 
* individuals/organizations who are working on comparing and developing new machine learning or recommendation models/algorithms, especially those who care about deploying their models/algorithms into production and evaluate them in front of end users

#### what to support in the near future

* text modeling
* scale up to clusters: learning algorithm and offline data storage

[Detailed Documentation](docs/SamanthaDoc.pdf)

### Note

* Samantha is a project developed by <a href="http://www-users.cs.umn.edu/~qian/">Qian Zhao</a>, a Ph.D. candiate at GroupLens lab and originated from his research projects. Currently, Samantha is in the process of integrating with <a href="http://lenskit.org/" target="_blank">Lenskit</a>. Feel free to give a try if you're intersted though. 

### For the paper GB-CENT

* The experiments were run with Samantha in: Qian Zhao, Yue Shi, Liangjie Hong. GB-CENT: Gradient Boosted Categorical Embedding and Numerical Trees. In Proceedings of the 26th International World Wide Web conference (WWW 2017), ACM, 2017.
* Go to branch <a href="https://github.com/grouplens/samantha/blob/qian/gbcent/docs/README.md">qian/gbcent, docs/README.md</a> for details
