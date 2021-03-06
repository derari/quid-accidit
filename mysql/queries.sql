-- list tests
SELECT *
FROM accidit.testtrace
ORDER BY id;

-- which methods were invoked
SELECT * 
FROM accidit.vinvocationtrace 
ORDER BY testId, callStep;

-- which objects are used in a test
SELECT o.*
FROM vObjectTrace o
JOIN TestTrace t ON t.id = o.testId
-- WHERE t.name LIKE "%test_re%"
ORDER BY testId, o.id;

-- which values appear for a field
SELECT f.*, o.type AS valueType 
FROM vputtrace f
LEFT JOIN vObjectTrace o ON o.testId = f.testId AND f.primType = 'L' AND f.valueId = o.id
-- WHERE f.type LIKE 'games.%'
ORDER BY f.testId, f.thisId, f.step;

-- where are exceptions thrown and caught
SELECT th.*, o.type as exception, thi.method as thrownIn, cai.method as caughtIn
FROM ThrowTrace th
JOIN TestTrace tt ON th.testId = tt.id
JOIN vObjectTrace o ON o.testId = th.testId AND o.id = th.exceptionId
JOIN vInvocationTrace thi ON thi.testId = th.testId AND thi.callStep = th.callStep
LEFT JOIN CatchTrace ca ON th.testId = ca.testId AND th.exceptionId = ca.exceptionId AND ca.step > th.step
LEFT JOIN vInvocationTrace cai ON th.testId = cai.testId AND ca.callStep = cai.callStep
GROUP BY th.testId, th.step
ORDER BY th.testId, th.step;

-- how often is a field changed
SELECT f.type, f.field, MAX(f.valueCount) as maxValueCount
FROM 
	(SELECT f.*, COUNT(*) as valueCount
	 FROM vputtrace f
	 GROUP BY testId, thisId, fieldId) f
GROUP BY fieldId
ORDER BY maxValueCount DESC, type, field;

-- how often is a variable changed
SELECT l.type, method, variable, MAX(l.valueCount) as maxValueCount
FROM 
	(SELECT l.*, COUNT(*) as valueCount
	 FROM vvariabletrace l
	 GROUP BY testId, callStep, variableId) l
GROUP BY variableId
ORDER BY maxValueCount DESC, type, method, variable;
