# Viaduct

[![Build Status](https://github.com/apl-cornell/viaduct/workflows/CI/badge.svg)](https://github.com/apl-cornell/viaduct/actions?query=workflow%3ACI)
[![Code Coverage](https://codecov.io/gh/apl-cornell/viaduct/branch/master/graph/badge.svg)](https://codecov.io/gh/apl-cornell/viaduct)
[![Docker Build Status](https://img.shields.io/docker/cloud/build/aplcornell/viaduct)](https://hub.docker.com/repository/docker/aplcornell/viaduct)

Viaduct is an extensible, optimizing compiler that automatically employs
cryptography to enforce high-level security specifications.
Viaduct can generate code that uses
- secure multiparty computation ([ABY](https://github.com/encryptogroup/ABY)),
- zero-knowledge proofs ([libsnark](https://github.com/scipr-lab/libsnark)),
- commitments,
- replication.

Checkout the guide to [get started](https://viaduct-lang.org)!

## References

\[ARGMS21]
Coşku Acay, Rolph Recto, Joshua Gancher, Andrew C. Myers, Elaine Shi.
[Viaduct: An Extensible, Optimizing Compiler for Secure Distributed Programs](https://eprint.iacr.org/2021/468).
In ACM SIGPLAN Conference on Programming Language Design and Implementation (PLDI '21).
