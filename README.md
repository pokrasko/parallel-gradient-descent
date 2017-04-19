# Gradient descent optimization

This is gradient descent optimization algorithm based
on [this paper](http://www.cse.buffalo.edu/faculty/miller/Courses/CSE633/Li-Hui-Fall-2012-CSE633.pdf).
The objective function here is cost function of linear regression.
The implementation works on the set of pre-generated points (as the used input format is binary).

[Vert.x](http://vertx.io) is used as application engine here.
The app uses a JSON configuration file to get the parameters such as input filename, output filename, etc.

## Single flow implementation

This implementation uses a single verticle to calculate gradient and cost function.

Configuration parameters:
* **"input"** &mdash; input filename (*a string*)
* **"output"** &mdash; output filename (*a string;
    optional, if not specified, the results are written into standard output*)
* **"convergence"** &mdash; convergence parameter, i. e. value which defines app termination:
    if the difference between cost function values of two last iterations are less than it, the app terminates
    (*a double*)

To run it use
```sh
mvn clean package
java -jar single-flow/target/single-flow-1.0-fat.jar -conf <configuration-file>
```
or
```sh
mvn clean package
vertx run ru.pokrasko.pgd.singleflow.SingleFlowOptimizer -cp single-flow/target/single-flow-1.0-fat.jar -conf <configuration-file>
```

## Parallel implementation

This implementation uses multiple computing verticles (**slave verticles**) which calculate partial sums
for cost function and its gradient. These verticles are connected with a **master verticle**
which sums up partial values, updates weights, sends them and decides when to stop a computation.

Configuration parameters:
* **"input"** &mdash; input filename (*a string*)
* **"output"** &mdash; output filename (*a string; optional*)
* **"slaves"** &mdash; the number of slave verticles (*an integer*)
* **"convergence"** &mdash; convergence parameter (*a double*)

To run it use
```sh
mvn clean package
java -jar parallel/target/parallel-1.0-fat.jar -conf <configuration-file>
```
or
```sh
mvn clean package
vertx run ru.pokrasko.pgd.parallel.ParallelMainVerticle -cp parallel/target/parallel-1.0-fat.jar -conf <configuration-file>