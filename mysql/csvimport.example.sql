-- Replace all "C:/trace" with the actual absolute path to the csv files

USE accidit;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM GetTrace;
DELETE FROM PutTrace;
DELETE FROM ArrayGetTrace;
DELETE FROM ArrayPutTrace;
DELETE FROM VariableTrace;
DELETE FROM ThrowTrace;
DELETE FROM CatchTrace;
DELETE FROM ExitTrace;
DELETE FROM CallTrace;
DELETE FROM ObjectTrace;
DELETE FROM TestTrace;
DELETE FROM Variable;
DELETE FROM Field;
DELETE FROM Method;
DELETE FROM Extends;
DELETE FROM Type;
SET FOREIGN_KEY_CHECKS = 1;

LOAD DATA LOCAL INFILE "C:/trace/mType.csv"
INTO TABLE Type
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, name, file, componentTypeId);

LOAD DATA LOCAL INFILE "C:/trace/mExtends.csv"
INTO TABLE Extends
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"'  
LINES TERMINATED BY '\n'
(subId, superId);

LOAD DATA LOCAL INFILE "C:/trace/mMethod.csv"
INTO TABLE Method
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, declaringTypeId, name, signature);

LOAD DATA LOCAL INFILE "C:/trace/mField.csv"
INTO TABLE Field
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, declaringTypeId, name, typeId);

LOAD DATA LOCAL INFILE "C:/trace/mVariable.csv"
INTO TABLE Variable
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(methodId, id, name, typeId, arg);

LOAD DATA LOCAL INFILE "C:/trace/tTrace.csv"
INTO TABLE TestTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(id, name);

LOAD DATA LOCAL INFILE "C:/trace/tObject.csv"
INTO TABLE ObjectTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, id, typeId, arrayLength);

LOAD DATA LOCAL INFILE "C:/trace/tCall.csv"
INTO TABLE CallTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, step, methodId, thisId, depth, line);

LOAD DATA LOCAL INFILE "C:/trace/tExit.csv"
INTO TABLE ExitTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, returned, primType, valueId, line);

LOAD DATA LOCAL INFILE "C:/trace/tThrow.csv"
INTO TABLE ThrowTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, exceptionId, line);

LOAD DATA LOCAL INFILE "C:/trace/tCatch.csv"
INTO TABLE CatchTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, exceptionId, line);

LOAD DATA LOCAL INFILE "C:/trace/tVariable.csv"
INTO TABLE VariableTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, methodId, variableId, primType, valueId, line);

LOAD DATA LOCAL INFILE "C:/trace/tPut.csv"
INTO TABLE PutTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, fieldId, primType, valueId, line);

LOAD DATA LOCAL INFILE "C:/trace/tGet.csv"
INTO TABLE GetTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, fieldId, primType, valueId, line);

LOAD DATA LOCAL INFILE "C:/trace/tArrayPut.csv"
INTO TABLE ArrayPutTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, `index`, primType, valueId, line);

LOAD DATA LOCAL INFILE "C:/trace/tArrayGet.csv"
INTO TABLE ArrayGetTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
(testId, callStep, step, thisId, `index`, primType, valueId, line);

