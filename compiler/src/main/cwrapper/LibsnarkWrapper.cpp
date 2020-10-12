#include <stdio.h>
#include "LibsnarkWrapper.hpp"

JNIEXPORT void JNICALL Java_edu_cornell_cs_apl_viaduct_backend_zkp_LibsnarkJNI_sayHello(JNIEnv * env,
    jobject obj) {

    printf("Say hello from native");

}
