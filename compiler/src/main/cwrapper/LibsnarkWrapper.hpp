#ifndef _LIBSNARK_WRAPPER
#define _LIBSNARK_WRAPPER

#include <libsnark/zk_proof_systems/ppzksnark/r1cs_ppzksnark/r1cs_ppzksnark.hpp>
#include <jni.h>


extern "C" {


    JNIEXPORT void JNICALL Java_edu_cornell_cs_apl_viaduct_backend_zkp_LibsnarkJNI_sayHello(JNIEnv *, jobject);
}

#endif
