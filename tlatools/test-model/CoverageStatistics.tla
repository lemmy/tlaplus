---------------------------- MODULE CoverageStatistics ----------------------------
EXTENDS Naturals, FiniteSets
VARIABLE x

Op1(S) == \A e \in SUBSET S : Cardinality(e) >= 0

Init == /\ x = 0
        /\ Op1(1..17)

\* Max(s)
Op2(n) == CHOOSE i \in 1..n : \A j \in ((1..n) \ {i}) : i > j

A(n) == x' = Op2(n)

B(n) == x' = Op2(n)

Next == A(10) \/ B(3)

Constraint == TRUE
=============================================================================
VARIABLE x

Init == x = FALSE

Switch(var) == ~var

A == x' = Switch(x)

B == x' = Switch(x)



Next == A \/ B

Constraint == TRUE

=============================================================================
EXTENDS Naturals, FiniteSets
VARIABLES x,y

c == x
d == y

vars == <<x,y>>

a == x'

Init == /\ x \in 1..3
        /\ y = 0

A == /\ c \in Nat
     /\ y \in Nat
     /\ a = x + 1
     /\ UNCHANGED y

B == /\ x \in Nat
     /\ UNCHANGED <<x,d>>

C == /\ x = 42
     /\ c' = TRUE
     /\ y' = FALSE

U1 == x < 0 /\ UNCHANGED vars

U2 == x < 0 /\ UNCHANGED <<x,y>>

U3 == x < 0 /\ UNCHANGED x /\ UNCHANGED y

Next == A \/ B \/ C \/ U1 \/ U2 \/ U3

Spec == Init /\ [][Next]_vars

Constraint == x < 20

=============================================================================
