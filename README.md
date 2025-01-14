# Trace-DFA

Finds the minimum Deterministic Finite-State Automaton (*DFA*) that is consistent with the given sequences to accept and to reject.

The program reads .xes files and uses these logs as input sequences.

This project implements: Marijn J. H. Heule and Sicco Verwer. 2010. Exact DFA identification using SAT solvers.


## Preliminaries

Download the needed JARs in `libs/`:

    ./scripts/download-jars.sh


Install [Gradle](https://gradle.org/) to build and run the app.

## Input sequences

The input of the program is composed of two sets of sequences: one for learning the DFA and one for testing. All sequences must be in XES format. By default, the program will use the directory "traces/train" for learning and "traces/test" for testing. However one may specify arbitrary paths when running the program, using options:

    gradle run --args="train_dir test_dir output_dir"

Each sequence in the log is a trace in XES format composed of events. Just the name of the events are used to identify transitions.  Add "OK" in the filename of XES files containing sequences to be accepted by the DFA. Sequences to reject need no modification.

## Run

Run with `gradle run --args="train_dir test_dir output_dir"`.

Or, using the JAR:

    java -jar trace-dfa.jar traces/train/ traces/test/ traces/output
    
You can easily produce the JAR by doing `gradle jar`. It will be located in `build/libs/`.

Please check the [examples](./examples) folder to get some working examples.

## Output

The program will create a directory "output" with tree Latex files:

* `dfa.tex` is the main output of the program. This is the extracted Finite State Automaton which is consistent with the given traces.
* `apta.tex` is the APTA the algorithm uses internally to represent the input traces (to learn). For a big number of traces you won't be able to compile this, due to space limitation of the Tex page.
* `constraints.tex` is the graph of constraints the algorithm internally uses to represent the constraints in the coloring problem. This also could exceed Tex limitations.

The DFA will be also saved in a .dot text file in the same directory. Tools such as GraphViz can read dot files.
