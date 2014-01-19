
LOAD DATA LOCAL INFILE "$CSVDIR$/mType.csv"
INTO TABLE `$SCHEMA$`.`Type`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, name, file, componentTypeId);

LOAD DATA LOCAL INFILE "$CSVDIR$/mExtends.csv"
INTO TABLE `$SCHEMA$`.`Extends`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"'  
LINES TERMINATED BY '\n'
(subId, superId);

LOAD DATA LOCAL INFILE "$CSVDIR$/mMethod.csv"
INTO TABLE `$SCHEMA$`.`Method`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, declaringTypeId, name, signature, line, hashcode);

LOAD DATA LOCAL INFILE "$CSVDIR$/mField.csv"
INTO TABLE `$SCHEMA$`.`Field`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, declaringTypeId, name, typeId);

LOAD DATA LOCAL INFILE "$CSVDIR$/mVariable.csv"
INTO TABLE `$SCHEMA$`.`Variable`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(methodId, id, name, typeId, parameter);

LOAD DATA LOCAL INFILE "$CSVDIR$/tTrace.csv"
INTO TABLE `$SCHEMA$`.`TestTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, name);

LOAD DATA LOCAL INFILE "$CSVDIR$/tObject.csv"
INTO TABLE `$SCHEMA$`.`ObjectTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, id, typeId, arrayLength);

LOAD DATA LOCAL INFILE "$CSVDIR$/tCall.csv"
INTO TABLE `$SCHEMA$`.`CallTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, parentStep, step, exitStep, methodId, thisId, depth, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tExit.csv"
INTO TABLE `$SCHEMA$`.`ExitTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, step, returned, primType, valueId, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tThrow.csv"
INTO TABLE `$SCHEMA$`.`ThrowTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, exceptionId, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tCatch.csv"
INTO TABLE `$SCHEMA$`.`CatchTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, exceptionId, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tVariable.csv"
INTO TABLE `$SCHEMA$`.`VariableTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, methodId, variableId, primType, valueId, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tPut.csv"
INTO TABLE `$SCHEMA$`.`PutTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, fieldId, primType, valueId, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tGet.csv"
INTO TABLE `$SCHEMA$`.`GetTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, fieldId, primType, valueId, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tArrayPut.csv"
INTO TABLE `$SCHEMA$`.`ArrayPutTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, `index`, primType, valueId, line);

LOAD DATA LOCAL INFILE "$CSVDIR$/tArrayGet.csv"
INTO TABLE `$SCHEMA$`.`ArrayGetTrace`
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, `index`, primType, valueId, line);