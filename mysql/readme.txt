
== Type ==
id; name; file; componentTypeId?
- componentTypeId:
  if type is an array, the id of the component type, otherwise NULL

== Extends ==
subId; superId

== Method ==
id; declaringTypeId; name; signature

== Variable ==
methodId; id; name; typeId; arg

== Field ==
id; declaringTypeId; name; typeId

== TestTrace ==
id; name

== ObjectTrace ==
testestId; id; typeId; arrayLength?

== CallTrace ==
testId; step; methodId; thisId?; depth; line

== ExitTrace ==
testId; callStep; step; returned; primtType; valueId; line
- returned: 
  if true, primType/valueId encode the return value;
  if false, valueId references an exception object

== View: InvocationTrace ==
testId; callStep; exitStep; methodId; thisId?; depth; callLine; returned; exitPrimType; exitValueId; exitLine

== ThrowTrace ==
testId; callStep; step; exceptionId; line

== CatchTrace ==
testId; callStep; step; exceptionId; line

== VariableTrace ==
testId; callStep; step; methodId; variableId; primType; valueId; line

== GetTrace ==
testId; callStep; step; thisId?; fieldId; primType; valueId; line

== PutTrace ==
testId; callStep; step; thisId?; fieldId; primType; valueId; line

== ArrayGetTrace ==
testId; callStep; step; thisId; index; primType; valueId; line

== ArrayPutTrace ==
testId; callStep; step; thisId; index; primType; valueId; line

-- primType/valueId --
PrimType  Value		long-to-value

L	  Object	ObjectTrace.id == valueId
Z	  boolean	(valueId == 1)
B	  byte		(byte) valueId
C	  char		(char) valueId
D	  double	Double.longBitsToDouble(valueId)
F	  float		Float.intBitsToFloat((int)valueId)
I	  int		(int) valueId
J	  long		valueId
S	  short		(short) valueId
V	  void	
