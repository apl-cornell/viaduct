#include "utils.hpp"


struct Keypair {
    ByteBuf proving_key;
    ByteBuf verification_key;
};

void initZKP() {
    libsnark::default_r1cs_ppzksnark_pp::init_public_params();
}

ByteBuf get_sha_nonce_val (ByteBuf nonce, long long val) {
    protoboard pb;
    auto res = mkSHA(pb, val, nonce.contents, true);
    auto hash_str = bvec_to_string(res.output.get_bits(pb));
    return {hash_str};
}

class R1CSInstance {
    private:
        protoboard pb;

        bool inserting_public = true;
        int num_public = 0;

    public:
        R1CSInstance() {
        }
        bool isProver = false;

        Var mkPublicVal(long long val) {
            assert(inserting_public);
            pb_variable x;
            x.allocate(pb);
            pb.val(x) = val;
            num_public++;
            return {x};
        }

        VarArray mkPublicBitvec(std::vector<bool> bits) {
            assert(inserting_public);
            pb_variable_array out;
            out.allocate(pb, bits.size());
            out.fill_with_bits(pb, bits);
            num_public += bits.size();
            return {out};
        }

        VarArray mkPublicBitvec(ByteBuf buf) {
            return mkPublicBitvec(string_to_bvec(buf.contents));
        }

        Var mkPrivateValProver(long long val, VarArray hash, VarArray nonce) {
            assert(isProver);
            assert(nonce.values.size() == libsnark::SHA256_digest_size);
            inserting_public = false;

            auto res = mkSHA(pb, val, bvec_to_string(nonce.values.get_bits(pb)), true);
            addEquality(pb, res.output, hash.values);
            return {res.val_var};
        }

        Var mkPrivateValVerifier(VarArray hash, VarArray nonce) {
            assert(!isProver);
            assert(nonce.values.size() == libsnark::SHA256_digest_size);
            inserting_public = false;

            auto res = mkSHA(pb, 0, bvec_to_string(nonce.values.get_bits(pb)), false);
            addEquality(pb, res.output, hash.values);
            return {res.val_var};
        }

        Var mkAnd(Var lhs, Var rhs) {
            inserting_public = false;
            libsnark::generate_boolean_r1cs_constraint<field128>(pb, lhs.value);
            libsnark::generate_boolean_r1cs_constraint<field128>(pb, rhs.value);
            pb_variable out;
            out.allocate(pb);
            pb.add_r1cs_constraint(constraint(lhs.value, rhs.value, out));
            if (isProver) {
                pb.val(out) = pb.val(lhs.value) * pb.val(rhs.value);
            }
            return {out};
        }

        Var mkNot(Var v) {
            inserting_public = false;
            libsnark::generate_boolean_r1cs_constraint<field128>(pb, v.value);
            pb_variable out;
            out.allocate(pb);
            pb.add_r1cs_constraint(constraint(1, 1-v.value, out));
            if (isProver) {
                pb.val(out) = field128::one() - pb.val(v.value);
            }
            return {out};
        }

        Var mkOr(Var lhs, Var rhs) {
            return mkNot(mkAnd(mkNot(lhs), mkNot(rhs)));
        }

        Var mkMult(Var lhs, Var rhs) {
            inserting_public = false;
            pb_variable out;
            out.allocate(pb);
            pb.add_r1cs_constraint(constraint(lhs.value, rhs.value, out));
            if (isProver) {
                pb.val(out) = pb.val(lhs.value) * pb.val(rhs.value);
            }
            return {out};
        }

        Var mkAdd(Var lhs, Var rhs) {
            inserting_public = false;
            pb_variable out;
            out.allocate(pb);
            pb.add_r1cs_constraint(constraint(1, lhs.value + rhs.value, out));
            if (isProver) {
                pb.val(out) = pb.val(lhs.value) + pb.val(rhs.value);
            }
            return {out};
        }

