%module libsnarkwrapper
%include various.i
%include "std_vector.i"
%include "std_string.i"


%{
    #include "libsnarkwrapper.hpp"
%}

%template(TermVector) std::vector<Term>;
%template(ConstraintVector) std::vector<Constraint>;
%template(WireInfoVector) std::vector<WireInfo>;

%include "libsnarkwrapper.hpp"


