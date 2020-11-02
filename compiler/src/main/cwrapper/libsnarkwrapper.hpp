
#include <vector>
#include <string>
#define CURVE_ALT_BN128
#include <libff/common/profiling.hpp>
#include <libff/common/utils.hpp>
#include <libsnark/common/default_types/r1cs_ppzksnark_pp.hpp>
#include <libsnark/zk_proof_systems/ppzksnark/r1cs_ppzksnark/r1cs_ppzksnark.hpp>
#include <iostream>


typedef libff::Fr<libsnark::default_r1cs_ppzksnark_pp> field128;
typedef libsnark::linear_combination<field128> lincomb;
typedef libsnark::linear_term<field128> linterm;
typedef libsnark::variable<field128> var;

struct Term {
    int coeff = 1;
    int wireID = 0;
};

struct LinComb {
    int constTerm = 0;
    std::vector<Term> linTerms;

    lincomb to_lincomb() {
        std::vector<linterm > terms;
        terms.push_back(linterm(var(0), constTerm));
        for (int i = 0; i < linTerms.size(); i++) {
            terms.push_back(linterm(var(linTerms[i].wireID + 1), linTerms[i].coeff));
        }
        return terms;
    }

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
};

struct WireInfo {
    WireType type; // should be an enum
    int input_val = 0;
};

// Basically just a wrapper around std::string for binary data
struct ByteBuf {
    std::string contents;
    const signed char* get_data(size_t *len) {
        *len = contents.size();
        return (signed char*) contents.data();
    }
};

ByteBuf mkByteBuf(const char data[], size_t len) {
    return {std::string(data, len)};
}

struct Keypair {
    ByteBuf proving_key;
    ByteBuf verification_key;
};

void initZKP() {
    libsnark::default_r1cs_ppzksnark_pp::init_public_params();
}

void dump_constraint_system(libsnark::r1cs_constraint_system<libff::Fr<libsnark::default_r1cs_ppzksnark_pp> > CS) {
    std::cout<<" Constraint system size: " << CS.constraints.size() << "\n";
    for (size_t c = 0; c < CS.constraints.size(); ++c) {
            std::cout<< "Constraint " << c << ":\n";
            printf("terms for a:\n"); CS.constraints[c].a.print();
            printf("terms for b:\n"); CS.constraints[c].b.print();
            printf("terms for c:\n"); CS.constraints[c].c.print();
    }
}

void dump_inputs(std::vector<field128> primary_input, std::vector<field128> aux_input) {
    std::cout<<"Primary: \n";
    for (int i = 0; i < primary_input.size(); ++i)
        std::cout<<"x_" << i + 1 << " = " << primary_input[i] << "\n";

    std::cout<<"\nAux: \n";
    for (int i = 0; i < aux_input.size(); ++i)
        std::cout<<"x_" << i + primary_input.size() + 1 << " = " << aux_input[i] << "\n";

}

bool ensure_satisfied(libsnark::r1cs_constraint_system<libff::Fr<libsnark::default_r1cs_ppzksnark_pp> > CS,
    std::vector<field128> primary_input,
    std::vector<field128> auxiliary_input ) {

    assert(primary_input.size() == CS.num_inputs());
    assert(primary_input.size() + auxiliary_input.size() == CS.num_variables());

    libsnark::r1cs_variable_assignment<field128> full_variable_assignment = primary_input;
    full_variable_assignment.insert(full_variable_assignment.end(), auxiliary_input.begin(), auxiliary_input.end());

    for (size_t c = 0; c < CS.constraints.size(); ++c)
    {
        const field128 ares = CS.constraints[c].a.evaluate(full_variable_assignment);
        const field128 bres = CS.constraints[c].b.evaluate(full_variable_assignment);
        const field128 cres = CS.constraints[c].c.evaluate(full_variable_assignment);

        if (!(ares*bres == cres))
        {
            printf("constraint %zu unsatisfied\n", c);
            printf("<a,(1,x)> = "); ares.print();
            printf("<b,(1,x)> = "); bres.print();
            printf("<c,(1,x)> = "); cres.print();
            printf("constraint was:\n");
            printf("terms for a:\n"); CS.constraints[c].a.print_with_assignment(full_variable_assignment);
            printf("terms for b:\n"); CS.constraints[c].b.print_with_assignment(full_variable_assignment);
            printf("terms for c:\n"); CS.constraints[c].c.print_with_assignment(full_variable_assignment);
            dump_inputs(primary_input, auxiliary_input);
            assert (false);
            return false;
        }
    }

    return true;
}

struct R1CS {
    std::vector<WireInfo> wires;
    std::vector<Constraint> constraints;

    // Derived notions
    std::vector<field128> primary_input;
    std::vector<field128> aux_input;

