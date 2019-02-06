# Viaduct - Design Document

## Compiler infrastructure

### Intermediate representations

1. *Surface language* - IMP with information flow types. This represents both
   the functional and security requirements of the program.

2. *Spec-PDG* - A program dependence graph containing information re: the
   security specification. Spec-PDG nodes will at least have information flow
   labels, as well as syntactic checks (e.g. `hasEquality`, `hasMult`) needed
   by protocol constraints during protocol selection.

3. *Protocol-PDG* - A program dependence graph that represents a protocol for
   data and computation. Each node either represents a storage node that
   specifies how a datum is to be stored among hosts or a computation node that
   specifies how a computation is to be executed among hosts.

4. *Via* - A process calculus that represents computation and communication
   among hosts. Cryptographic protocols are represented here abstractly as
   primitives.

###

The compiler has the following phases:

1. *Specification generation* (`Surface` --> `Spec-PDG`). This phase generates a
   security specification from a surface language. Currently the surface
   language is simple, but in the future we plan to support multiple frontend
   languages (e.g. Jif), and the modular design of the Viaduct compiler reflects
   that.

2. *Protocol selection* (`Spec-PDG` --> `Protocol-PDG`). This phase generates
  a protocol for storage and computation nodes from the security specification
  outlined in `Spec-PDG`.
  Protocol optimizations (`Protocol-PDG` --> `Protocol-PDG` passes) are also
  included in this phase.

3. *Abstract implementation generation* (`Protocol-PDG` --> `Via`). This phase
   generates the code for each host in the configuration from the protocols
   specified in `Protocol-PDG`. Protocols (cryptographic or otherwise) here are
   *abstract* in the sense that they are treated as primitives.

4. *Concrete implementation generation* (`Via` --> `?`). This phase takes
  an abstract protocol specified in `Via` and translates it into an 
  actual implementation. For example, we can provide backend translators
  to SGX configurations, or other languages such as ObliVM that support MPC.


## Protocol selection


## Via -- an intermediate language for cryptographic protocols

The `Via` language is a process calculus with cryptographic primitives in
the style of the spi calculus and the applied pi calculus.


