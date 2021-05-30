# Using the Compiler

There are two main compiler commands you need to know.
The `compile` command compiles Viaduct source programs into distributed programs.
The `run` command allows hosts to execute a compiled distributed program together.
We will go over the basics of using these two commands in the following
sections. You can run `viaduct compile --help` and `viaduct run --help` to get
more information about each of these commands and the various options and flags
they support.


## Compiling Source Programs

As an example, we will compile the `examples/Millionaires.via` program,
which is an implementation of the standard Millionaires' problem in Viaduct.
You can view the source program with the following command:

```shell
less examples/Millionaires.via
```

Note that this and other programs in the `examples` directory are purposefully simple;
you can find more complex examples under the `benchmarks` directory.
Run the following to compile the program:

```shell
viaduct -v compile examples/Millionaires.via
```

This will print the compiled program to the standard output.
The `-v` option turns on detailed logging,
and must come before the command name (e.g., `viaduct compile -v` will not work).
You can repeat it (e.g., `-vvv`) for more granular messages, or leave it out.

To save the compiled file to disk, provide the `-o` option along with a file name:

```shell
viaduct -v compile examples/Millionaires.via -o m-out.via
```

You should see logging information that looks like this:

```console
1015 ms [main] INFO  Compile - elaborating source program...
1110 ms [main] INFO  Compile - specializing functions...
1115 ms [main] INFO  Check - name analysis...
1149 ms [main] INFO  Check - type checking...
1159 ms [main] INFO  Check - out parameter initialization analysis...
1165 ms [main] INFO  Check - information flow analysis...
1251 ms [main] INFO  InformationFlowAnalysis - number of label variables: 28
1251 ms [main] INFO  Check - finished information flow analysis, ran for 87ms
1272 ms [main] INFO  Compile - selecting protocols...
1779 ms [main] INFO  Z3Selection - number of symvars: 91
1779 ms [main] INFO  Z3Selection - cost mode set to MINIMIZE
1828 ms [main] INFO  Z3Selection - constraints satisfiable, extracted model
1834 ms [main] INFO  Compile - finished protocol selection, ran for 519ms
1842 ms [main] INFO  Compile - annotating program with protocols...
```

You can view the compiled program:

```shell
less m-out.via
```

Notice that the compiled program is an elaborated version of the source
program where each variable declaration and let binding is annotated with
the protocol that will execute it.


## Running Compiled Programs

The `run` command takes as arguments a host name and a compiled program,
and executes the host's "projection" of the distributed program.
Since compiled programs are distributed, we need to run multiple instances
of Viaduct.
For instance, to execute our example program `m-out.via`,
we need two participants standing in for hosts `alice` and `bob`, respectively.

The easiest way to accomplish this from the single terminal window we have is to
run one of the commands in the background:

```shell
viaduct -v run alice m-out.via -in inputs/alice.txt &
viaduct -v run bob m-out.via -in inputs/bob.txt
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
   viaduct run alice m-out.via -in inputs/alice.txt
   viaduct run bob m-out.via -in inputs/bob.txt
   ```

   We recommend starting Alice's process first; you may get a "connection timed out"
   error otherwise.

5. Quit Tmux by typing `tmux kill-session`.

You can provide input manually for one or both of the participants by omitting
the `-in` option (we also recommend leaving out the `-v` option).
The participant will block on an `Input: ` prompt when you need to provide input.

You can repeat these steps for the other programs in the `examples` and `benchmarks`
directories. However, programs in the `benchmarks` directory expect many
(sometimes hundreds) of inputs, so we don't recommend providing inputs by hand!


## Editing files

You can edit files using the `nano` editor:

```shell
nano examples/Millionaires.via
```
