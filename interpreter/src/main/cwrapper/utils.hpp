#include <vector>
#include <map>
#include <string>
#include <bitset>
#define CURVE_ALT_BN128
#include <libff/algebra/fields/field_utils.hpp>
#include <libsnark/zk_proof_systems/ppzksnark/r1cs_ppzksnark/r1cs_ppzksnark.hpp>
#include <libsnark/common/default_types/r1cs_ppzksnark_pp.hpp>
#include <libsnark/gadgetlib1/pb_variable.hpp>
#include <libsnark/gadgetlib1/gadgets/hashes/hash_io.hpp>
#include <libsnark/gadgetlib1/gadgets/hashes/sha256/sha256_gadget.hpp>
#include <libsnark/gadgetlib1/gadgets/basic_gadgets.hpp>

// https://stackoverflow.com/questions/41456692/convert-stdvectorbool-to-stdstring
std::size_t divide_rounding_up( std::size_t dividend, std::size_t divisor )
    { return ( dividend + divisor - 1 ) / divisor; }

std::string bvec_to_string( std::vector< bool > const & bitvector ) {
    std::string ret( divide_rounding_up( bitvector.size(), 8 ), 0 );
    auto out = ret.begin();
    int shift = 0;

    for ( bool bit : bitvector ) {
        * out |= bit << shift;

        if ( ++ shift == 8 ) {
            ++ out;
            shift = 0;
        }
    }
    return ret;
}

std::vector<bool> string_to_bvec(std::string data) {
            std::vector<bool> bits;
            for (int i = 0; i < data.size(); i++) {
                for (int j = 0; j < 8; j++)
                    bits.push_back((data[i] >> j) & 1);
            }
            return bits;
}



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

typedef libff::Fr<libsnark::default_r1cs_ppzksnark_pp> field128;
typedef libsnark::protoboard<field128> protoboard;
typedef libsnark::pb_variable<field128> pb_variable;
typedef libsnark::pb_variable_array<field128> pb_variable_array;
typedef libsnark::digest_variable<field128> digest_variable;

typedef libsnark::linear_combination<field128> lincomb;
typedef libsnark::linear_term<field128> linterm;
typedef libsnark::variable<field128> var;
typedef libsnark::r1cs_constraint<field128> constraint;


bool ensure_satisfied(libsnark::r1cs_constraint_system<libff::Fr<libsnark::default_r1cs_ppzksnark_pp> > CS,
    std::vector<field128> primary_input,
    std::vector<field128> auxiliary_input) {

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
            assert (false);
            return false;
        }
    }

    return true;
}

bool ensure_satisfied (const protoboard &pb) {
    ensure_satisfied(pb.get_constraint_system(), pb.primary_input(), pb.auxiliary_input());
}

struct Var {
    pb_variable value;
};

struct VarArray {
    pb_variable_array values;
};

void addEquality(protoboard &pb, Var a, Var b) {
    pb.add_r1cs_constraint(constraint(1, a.value, b.value));
}

void addEquality(protoboard &pb, pb_variable_array a, pb_variable_array b) {
    assert (a.size() == b.size());
    for (int i = 0; i < a.size(); i++) {
        pb.add_r1cs_constraint(constraint(1, a[i], b[i]));
    }
}

struct ShaResult {
    pb_variable val_var;
    pb_variable_array data;
    pb_variable_array nonce;
    pb_variable_array output;
};

ShaResult mkSHA(protoboard &pb, long long v, std::string nonce, bool isProver) {
    if (isProver) {
        assert (pb.is_satisfied()); // Make sure protoboard is good
    }

    pb_variable x;
    x.allocate(pb);
    if (isProver) {
        pb.val(x) = v;
    }


    digest_variable data(pb, libsnark::SHA256_digest_size, "");
    auto pg = libsnark::packing_gadget<field128>(pb, data.bits, x);
    pg.generate_r1cs_constraints(true);
    if (isProver) {
        pg.generate_r1cs_witness_from_packed();
    }

    auto nonce_bv = string_to_bvec(nonce);
    assert (nonce_bv.size() == libsnark::SHA256_digest_size);
    digest_variable nonce_digest(pb, libsnark::SHA256_digest_size, "");
    nonce_digest.generate_r1cs_constraints();
    if (isProver) {
        nonce_digest.generate_r1cs_witness(nonce_bv);
    }

    digest_variable output_digest(pb, libsnark::SHA256_digest_size, "");

    auto g = libsnark::sha256_two_to_one_hash_gadget<field128>(
        pb,
        data,
        nonce_digest,
        output_digest,
        "");
    g.generate_r1cs_constraints();

    if (isProver) {
        g.generate_r1cs_witness();
        assert(pb.is_satisfied());
    }

    return {x, data.bits, nonce_digest.bits, output_digest.bits};
}