        Var mkMux(Var b, Var lhs, Var rhs) {
            libsnark::generate_boolean_r1cs_constraint<field128>(pb, b.value);

            pb_variable out1;
            out1.allocate(pb);
            pb.add_r1cs_constraint(constraint(b.value, lhs.value, out1));

            if (isProver) {
                pb.val(out1) = pb.val(b.value) * pb.val(lhs.value);
            }

            pb_variable out2;
            out2.allocate(pb);
            pb.add_r1cs_constraint(constraint(1- b.value, rhs.value, out1));

            if (isProver) {
                pb.val(out2) = (field128::one() - pb.val(b.value)) * pb.val(rhs.value);
            }

            return mkAdd({out1}, {out2});
        }

        Var mkEqualTo(Var a, Var b) {
            pb_variable less, less_or_eq;
            less.allocate(pb);
            less_or_eq.allocate(pb);
            auto cg = libsnark::comparison_gadget<field128>(pb, 32, a.value, b.value, less, less_or_eq);
            cg.generate_r1cs_constraints();
            if (isProver) {
                cg.generate_r1cs_witness();
            }

            return mkAnd(mkNot({less}), {less_or_eq});
        }

        Var mkLessThan(Var a, Var b) {
            pb_variable less, less_or_eq;
            less.allocate(pb);
            less_or_eq.allocate(pb);
            auto cg = libsnark::comparison_gadget<field128>(pb, 32, a.value, b.value, less, less_or_eq);
            cg.generate_r1cs_constraints();
            if (isProver) {
                cg.generate_r1cs_witness();
            }
            return {less};
        }

        Var mkLE(Var a, Var b) {
            pb_variable less, less_or_eq;
            less.allocate(pb);
            less_or_eq.allocate(pb);
            auto cg = libsnark::comparison_gadget<field128>(pb, 32, a.value, b.value, less, less_or_eq);
            cg.generate_r1cs_constraints();
            if (isProver) {
                cg.generate_r1cs_witness();
            }
            return {less_or_eq};
        }

        void AddEquality(Var a, Var b) {
            addEquality(pb, a, b);
        }

        // Requires that wires are only dummy_in, public, or internal
        bool verifyProof(ByteBuf verificationKey, ByteBuf proof) {

            libff::inhibit_profiling_info = true;
            libff::inhibit_profiling_counters = true;
            pb.set_input_sizes(num_public);

            // Deserialize the verification key
            libsnark::r1cs_ppzksnark_verification_key<libsnark::default_r1cs_ppzksnark_pp> vk;
            std::stringstream vk_stream(verificationKey.contents);
            vk_stream >> vk;

            // Deserialize proof
            libsnark::r1cs_ppzksnark_proof<libsnark::default_r1cs_ppzksnark_pp> pf;
            std::stringstream pf_stream(proof.contents);
            pf_stream >> pf;

            return libsnark::r1cs_ppzksnark_verifier_strong_IC(vk, pb.primary_input(), pf);
        }

        // Generate proof. Input: Proving key.
        ByteBuf generateProof(ByteBuf provingKey) {
            assert (isProver);
            pb.set_input_sizes(num_public);
            assert (ensure_satisfied(pb));

            libff::inhibit_profiling_info = true;
            libff::inhibit_profiling_counters = true;

            libsnark::r1cs_ppzksnark_proving_key<libsnark::default_r1cs_ppzksnark_pp> pk;
            std::stringstream pk_stream(provingKey.contents);
            pk_stream >> pk;

            std::stringstream pf_stream;
            pf_stream << libsnark::r1cs_ppzksnark_prover<libsnark::default_r1cs_ppzksnark_pp>(pk, pb.primary_input(), pb.auxiliary_input());
            return {pf_stream.str()};
        }

        Keypair genKeypair() {
            assert (isProver);
            pb.set_input_sizes(num_public);
            assert (ensure_satisfied(pb));

            libff::inhibit_profiling_info = true;
            libff::inhibit_profiling_counters = true;

            auto cs = pb.get_constraint_system();
            auto kp = libsnark::r1cs_ppzksnark_generator<libsnark::default_r1cs_ppzksnark_pp>(cs);

            std::stringstream kp_pk, kp_vk;
            kp_pk << kp.pk;
            kp_vk << kp.vk;
            return { { kp_pk.str() }, { kp_vk.str() } };
        }

};
