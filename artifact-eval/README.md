# Viaduct - Artifact Evaluation Instructions

This document contains information for evaluating the artifact for the
submission "Viaduct: An Extensible, Optimizing Compiler for Secure Distributed
Programs." It is in two parts: Section 1 ("Getting Started") has instructions
to build the artifact, provided as a Docker image, and contains the basics of
how to use the Viaduct compiler. Section 2 ("Replicating Evaluation Results")
describes the infrastructure we have provided to replicate the results in
the submission.


## 1 - Getting Started

### 1.1 - Building the artifact

We provide the artifact for the Viaduct compiler through a Docker image, so
these instructions assume that your machine already has Docker installed. We
have tested the artifact building process on OS X (VERSION) and Linux (Ubuntu
20.0.1) operating systems with Docker version (VERSION), but it should work on
any machine with a relatively recent version of Docker.

To use the image, build and run the submitted `Dockerfile` by running the
following shell command:

```
docker build --tag viaduct .
```

Make sure that the `Dockerfile` is in the current working directory before
running this command.

Once the container has been built, run this command:

```
docker run --rm -it viaduct
```

Your shell should now be running inside the container.


### Using the Viaduct compiler

The container is set up so that the compiler is already built.
When you enter the `viaduct` command, you should see the compiler's help
text as follows:

```
Usage: viaduct [OPTIONS] COMMAND [ARGS]...

  Compile high level specifications to secure distributed programs.

Options:
  -v, --verbose  Print debugging information

                 Repeat for more and more granular messages.
  --version      Show the version and exit
  -h, --help     Show this message and exit

Commands:
  format               Pretty print source program
  compile              Compile ideal protocol to secure distributed program
  generate-completion  Generate a tab-complete script for the given shell
  specification        Generate UC ideal functionality from source program
  run                  Run compiled protocol for a single host
```

There are two main compiler commands you need to know. The `compile` command
compiles Viaduct source programs into distributed programs. The `run`
command allows hosts to execute a compiled distributed program together.
We will go over the basics of using these two commands in the following
sections. You can run `viaduct compile --help` and `viaduct run --help` to get
more information about each of these commands and the various options and flags
they support.


#### 1.1.1 - Compiling source programs

To test compilation, we can compile one of the example programs in the
`benchmarks` folder. For example, we can compile `HistoricalMillionaires.via`
program as follows. Note that the `-v` flag turns on verbose mode, which
prints log information to stdout, and the `-o` option allows you to specify
the output file where the compiled distributed program will be written.
Now run the following command:

```
viaduct -v compile benchmarks/HistoricalMillionaires.via -o hm-out.via
```

You should see something similar to the following printed on stdout:

```
 479 ms [main] INFO  Compile - elaborating source program...
 516 ms [main] INFO  Compile - specializing functions...
 519 ms [main] INFO  Check - name analysis...
 530 ms [main] INFO  Check - type checking...
 533 ms [main] INFO  Check - out parameter initialization analysis...
 536 ms [main] INFO  Check - information flow analysis...
 580 ms [main] INFO  InformationFlowAnalysis - number of label variables: 54
 580 ms [main] INFO  Check - finished information flow analysis, ran for 44ms
 586 ms [main] INFO  Compile - selecting protocols...
1146 ms [main] INFO  Z3Selection - number of symvars: 187
1147 ms [main] INFO  Z3Selection - cost mode set to MINIMIZE
1286 ms [main] INFO  Z3Selection - constraints satisfiable, extracted model
1297 ms [main] INFO  Compile - finished protocol selection, ran for 689ms
1305 ms [main] INFO  Compile - annotating program with protocols...
```

and if you open `hm-out.via`, you should see:

```
host alice : {(A & B<-)}

host bob : {(B & A<-)}

process main {
    val length: int{(A ⊓ B)}@Replication(hosts = {alice, bob}) = 100;
    var a_min: int{(A & B<-)}@Local(host = alice) = 0;
    var b_min: int{(B & A<-)}@Local(host = bob) = 0;
    var i: int{(A ⊓ B)}@Replication(hosts = {alice, bob}) = 0;
    loop {
        let $tmp@Replication(hosts = {alice, bob}) = i;
        let $tmp_1@Replication(hosts = {alice, bob}) = length;
        let $tmp_2@Replication(hosts = {alice, bob}) = ($tmp < $tmp_1);
        if ($tmp_2) {
            let $tmp_3@Local(host = alice) = a_min;
            let $tmp_4@Local(host = alice) = input int from alice;
            let $tmp_5@Local(host = alice) = (min($tmp_3, $tmp_4));
            a_min = $tmp_5;
            let $tmp_6@Local(host = bob) = b_min;
            let $tmp_7@Local(host = bob) = input int from bob;
            let $tmp_8@Local(host = bob) = (min($tmp_6, $tmp_7));
            b_min = $tmp_8;
            i += 1;
        } else {
            break;
        }
    }
    let $tmp_9@Local(host = alice) = a_min;
    let $tmp_10@Local(host = bob) = b_min;
    let $tmp_11@YaoABY(client = bob, server = alice) = ($tmp_9 > $tmp_10);
    let $tmp_12@Replication(hosts = {alice, bob}) = declassify $tmp_11 to {(A ⊓ B)};
    val a_wins: bool{(A ⊓ B)}@Replication(hosts = {alice, bob}) = $tmp_12;
    let $tmp_13@Replication(hosts = {alice, bob}) = a_wins;
    output $tmp_13 to alice;
    let $tmp_14@Replication(hosts = {alice, bob}) = a_wins;
    output $tmp_14 to bob;
}
```

