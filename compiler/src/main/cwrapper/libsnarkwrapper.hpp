
#include <vector>
#include <string>

struct Term {
    int coeff = 1;
    int wireID = 0;
};

struct LinComb {
    int constTerm = 0;
    std::vector<Term> linTerms;
};

struct Constraint {
    LinComb lhs;
    LinComb rhs;
    LinComb eq;
};


enum WireType {
    WIRE_IN,
    WIRE_DUMMY_IN,
    WIRE_PUBLIC_IN,
    WIRE_INTERNAL
};

struct WireInfo {
    WireType type; // should be an enum
    int input_val = 0;
};

struct Keypair {
    std::string proving_key;
    std::string verification_key;
};

long provingKeySize() {
    return 6;
}

long verificationKeySize() {
    return 6;
}

long proofSize() {
    return 6;
}

struct R1CS {
    std::vector<WireInfo> wires;
    std::vector<Constraint> constraints;

    int mkWire(WireInfo info) {
        wires.push_back(info);
        return wires.size() - 1;
    }

    void addConstraint(Constraint c) {
        constraints.push_back(c);
    }

    // Requires that wires are only in, public, or internal
    std::string generateProof(std::string provingKey) {
        return std::string("pf");
    }

    // Requires that wires are only dummy_in, public, or internal
    int verifyProof(std::string verificationKey, std::string proof) {
        return 1;
    }

    // Requires that wires are only in, public, or internal
    Keypair genKeypair() {
        return Keypair({"hello", "there"});
    }
};

/*
int mkWire(R1CS *r1cs, WireInfo info) {
    r1cs->wires.push_back(info);
    return r1cs->wires.size() - 1;
}

void addConstraint(R1CS *r1cs, Constraint c) {
    r1cs->constraints.push_back(c);
}
*/





