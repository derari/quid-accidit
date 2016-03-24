
--try
DROP SCHEMA "$SCHEMA$" CASCADE 

CREATE SCHEMA "$SCHEMA$"

-- -----------------------------------------------------
-- Table "$SCHEMA$"."TestTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."TestTrace" (
  "id" INT NOT NULL ,
  "name" VARCHAR(255) NOT NULL ,
  PRIMARY KEY ("id") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."Type"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."Type" (
  "id" INT NOT NULL ,
  "name" VARCHAR(255) NOT NULL ,
  "file" VARCHAR(255) NULL ,
  "componentTypeId" INT NULL ,
  PRIMARY KEY ("id"))

-- -----------------------------------------------------
-- Table "$SCHEMA$"."Method"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."Method" (
  "id" INT NOT NULL ,
  "declaringTypeId" INT NOT NULL ,
  "name" VARCHAR(255) NOT NULL ,
  "signature" VARCHAR(2048) NOT NULL ,
  "line" INT NULL ,
  "hashcode" BIGINT NOT NULL,
  PRIMARY KEY ("id") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."ObjectTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."ObjectTrace" (
  "testId" INT NOT NULL ,
  "id" BIGINT NOT NULL ,
  "typeId" INT NOT NULL ,
  "arrayLength" INT NULL ,
  PRIMARY KEY ("testId", "id") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."CallTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."CallTrace" (
  "testId" INT NOT NULL ,
  "parentStep" BIGINT NULL ,
  "step" BIGINT NULL ,
  "exitStep" BIGINT NOT NULL ,
  "methodId" INT NOT NULL ,
  "thisId" BIGINT NULL ,
  "depth" INT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "step") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."Extends"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."Extends" (
  "subId" INT NOT NULL ,
  "superId" INT NOT NULL ,
  PRIMARY KEY ("subId", "superId") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."Field"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."Field" (
  "id" INT NOT NULL ,
  "declaringTypeId" INT NOT NULL ,
  "name" VARCHAR(255) NOT NULL ,
  "typeId" INT NOT NULL ,
  PRIMARY KEY ("id") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."Variable"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."Variable" (
  "methodId" INT NOT NULL ,
  "id" INT NOT NULL ,
  "name" VARCHAR(255) NOT NULL ,
  "typeId" INT NOT NULL ,
  "parameter" INT NOT NULL ,
  PRIMARY KEY ("methodId", "id") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."VariableTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."VariableTrace" (
  "testId" INT NOT NULL ,
  "callStep" BIGINT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "methodId" INT NOT NULL ,
  "variableId" INT NOT NULL ,
  "primType" CHAR NOT NULL ,
  "valueId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "callStep", "step", "variableId") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."PutTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."PutTrace" (
  "testId" INT NULL ,
  "callStep" BIGINT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "thisId" BIGINT NULL ,
  "fieldId" INT NOT NULL ,
  "primType" CHAR NOT NULL ,
  "valueId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "callStep", "step") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."ThrowTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."ThrowTrace" (
  "testId" INT NOT NULL ,
  "callStep" BIGINT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "exceptionId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "callStep", "step") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."CatchTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."CatchTrace" (
  "testId" INT NOT NULL ,
  "callStep" BIGINT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "exceptionId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "callStep", "step") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."ExitTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."ExitTrace" (
  "testId" INT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "returned" INT NOT NULL ,
  "primType" CHAR NOT NULL ,
  "valueId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "step") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."GetTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."GetTrace" (
  "testId" INT NULL ,
  "callStep" BIGINT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "thisId" BIGINT NULL ,
  "fieldId" INT NOT NULL ,
  "primType" CHAR NOT NULL ,
  "valueId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "callStep", "step") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."ArrayPutTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."ArrayPutTrace" (
  "testId" INT NULL ,
  "callStep" BIGINT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "thisId" BIGINT NULL ,
  "index" INT NOT NULL ,
  "primType" CHAR NOT NULL ,
  "valueId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "callStep", "step") )

-- -----------------------------------------------------
-- Table "$SCHEMA$"."ArrayGetTrace"
-- -----------------------------------------------------
CREATE TABLE "$SCHEMA$"."ArrayGetTrace" (
  "testId" INT NULL ,
  "callStep" BIGINT NOT NULL ,
  "step" BIGINT NOT NULL ,
  "thisId" BIGINT NULL ,
  "index" INT NOT NULL ,
  "primType" CHAR NOT NULL ,
  "valueId" BIGINT NOT NULL ,
  "line" INT NULL ,
  PRIMARY KEY ("testId", "callStep", "step") )

