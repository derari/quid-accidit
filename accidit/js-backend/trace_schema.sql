
DROP SCHEMA Quid_Accidit CASCADE;
CREATE SCHEMA Quid_Accidit;

CREATE COLUMN TABLE Quid_Accidit."TRACES" (
	"ID" INTEGER NOT NULL,
	"NAME" VARCHAR (100) NULL,
	PRIMARY KEY ("ID"));
	 
CREATE COLUMN TABLE Quid_Accidit."STEPS" (
	"TRACE_ID" INTEGER NOT NULL,
	"STEP" INTEGER NOT NULL,
	"ENTRY_STEP" INTEGER NOT NULL, -- step# of procedure entry
	"TYPE" VARCHAR (20) NOT NULL, -- call, enter, return, variable, query, cursor, cursor-row
	"TARGET" VARCHAR (100) NULL, -- var name, (enter: procedure name)
	"VALUE" VARCHAR (1000) NULL, -- atomic value
	"LINE" INTEGER NOT NULL, -- line number
	PRIMARY KEY ("TRACE_ID", "STEP", "TARGET"));
	
CREATE COLUMN TABLE Quid_Accidit."QUERIES" (
	"TRACE_ID" INTEGER NOT NULL,
	"STEP" INTEGER NOT NULL,
	"QUERY" VARCHAR (100) NOT NULL,
	"COUNT" INTEGER NOT NULL,
	"PRE" TIMESTAMP NOT NULL,
	"POST" TIMESTAMP NOT NULL,
	PRIMARY KEY ("TRACE_ID", "STEP"));
	
CREATE COLUMN TABLE Quid_Accidit."QUERY_ARGS" (
	"TRACE_ID" INTEGER NOT NULL,
	"STEP" INTEGER NOT NULL,
	"ID" INTEGER NOT NULL,
	"NAME" VARCHAR (100) NOT NULL,
	"VALUE" VARCHAR (100) NULL,
	PRIMARY KEY ("TRACE_ID", "STEP", "ID"));
	
CREATE FUNCTION Quid_Accidit.Snapshot(IN trace_id INT, IN curstep INT)
RETURNS TABLE (step INT, target VARCHAR(100), value VARCHAR(100)) LANGUAGE SQLSCRIPT AS
BEGIN
RETURN SELECT v.step, v.target, v.value
	FROM Quid_Accidit.Steps v
	JOIN (SELECT MAX(step) AS step, target FROM Quid_Accidit.Steps
		WHERE trace_id=:trace_id AND step <= :curstep AND (type = 'VARIABLE' OR type = 'QUERY')
		GROUP BY target) s
	  ON v.trace_id = :trace_id AND v.step = s.step AND v.target = s.target;
END;

DROP FUNCTION Quid_Accidit.Variables;
CREATE FUNCTION Quid_Accidit.Variables(IN trace_id INT, IN curstep INT)
RETURNS TABLE (target VARCHAR(100), value VARCHAR(100), lastWrite INTEGER, nextWrite INTEGER) LANGUAGE SQLSCRIPT AS
BEGIN
	snapshot = SELECT step, target, value FROM Quid_Accidit.Snapshot(:trace_id, :curstep);
	nextWrites = SELECT MIN(step) AS step, target
			FROM Quid_Accidit.Steps
			WHERE trace_id=:trace_id AND step > :curstep AND (type = 'VARIABLE' OR type = 'QUERY')
			GROUP BY target;
	RETURN SELECT COALESCE(s.target, n.target) AS target, 
				  COALESCE(q.count + ' rows', s.value) AS value, 
				  s.step AS lastWrite, n.step AS nextWrite
		FROM :snapshot s
		FULL OUTER JOIN :nextWrites n ON s.target = n.target
		LEFT OUTER JOIN Quid_Accidit.Queries q ON s.step = q.step AND q.trace_id = :trace_id;
END;