Notice that the distributed program is an elaborated version of the source
program where each variable declaration and let-binding is annotated with
the protocol that will execute it. As described in the paper, the
compiled distributed program is optimized so that Alice and Bob compute
their respective minima locally, and then use MPC (the YaoABY protocol above)
to perform the comparison.


#### 1.1.2 - Running compiled programs

The `run` command takes as arguments a host name and a compiled distributed
program and executes the host's "projection" of the distributed program.
To execute the compiled program `hm-out.via` for the historical millionaires'
game, we need two participants standing in for hosts `alice` and `bob`
respectively. The easiest way to do this is by running a terminal multiplexer
such as `tmux` and running a participant on two separate terminal instances.

Start a `tmux` session with two terminal instances---make sure that the current
working directory is the home directory (`~/`) of the container, as the
commands below assume it. Then run this command one terminal instance:

```
viaduct -v run alice hm-out.via -in alice-input.txt
```

and this command on another:

```
viaduct -v run bob hm-out.via -in bob-input.txt
```

As you can see, each terminal instance is running a participant in the
compiled distributed program for the historical millionaires' game.
The `-in` option allows you to specify an file from which a participant provides
input. Thus the `input int from alice` command in the compiled program will
read lines from the file `alice-input.txt`, while `input int from bob` will
read lines from `bob-input.txt`. By omitting the `-in` option, you can also
provide input through stdin; a participant will block on an `Input: ` prompt
when you need to provide input. Note that the default input size for the
historical millionaires' game is 100 though, so it will be tedious
to provide input this way.

On the terminal running host `alice`, you should see something similar to
the following printed on stdout:

```
1189 ms [main] INFO  Runtime - accepted connection from host bob
1235 ms [main] INFO  ABY - connected ABY to other host at 127.0.0.1:7766
1257 ms [pool-2-thread-1] INFO  Runtime - launching receiver thread for host bob
1262 ms [pool-3-thread-1] INFO  Runtime - launching sender thread for host bob
1268 ms [main] INFO  Interpreter - starting interpretation
1342 ms [main] INFO  ABY - circuit size: 3
Decreasing nthreads from 2 to 1 to fit window size
1555 ms [main] INFO  ABY - executed ABY circuit in 209ms, sent output to SERVER
total gates: 194
total depth: 3
total time: 0.536
total sent/recv: 2596 / 2114
network time: 7.859
setup time: 0.307
setup sent/recv: 1042 / 2082
online time: 0.227
online sent/recv: 1554 / 32

false
1568 ms [main] INFO  Runtime - sent remote message bool from Local(host = alice)@Host(name=alice) to Local(host = bob)@Host(name=bob)
1569 ms [main] INFO  Interpreter - finished interpretation, total running time: 300ms
1570 ms [main] INFO  Runtime - bytes sent to host alice: 4
1570 ms [main] INFO  Runtime - bytes received from host alice: 0
1570 ms [pool-2-thread-1] INFO  Runtime - shutting down receiver thread for host bob
1570 ms [pool-3-thread-1] INFO  Runtime - shutting down sender thread for host bob
1571 ms [main] INFO  Runtime - closing connection to host bob
1572 ms [main] INFO  ViaductBackend - runtime duration: 910ms
```

Note that the Docker image does not have libsnark installed, so you cannot run
compiled examples that use zero-knowledge proof back end. The evaluation
results in the submission can be replicated without this back end.


### 1.2 - Building the compiler

If you wish, you can also build the compiler within the container.

TODO: fill this in



## 2 - Replicating Evaluation Results

To reproduce the evaluation results in the submission, we have provided
scripts to drive the Viaduct compiler and runtime system.

`compilebench.sh` is a Bash script that 

### RQ2 - Scalability of Compilation

### RQ3 - Cost of Compiled Programs

### RQ4 - Annotation Burden of Security Labels

We note that the compilation between erased programs and annotated programs can
differ in trivial ways. For example, in `BettingMillionaires.via`
the compiled program involves Chuck sending a commitment to either Alice or Bob.
It doesn't matter which because Alice and Bob trust each other.


