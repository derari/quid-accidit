
== Type ==
id;name;source;compType?

== Extends ==
sub;super

== Method ==
id;declaringTypeId;name;sig

== Variable ==
methodId;id;name;typeId;argument

== Field ==
id;declaringTypeId;name;typeId

== Trace ==
id;name

== ObjectTrace ==
tId;id;typeId;arrayLength

== CallTrace ==
tId;step;methodId;thisId;depth;line

== ExitTrace ==
tId;call;step;ret;valPType;valId;line

== ThrowTrace ==
tId;call;step;exId;line

== CatchTrace ==
tId;call;step;exId;line

== VariableTrace ==
tId;call;step;methId;varId;valPType;valId;line

== GetTrace ==
tId;call;step;thisId;fieldId;valPType;valId;line

== PutTrace ==
tId;call;step;thisId;fieldId;valPType;valId;line

== ArrayGetTrace ==
tId;call;step;thisId;index;valPType;valId;line

== ArrayPutTrace ==
tId;call;step;thisId;index;;valPType;valId;line

-- primType/valueId --
PrimType	Value
L			Object	ObjectTrace.id
Z			boolean	(valueId == 1)
B			byte	(byte) valueId
C			char	(char) valueId
D			double	Double.longBitsToDouble(valueId)
F			float	Float.intBitsToFloat((int)valueId)
I			int		(int) valueId
J			long	valueId
S			short	(short) valueId
V			void	
