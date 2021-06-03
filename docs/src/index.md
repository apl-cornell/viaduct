# Viaduct

Viaduct is an extensible, optimizing compiler for secure distributed programs.
It lets developers write programs in a simple sequential language that
embeds high-level security policies using information flow labels.
Viaduct then compiles the source program into a distributed implementation that
employs cryptography to defend the security policy.

Viaduct is *extensible*: it provides a set of well-defined interfaces that
developers can implement to add support for new cryptographic mechanisms.
Because of its novel design, Viaduct does not place limitations
on the source programs it can compile, such as the number of participants
or trust assumptions between participants.
Viaduct uses a cost model to generate efficient distributed programs,
avoiding the use of expensive cryptography unless necessary.

Our prototype compiler and runtime, written in Kotlin, can be found
[on Github][repo].
It currently supports the following cryptographic mechanisms:

* multiparty computation (via [ABY][aby])
* zero-knowledge proofs (via [libsnark][libsnark])
* commitments (SHA256 hash + nonce)

[aby]: https://github.com/encryptogroup/ABY
[libsnark]: https://github.com/scipr-lab/libsnark
[repo]: https://github.com/apl-cornell/viaduct

More details about the compiler can be found in our
[PLDI 2021 paper][pldi-2021].

[pldi-2021]: https://eprint.iacr.org/2021/468
