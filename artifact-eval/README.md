# Viaduct - Artifact Evaluation Instructions

This document contains information to evaluate the artifact for the submission
"Viaduct: An Extensible, Optimizing Compiler for Secure Distributed Programs."
It has two parts:
[Getting Started](#getting-started) explains how to get the Docker image running,
and contains the basics of how to use the Viaduct compiler.
[Replicating Evaluation Results](#replicating-evaluation-results)
describes the infrastructure we have provided to replicate the results in
the submission.


## Getting Started

### Running the Docker image

Our artifact is packaged as a Docker image for easy reproducibility.
We have tested running the image on macOS (Big Sur) and Linux (Ubuntu 20.0.1)
with Docker 20.10.2, but it should work on any machine with a relatively recent
version of Docker.

Follow these steps to run the image:

1. [Install Docker](https://docs.docker.com/get-docker/) if you don't already have it.
   You shouldn't need a deep understanding of Docker to follow this guide,
   but [Docker Docs](https://docs.docker.com/get-started/) are a good resource if
   you get stuck or would like to know more about Docker.

2. Download the archive file linked in the submission.
   We will assume it is named `viaduct-docker.tar.gz`.

3. Load the image by running the following command in the same directory as the archive file:

   ```shell
   docker load --input viaduct-docker.tar.gz
   ```

   which will print something similar to:

   ```console
   Loaded image: viaduct:pldi-2021
   ```

   Docker should automatically decompress the file. If your installation of Docker doesn't
   recognize the archive format for whatever reason, you can decompress it manually:
   ```shell
   gzip --decompress --keep viaduct-docker.tar.gz
   # Produces viaduct-docker.tar
   docker load --input viaduct-docker.tar
   ```

4. Run the image as a container:

   ```shell
   docker run --rm -it viaduct:pldi-2021
   ```

This will drop you in a container with a standard (albeit stripped down) Unix shell.


### Using the Viaduct compiler

The container has a built and installed version of the Viaduct compiler.
Running

```shell
viaduct --help
```

will give you the compiler's help text as follows:

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
  run                  Run compiled protocol for a single host
```

There are two main compiler commands you need to know.
The `compile` command compiles Viaduct source programs into distributed programs.
The `run` command allows hosts to execute a compiled distributed program together.
We will go over the basics of using these two commands in the following
sections. You can run `viaduct compile --help` and `viaduct run --help` to get
more information about each of these commands and the various options and flags
they support.


#### Compiling source programs

As an example, we will compile the `HistoricalMillionaires.via` program,
which is in the `benchmarks` folder. You can view the source program
with the following command:

```shell
less benchmarks/HistoricalMillionaires.via
```

Run the following to compile the program:

```shell
viaduct -v compile benchmarks/HistoricalMillionaires.via
```

This will print the compiled program to the standard output.
The `-v` option turns on the verbose mode, which prints logging information.
You can repeat it (e.g., `-vvv`) for more granular messages, or leave it out.

To save the compiled file to disk, run:

```shell
viaduct -v compile benchmarks/HistoricalMillionaires.via -o hm-out.via
```

You should see logging information that looks like this:

```console
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

Viewing `hm-out.via` (using `less` or `cat`), you should see:

```viaduct
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
the protocol that will execute it. As described in the paper, the compiled
distributed program is optimized so that Alice and Bob compute their respective
minima locally, and then use MPC (the `YaoABY` protocol above) to perform the
comparison.


#### Running compiled programs

The `run` command takes as arguments a host name and a compiled program,
and executes the host's "projection" of the distributed program.
Since compiled programs are distributed, we need to run multiple instances
of Viaduct.
For instance, to execute our example program `hm-out.via`,
we need two participants standing in for hosts `alice` and `bob` respectively.

The easiest way to accomplish this from the single terminal window we have is to
run one of the commands in the background:

```shell
viaduct -v run alice hm-out.via -in inputs/alice.txt &
viaduct -v run bob hm-out.via -in inputs/bob.txt
```

Here, we run two instances with logging enabled (the `-v` options),
and provide inputs from files (`-in FILENAME`).

An alternative to running one of the instances in the background is
using [Tmux](https://github.com/tmux/tmux/wiki) and running a participant
on two separate terminal instances.
This method allows you to manually provide input for any and all participants.
However, we only recommend this alternative if you are already familiar with
Tmux (or are willing to pick up the basics on your own).
Here is a very quick tutorial to get you started:

1. Start a new session by typing `tmux`.

2. Split your terminal using the keyboard shortcut `Ctrl+b "`.

3. Switch between panes using `Ctrl+b <arrow key>` (up and down keys specifically).

4. Execute the following two commands in separate panes:
   ```shell
   viaduct run alice hm-out.via -in inputs/alice.txt
   viaduct run bob hm-out.via -in inputs/bob.txt
   ```

5. Quit Tmux with the keyboard shortcut `Ctrl+b d`.

You can provide input manually for one or both of the participant by omitting
the `-in` option (we also recommend leaving out the `-v` option).
The participant will block on an `Input: ` prompt when you need to provide input.
However, note that the default input size for the historical millionaires' game is
100, so it will be tedious to provide input this way.

Note that the Docker image does not have libsnark installed, so you cannot _run_
compiled examples that use the zero-knowledge proof back end
(however, you can still compile them).
The evaluation results in the submission can be replicated without this back end.


### Building the compiler from source

The Viaduct source code is included in the image under the `source` directory.
If you wish, you can build the compiler within the container.
To do so, run the following commands:

```shell
cd source
./gradlew build
```

This will build the compiler and run all unit tests.
Note that the image does not contain third party dependencies to (significantly)
reduce image size, but Gradle will download all dependencies automatically.
Every dependency is version pinned, so you should not run into any issues.
However, you do need an internet connection.

You can now run `./viaduct` in the `source` directory which will
use the binary you just compiled instead of the system wide binary.
Note, however, that the benchmarking scripts we provide will continue to use
the system wide binary.


## Replicating Evaluation Results

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


