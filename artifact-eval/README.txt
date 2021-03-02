# Viaduct - Artifact Evaluation Instructions

## Getting Started

We provide the Viaduct artifact through a Docker image.


## Step-by-Step Instructions

To reproduce the evaluation results in the submission, we have provided
scripts that will drive the Viaduct compiler and runtime system:

`compilebench.sh` is a Bash script that 

### RQ2 - Scalability of Compilation

### RQ3 - Cost of Compiled Programs

### RQ4 - Annotation Burden of Security Labels

We note that the compilation between erased programs and annotated programs can
differ in trivial ways. For example, in `BettingMillionaires.via`
the compiled program involves Chuck sending a commitment to either Alice or Bob.
It doesn't matter which because Alice and Bob trust each other.


