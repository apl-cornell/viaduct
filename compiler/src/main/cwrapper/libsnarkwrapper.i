%module libsnarkwrapper
%include various.i
%include "std_vector.i"


%apply (char *STRING, size_t LENGTH) { (const char data[], size_t len) }
%apply (char *STRING, size_t LENGTH) { (const char data2[], size_t len2) }

%apply (char *STRING, size_t LENGTH) { (const char *data, size_t len) }

%typemap(jni) const signed char *get_data "jbyteArray"
%typemap(jtype) const signed char *get_data "byte[]"
%typemap(jstype) const signed char *get_data "byte[]"
%typemap(javaout) const signed char *get_data {
  return $jnicall;
}

%typemap(in,numinputs=0,noblock=1) size_t *len {
  size_t length=0;
  $1 = &length;
}

%typemap(out) const signed char *get_data {
  $result = JCALL1(NewByteArray, jenv, length);
  JCALL4(SetByteArrayRegion, jenv, $result, 0, length, $1);
}

%{
    #include "libsnarkwrapper.hpp"
%}

%template(TermVector) std::vector<Term>;
%template(ConstraintVector) std::vector<Constraint>;
%template(WireInfoVector) std::vector<WireInfo>;

%include "libsnarkwrapper.hpp"


