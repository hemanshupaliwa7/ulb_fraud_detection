# ulb_fraud_detection
Contains Machine Learning with Spark and H2O Sparkling Water Package

# Download data 
from bigquery-public-data.ml_datasets.ulb_fraud_detection as 
```
SELECT *
FROM `bigquery-public-data.ml_datasets.ulb_fraud_detection`
;
```

After results are displayed, save results to local or drive.

# How to execute this application
If you are using any Intellij, then import this project as sbt and build project with all dependencies. DON'T Forget to copy C:\Users\<user-name>\.ivy2\cache\ai.h2o\sparkling-water-package_2.12\jars file to lib/ folder from this repository.

If you are running on any terminal, then you would need to install sbt and run the code with "sbt assembly" to build the package. and then execute following commands

```
sbt assembly
java -cp root\target\scala-2.12\ulb_fraud_prediction-assembly-1.0.jar com.cldcvr.FraudPredictor
```
