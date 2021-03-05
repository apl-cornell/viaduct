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

The artifact is packaged as a Docker image for easy reproducibility.
We have tested running the image on macOS (Big Sur) and Linux (Ubuntu 20.0.1)
with Docker 20.10.2, but it should work on any machine with a relatively recent
version of Docker.

Follow these steps to run the image:

1. [Install Docker](https://docs.docker.com/get-docker/) if you don't already have it.
   You shouldn't need a deep understanding of Docker to follow this guide,
   but [Docker Docs](https://docs.docker.com/get-started/) are a good resource if
   you get stuck or would like to learn more about Docker.

2. Download the archive file linked in the submission.
   We will assume it is named `viaduct-docker.tar.gz`.

3. Load the image by running the following command in the same directory as the archive file:

   ```shell
   docker load --input viaduct-docker.tar.gz
   ```

   which should print:

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

The container comes with the Viaduct compiler already installed.
Running

```shell
viaduct --help
```

will give you the compiler's help text, which should look as follows:

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
Note that this option needs to come before the command name,
so `viaduct compile -v` will not work.

To save the compiled file to disk, provide the `-o` option along with a file name:

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
program where each variable declaration and let binding is annotated with
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
we need two participants standing in for hosts `alice` and `bob`, respectively.

The easiest way to accomplish this from the single terminal window we have is to
run one of the commands in the background:

```shell
viaduct -v run alice hm-out.via -in inputs/alice.txt &
viaduct -v run bob hm-out.via -in inputs/bob.txt
```

Here, we run two instances with logging enabled (the `-v` option),
and provide inputs from files (`-in FILENAME`).

An alternative to running one of the instances in the background is
using [Tmux](https://github.com/tmux/tmux/wiki) and running each participant
in a different pane.
This method allows you to manually provide input to each participant.
However, we only recommend this alternative if you are already familiar with
Tmux (or are willing to pick up the basics).
Here is a very quick tutorial to get you started:

1. Start a new session by typing `tmux`.

2. Split your terminal using the keyboard shortcut `Ctrl+b "`.

3. Switch between panes using `Ctrl+b <arrow key>` (up and down keys specifically).

4. Execute the following two commands in separate panes:
   ```shell
   viaduct run alice hm-out.via -in inputs/alice.txt
   viaduct run bob hm-out.via -in inputs/bob.txt
   ```

5. Quit Tmux by typing `tmux kill-session`.

You can provide input manually for one or both of the participant by omitting
the `-in` option (we also recommend leaving out the `-v` option).
The participant will block on an `Input: ` prompt when you need to provide input.
However, note that the default input size for the historical millionaires' game is
100, so it will be tedious to provide input this way.

#### Limitations

The Docker image does not have libsnark installed, so you cannot _run_
compiled examples that use the zero-knowledge proof back end
(though you can still compile them).
The evaluation results in the submission can be replicated without this back end.


### Building the compiler from source

The Viaduct source code is included in the image under the `source` directory.
If you wish, you can build the compiler within the container.
To do so, run the following commands:

```shell
cd source
./gradlew build
```

This will build the compiler and run all unit tests using the Gradle build
tool. Gradle provides many other commands; refer to the
[documentation](https://docs.gradle.org/6.8.2/userguide/command_line_interface.html)
for details.

Note that the Docker image does not include third party dependencies to
(significantly) reduce image size, but Gradle will download all
dependencies automatically.
Every dependency is version pinned, so you should not run into any issues.
However, you do need an internet connection.

You can now run `./viaduct` in the `source` directory to invoke the binary
you just compiled instead of the system wide binary.
Note, however, that the benchmarking scripts we provide will continue to use
the system wide binary.


## Replicating Evaluation Results

To reproduce the evaluation results in the submission (Section 7), we have provided
a `benchmark.py` script to drive the Viaduct compiler and runtime system.
Certain benchmarks may require some additional setup depending on your system,
which is detailed below.


### Before you start

#### Increasing memory available to Docker

Benchmarks for RQ1, RQ2, and RQ4 should run with the default Docker settings,
since these benchmarks only compile programs and compilation does not require
a lot of memory.
However, RQ3 executes programs using an MPC backend, and some programs
require significant amounts of memory.

There is no memory limit for Docker containers on Linux by default,
but macOS and Windows set a 2 GB memory limit for Docker containers by default.
Follow the steps outlined in the provided links and allocate at least 10 GB
of memory to Docker:

- macOS: https://docs.docker.com/docker-for-mac/#resources
- Windows: https://docs.docker.com/docker-for-windows/#resources

Note that this limit is for all Dockers containers running on your machine
combined. If you are running other containers, adjust the limit accordingly.

Changing these settings will kill all running containers;
you will have to start the container again.


#### Enabling network admin privileges (Optional)

If you would like to simulate our wide area network (WAN) settings when running
the RQ3 benchmarks, you need to start the Docker container with network
admin privileges:

```shell
docker run --rm -it --cap-add NET_ADMIN viaduct:pldi-2021
```

By default, the benchmarks simulate a local area network (LAN).


### RQ1 - Scalability of Compilation

Replicating the "result" for this is simple: peruse the example programs in the
`benchmarks` folder and see if you are persuaded that the Viaduct source
language is expressive. That's it!


### RQ2 - Scalability of Compilation

To replicate the result for this research question, run the script as follows:

```shell
./benchmark.py rq2
```

The script will build benchmarks (with the cost model optimized for LAN)
and save compilation information into a report file, which lives in
`build/rq2/report.csv`.
The compiled programs are placed under `build/rq2/lan/`.
The script will print the commands it is running,
and will display the final report, like so:

```console
make BUILD_DIR=build/rq2 clean lan
rm -rf build/rq2/lan
rm -rf build/rq2/wan
../viaduct -v compile benchmarks/Battleship.via -o build/rq2/lan/Battleship.via
../viaduct -v compile benchmarks/BettingMillionaires.via -o build/rq2/lan/BettingMillionaires.via
../viaduct -v compile benchmarks/Biomatch.via -o build/rq2/lan/Biomatch.via
../viaduct -v compile benchmarks/GuessingGame.via -o build/rq2/lan/GuessingGame.via
../viaduct -v compile benchmarks/HhiScore.via -o build/rq2/lan/HhiScore.via
../viaduct -v compile benchmarks/HistoricalMillionaires.via -o build/rq2/lan/HistoricalMillionaires.via
../viaduct -v compile benchmarks/Interval.via -o build/rq2/lan/Interval.via
../viaduct -v compile benchmarks/Kmeans.via -o build/rq2/lan/Kmeans.via
../viaduct -v compile benchmarks/KmeansUnrolled.via -o build/rq2/lan/KmeansUnrolled.via
../viaduct -v compile benchmarks/Median.via -o build/rq2/lan/Median.via
../viaduct -v compile benchmarks/Rochambeau.via -o build/rq2/lan/Rochambeau.via
../viaduct -v compile benchmarks/TwoRoundBidding.via -o build/rq2/lan/TwoRoundBidding.via
Benchmark,Information Flow Variables,Information Flow Time (ms),Selection Variables,Selection Time (ms)
Battleship,323,135,1022,1154
BettingMillionaires,102,83,387,2072
Biomatch,248,108,708,2378
GuessingGame,59,53,193,411
HhiScore,89,62,285,1165
HistoricalMillionaires,54,44,187,712
Interval,170,95,660,4662
Kmeans,550,192,1684,10157
KmeansUnrolled,1219,401,3629,34031
Median,117,71,386,1199
Rochambeau,254,95,741,878
TwoRoundBidding,187,68,575,1766
Report written to build/rq2/report.csv
```


### RQ3 - Cost of Compiled Programs

To replicate the result for this research question, run the following command:

```shell
./benchmark.py rq3
```

The command will compile two versions of the MPC benchmarks
(Biomatch, HhiScore, HistoricalMillionaires, Kmeans, Median, TwoRoundBidding),
one optimized for the LAN setting and another optimized for the WAN setting.
The command will then run four versions of each benchmark over the same inputs:
the compiled LAN and WAN versions, as well as hand-written Bool and Yao versions
that use Boolean and Yao circuits respectively.

The script takes an optional argument `-i` which specifies the number of times
each benchmark should be executed. This is set to 1 by default to reduce the time
it takes to run the benchmarks. Results in the paper use 5:

```shell
./benchmark.py rq3 -i 5
```

The script will output a CSV report that has the following form:

```csv
Benchmark,Variant,Iteration,Host,Running Time (s),Communication (MB)
Biomatch,BOOL,1,alice,5.866,53.41919994354248
Biomatch,BOOL,1,bob,5.868,53.41919136047363
Biomatch,YAO,1,alice,3.461,49.89405059814453
...
```

The report lists each host and iteration separately;
to get the numbers in the paper, group by these columns and take the mean.


#### Simulating wide area networks

We have provided a `settraffic` script that uses the
[tc](https://linux.die.net/man/8/tc) utility to simulate a WAN environment.
The script artificially creates a bandwidth limit and latency on the loopback
device (`lo`). It is simple to use: run

```shell
./scripts/settraffic wan
```

to set a WAN environment (100 Mbps bandwidth and 50 ms latency), or

```shell
./scripts/settraffic lan
```

to set a LAN environment (no limits; reverts to default settings).

Note that running the benchmarks takes quite a bit of time. This is
particularly true for running the benchmarks in the WAN setting, which can
take several hours.


### RQ4 - Annotation Burden of Security Labels

To replicate the result for this research question, run the following command:

```shell
./benchmark.py rq4
```

The command builds the annotated and erased versions of benchmarks
(with the cost model optimized for LAN) and takes the diffs between the compiled
programs, and then saves these into a report file, which lives in
`build/rq4/report.txt`. The compiled programs are in `build/rq4/lan/`.

For the most part, the diffs should be between lines that are exactly the same
except for the label annotations, which the compiler preserves in compiled
programs. We do note that the compilation between erased programs and annotated
programs can differ in trivial ways. For example, in `BettingMillionaires.via`
the compiled program involves Chuck sending a commitment to either Alice or
Bob. It doesn't matter which because Alice and Bob trust each other. Also, our
cost model currently treats Local and Replication protocols to have the same
execution cost, so there are some lines in one version of the compiled program
executed in a Local protocol and the corresponding lines in the other version
executed in a Replication protocol. This is not due to the erasure of label
annotations but rather due to Z3 returning different models with the same
minimal cost.