    // Constraint system
    libsnark::r1cs_constraint_system<libff::Fr<libsnark::default_r1cs_ppzksnark_pp> > CS =
        libsnark::r1cs_constraint_system<libff::Fr<libsnark::default_r1cs_ppzksnark_pp> >();
    bool initialized = false;

    int mkWire(WireInfo info) {
        wires.push_back(info);
        if (info.type == WIRE_PUBLIC_IN) {
            primary_input.push_back(info.input_val);
            CS.primary_input_size++;
        }
        if (info.type == WIRE_IN) {
            aux_input.push_back(info.input_val);
            CS.auxiliary_input_size++;
        }
        if (info.type == WIRE_DUMMY_IN) {
            CS.auxiliary_input_size++;
        }
        return wires.size() - 1;
    }

    void addConstraint(Constraint c) {
        constraints.push_back(c);
    }

    void reportConstraintSystem(libsnark::r1cs_constraint_system<libff::Fr<libsnark::default_r1cs_ppzksnark_pp> > cs) {
            std::cout<<"constraint system primary inputs = " << cs.primary_input_size << "\n";
            std::cout<<"constraint system aux inputs = " << cs.auxiliary_input_size << "\n";
            std::cout<<"constraint system constraint size = " << cs.num_constraints() << "\n";
    }

    // Generate proof. Input: Proving key.
    ByteBuf generateProof(ByteBuf provingKey) {

        libsnark::r1cs_ppzksnark_proving_key<libsnark::default_r1cs_ppzksnark_pp> pk;
        std::stringstream pk_stream(provingKey.contents);
        pk_stream >> pk;

        if (pk.constraint_system.primary_input_size != primary_input.size()) {
            reportConstraintSystem(pk.constraint_system);
            assert(false);
        }


        std::cout<<"pk constraint system: " << "\n";
        dump_constraint_system(pk.constraint_system);
        ensure_satisfied(pk.constraint_system, primary_input, aux_input);

        std::stringstream pf_stream;
        pf_stream << libsnark::r1cs_ppzksnark_prover(pk, primary_input, aux_input);

        return {pf_stream.str()};
    }

    // Requires that wires are only dummy_in, public, or internal
    bool verifyProof(ByteBuf verificationKey, ByteBuf proof) {

        // Deserialize the verification key
        libsnark::r1cs_ppzksnark_verification_key<libsnark::default_r1cs_ppzksnark_pp> vk;
        std::stringstream vk_stream(verificationKey.contents);
        vk_stream >> vk;

        // Deserialize proof
        libsnark::r1cs_ppzksnark_proof<libsnark::default_r1cs_ppzksnark_pp> pf;
        std::stringstream pf_stream(proof.contents);
        pf_stream >> pf;

        return libsnark::r1cs_ppzksnark_verifier_strong_IC(vk, primary_input, pf);
    }

    void initConstraintSystem() {
        if (initialized) {
            return;
        }
        else {
            // Get input sizes
            size_t num_primary_inputs = 0;
            size_t num_aux_inputs = 0;
            for (int i = 0; i < wires.size(); i++) {
                if ((wires[i].type == WIRE_IN) || (wires[i].type == WIRE_DUMMY_IN)) {
                    num_aux_inputs++;
                }
                if (wires[i].type == WIRE_PUBLIC_IN) {
                    num_primary_inputs++;
                }
            }
            CS.primary_input_size = num_primary_inputs;
            CS.auxiliary_input_size = num_aux_inputs;
            for (int i = 0; i < constraints.size(); i++) {
                // Add constraint to CS
                auto lhs_lc = constraints[i].lhs.to_lincomb();
                auto rhs_lc = constraints[i].rhs.to_lincomb();
                auto eq_lc = constraints[i].eq.to_lincomb();
                auto constraint = libsnark::r1cs_constraint<libff::Fr<libsnark::default_r1cs_ppzksnark_pp> >();
                constraint.a = lhs_lc;
                constraint.b = rhs_lc;
                constraint.c = eq_lc;
                CS.add_constraint(constraint);
            }
            initialized = true;
            std::cout<<"Initialized constraint system with primary_input_size=" << num_primary_inputs
            << ", aux input size=" << num_aux_inputs << " and num constraints = " << constraints.size() << "\n";
        }
    }


    // Requires that wires are only in, public, or internal
    Keypair genKeypair() {
        initConstraintSystem();
        reportConstraintSystem(CS);
        dump_constraint_system(CS);
        ensure_satisfied(CS, primary_input, aux_input);
        auto kp = libsnark::r1cs_ppzksnark_generator<libsnark::default_r1cs_ppzksnark_pp>(CS);
        std::stringstream kp_pk, kp_vk;
        kp_pk << kp.pk;
        kp_vk << kp.vk;
        return { { kp_pk.str() }, { kp_vk.str() } };
    }
};





