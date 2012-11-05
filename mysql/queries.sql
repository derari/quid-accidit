-- list tests
SELECT *
FROM trace.testtrace
ORDER BY id;

-- which methods were invoked
SELECT * 
FROM trace.vinvocationtrace 
ORDER BY testId, entry;

-- which objects are used in a test
SELECT o.*
FROM vObjectTrace o
JOIN TestTrace t ON t.id = o.testId
-- WHERE t.name LIKE "%test_re%"
ORDER BY testId, o.id;

-- which values appear for a field
SELECT f.*, o.type AS valueType 
FROM trace.vfieldtrace f
LEFT JOIN vObjectTrace o ON o.testId = f.testId AND f.primType = 0 AND f.value = o.id
WHERE f.type LIKE 'games.%'
ORDER BY f.testId, f.thisId, f.step;

-- where are exceptions thrown and caught
SELECT th.*, o.type as exception, thi.method as thrownIn, cai.method as caughtIn
FROM ThrowTrace th
JOIN TestTrace tt ON th.testId = tt.id
JOIN vObjectTrace o ON o.testId = th.testId AND o.id = th.exceptionId
JOIN vInvocationTrace thi ON thi.testId = th.testId AND thi.entry = th.invEntry
JOIN CatchTrace ca ON th.testId = ca.testId AND th.exceptionId = ca.exceptionId AND ca.step > th.step
JOIN vInvocationTrace cai ON th.testId = cai.testId AND ca.invEntry = cai.entry
GROUP BY th.testId, th.step
ORDER BY th.testId, th.step;

-- how often is a field changed
SELECT f.type, f.field, MAX(f.valueCount) as maxValueCount
FROM 
	(SELECT f.*, COUNT(*) as valueCount
	 FROM vfieldtrace f
	 GROUP BY testId, thisId, fieldId) f
GROUP BY fieldId
ORDER BY maxValueCount DESC, type, field;

-- how often is a variable changed
SELECT l.type, method, variable, MAX(l.valueCount) as maxValueCount
FROM 
	(SELECT l.*, COUNT(*) as valueCount
	 FROM vlocaltrace l
	 GROUP BY testId, invEntry, localId) l
GROUP BY localId
ORDER BY maxValueCount DESC, type, method, variable;

SELECT f.type, f.field, MAX(f.valueCount) as maxValueCount
FROM 
	(SELECT f.*, COUNT(*) as valueCount
	 FROM vfieldtrace f
	 GROUP BY testId, thisId, fieldId) f
GROUP BY fieldId
ORDER BY maxValueCount DESC, type, field;
