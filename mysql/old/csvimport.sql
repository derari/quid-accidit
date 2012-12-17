DELETE FROM AccessTrace;
DELETE FROM FieldTrace;
DELETE FROM LocalTrace;
DELETE FROM ThrowTrace;
DELETE FROM CatchTrace;
DELETE FROM InvocationTrace;
DELETE FROM ObjectTrace;
DELETE FROM TestTrace;
DELETE FROM Local;
DELETE FROM Field;
DELETE FROM Method;
DELETE FROM Extends;
DELETE FROM Type;

LOAD DATA LOCAL INFILE "C:/trace/mTypes.csv"
INTO TABLE Type
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(id, name, file);

LOAD DATA LOCAL INFILE "C:/trace/mExtends.csv"
INTO TABLE Extends
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(subId, superId);

LOAD DATA LOCAL INFILE "C:/trace/mMethods.csv"
INTO TABLE Method
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(id, declaringTypeId, name, signature);

LOAD DATA LOCAL INFILE "C:/trace/mFields.csv"
INTO TABLE Field
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(id, declaringTypeId, name);

LOAD DATA LOCAL INFILE "C:/trace/mLocals.csv"
INTO TABLE Local
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(methodId, id, name, arg);

LOAD DATA LOCAL INFILE "C:/trace/tTests.csv"
INTO TABLE TestTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(id, name);

LOAD DATA LOCAL INFILE "C:/trace/tObjects.csv"
INTO TABLE ObjectTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(testId, id, typeId);

LOAD DATA LOCAL INFILE "C:/trace/tInvocations.csv"
INTO TABLE InvocationTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(testId, entry, `exit`, depth, callLine, methodId, thisId, returned, retPrimType, retValue, retLine);

LOAD DATA LOCAL INFILE "C:/trace/tLocals.csv"
INTO TABLE LocalTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(testId, invEntry, step, methodId, localId, primType, value, line);

LOAD DATA LOCAL INFILE "C:/trace/tFields.csv"
INTO TABLE FieldTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(testId, invEntry, step, thisId, fieldId, primType, value, line);

LOAD DATA LOCAL INFILE "C:/trace/tAccesses.csv"
INTO TABLE AccessTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(testId, invEntry, step, thisId, fieldId, line);

LOAD DATA LOCAL INFILE "C:/trace/tThrows.csv"
INTO TABLE ThrowTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(testId, invEntry, step, exceptionId, line);

LOAD DATA LOCAL INFILE "C:/trace/tCatches.csv"
INTO TABLE CatchTrace
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '\'' 
LINES TERMINATED BY '\n'
(testId, invEntry, step, exceptionId, line);
