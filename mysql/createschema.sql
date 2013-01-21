SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

DROP SCHEMA IF EXISTS `accidit` ;
CREATE SCHEMA IF NOT EXISTS `accidit` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci ;
USE `accidit` ;

-- -----------------------------------------------------
-- Table `accidit`.`TestTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`TestTrace` (
  `id` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  PRIMARY KEY (`id`) )
ENGINE = InnoDB;

CREATE UNIQUE INDEX `id_UNIQUE` ON `accidit`.`TestTrace` (`id` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`Type`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`Type` (
  `id` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `file` VARCHAR(255) NULL ,
  `componentTypeId` INT NULL ,
  PRIMARY KEY (`id`) ,
  CONSTRAINT `fk_Type_componentType`
    FOREIGN KEY (`componentTypeId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `Type_name_idx` ON `accidit`.`Type` (`name` ASC) ;

CREATE INDEX `fk_Type_componentTypeId_idx` ON `accidit`.`Type` (`componentTypeId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`Method`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`Method` (
  `id` INT NOT NULL ,
  `declaringTypeId` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `signature` VARCHAR(2048) NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`id`) ,
  CONSTRAINT `fk_Method_Type`
    FOREIGN KEY (`declaringTypeId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_Method_Type_idx` ON `accidit`.`Method` (`declaringTypeId` ASC) ;

CREATE INDEX `Method_declType_name_sig_idx` ON `accidit`.`Method` (`declaringTypeId` ASC, `name` ASC, `signature` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`ObjectTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`ObjectTrace` (
  `testId` INT NOT NULL ,
  `id` BIGINT NOT NULL ,
  `typeId` INT NOT NULL ,
  `arrayLength` INT NULL ,
  PRIMARY KEY (`testId`, `id`) ,
  CONSTRAINT `fk_ObjectTrace_TestTrace`
    FOREIGN KEY (`testId` )
    REFERENCES `accidit`.`TestTrace` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ObjectTrace_Type`
    FOREIGN KEY (`typeId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_ObjectTrace_Type_idx` ON `accidit`.`ObjectTrace` (`typeId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`CallTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`CallTrace` (
  `testId` INT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `methodId` INT NOT NULL ,
  `thisId` BIGINT NULL ,
  `depth` INT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `step`) ,
  CONSTRAINT `fk_CallTrace_TestTrace`
    FOREIGN KEY (`testId` )
    REFERENCES `accidit`.`TestTrace` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_CallTrace_Method`
    FOREIGN KEY (`methodId` )
    REFERENCES `accidit`.`Method` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_CallTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_InvocationTrace_TestTrace_idx` ON `accidit`.`CallTrace` (`testId` ASC) ;

CREATE INDEX `fk_InvocationTrace_Method_idx` ON `accidit`.`CallTrace` (`methodId` ASC) ;

CREATE INDEX `fk_InvocationTrace_ObjectTrace_idx` ON `accidit`.`CallTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`Extends`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`Extends` (
  `subId` INT NOT NULL ,
  `superId` INT NOT NULL ,
  PRIMARY KEY (`subId`, `superId`) ,
  CONSTRAINT `fk_Extends_Type_sub`
    FOREIGN KEY (`subId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_Extends_Type_super`
    FOREIGN KEY (`superId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_Extends_Type_super_idx` ON `accidit`.`Extends` (`superId` ASC) ;

CREATE INDEX `fk_Extends_Type_sub_idx` ON `accidit`.`Extends` (`subId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`Field`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`Field` (
  `id` INT NOT NULL ,
  `declaringTypeId` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `typeId` INT NOT NULL ,
  PRIMARY KEY (`id`) ,
  CONSTRAINT `fk_Field_Type_declaringType`
    FOREIGN KEY (`declaringTypeId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_Field_Type_type`
    FOREIGN KEY (`typeId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_Field_Type_idx` ON `accidit`.`Field` (`declaringTypeId` ASC) ;

CREATE INDEX `Field_declType_name_idx` ON `accidit`.`Field` (`declaringTypeId` ASC, `name` ASC) ;

CREATE INDEX `fk_Field_Type_type_idx` ON `accidit`.`Field` (`typeId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`Variable`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`Variable` (
  `methodId` INT NOT NULL ,
  `id` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `typeId` INT NOT NULL ,
  `parameter` TINYINT(1) NOT NULL ,
  PRIMARY KEY (`methodId`, `id`) ,
  CONSTRAINT `fk_Variable_Method`
    FOREIGN KEY (`methodId` )
    REFERENCES `accidit`.`Method` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_Variable_Type`
    FOREIGN KEY (`typeId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `Local_method_name_idx` ON `accidit`.`Variable` (`methodId` ASC, `name` ASC) ;

CREATE INDEX `fk_Variable_Typ_idx` ON `accidit`.`Variable` (`typeId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`VariableTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`VariableTrace` (
  `testId` INT NOT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `methodId` INT NOT NULL ,
  `variableId` INT NOT NULL ,
  `primType` CHAR NOT NULL ,
  `valueId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`, `variableId`) ,
  CONSTRAINT `fk_VariableTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_VariableTrace_Variable`
    FOREIGN KEY (`methodId` , `variableId` )
    REFERENCES `accidit`.`Variable` (`methodId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_LocalTrace_Local_idx` ON `accidit`.`VariableTrace` (`methodId` ASC, `variableId` ASC) ;

CREATE INDEX `fk_LocalTrace_InvocationTrace_idx` ON `accidit`.`VariableTrace` (`testId` ASC, `callStep` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`PutTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`PutTrace` (
  `testId` INT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `thisId` BIGINT NULL ,
  `fieldId` INT NOT NULL ,
  `primType` CHAR NOT NULL ,
  `valueId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_PutTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_PutTrace_Field`
    FOREIGN KEY (`fieldId` )
    REFERENCES `accidit`.`Field` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_PutTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_FieldTrace_Field_idx` ON `accidit`.`PutTrace` (`fieldId` ASC) ;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `accidit`.`PutTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `accidit`.`PutTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`ThrowTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`ThrowTrace` (
  `testId` INT NOT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `exceptionId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_ThrowTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ThrowTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `exceptionId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_ThrowTrace_ObjectTrace_idx` ON `accidit`.`ThrowTrace` (`testId` ASC, `exceptionId` ASC) ;

CREATE INDEX `fk_ThrowTrace_InvocationTrace_idx` ON `accidit`.`ThrowTrace` (`testId` ASC, `callStep` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`CatchTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`CatchTrace` (
  `testId` INT NOT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `exceptionId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_CatchTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_CatchTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `exceptionId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_CatchTrace_ObjectTrace_idx` ON `accidit`.`CatchTrace` (`testId` ASC, `exceptionId` ASC) ;

CREATE INDEX `fk_CatchTrace_InvocationTrace_idx` ON `accidit`.`CatchTrace` (`testId` ASC, `callStep` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`ExitTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`ExitTrace` (
  `testId` INT NOT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `returned` TINYINT(1) NOT NULL ,
  `primType` CHAR NOT NULL ,
  `valueId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`) ,
  CONSTRAINT `fk_ExitTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_ExitTrace_CallTrace_idx` ON `accidit`.`ExitTrace` (`testId` ASC, `callStep` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`GetTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`GetTrace` (
  `testId` INT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `thisId` BIGINT NULL ,
  `fieldId` INT NOT NULL ,
  `primType` CHAR NOT NULL ,
  `valueId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_GetTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_GetTrace_Field`
    FOREIGN KEY (`fieldId` )
    REFERENCES `accidit`.`Field` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_GetTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_FieldTrace_Field_idx` ON `accidit`.`GetTrace` (`fieldId` ASC) ;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `accidit`.`GetTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `accidit`.`GetTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`ArrayPutTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`ArrayPutTrace` (
  `testId` INT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `thisId` BIGINT NULL ,
  `index` INT NOT NULL ,
  `primType` CHAR NOT NULL ,
  `valueId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_ArrayPutTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ArrayPutTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `accidit`.`ArrayPutTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `accidit`.`ArrayPutTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`ArrayGetTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`ArrayGetTrace` (
  `testId` INT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `thisId` BIGINT NULL ,
  `index` INT NOT NULL ,
  `primType` CHAR NOT NULL ,
  `valueId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_ArrayGetTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ArrayGetTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `accidit`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `accidit`.`ArrayGetTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `accidit`.`ArrayGetTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vInvocationTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vInvocationTrace` (`testId` INT, `callStep` INT, `exitStep` INT, `depth` INT, `callLine` INT, `methodId` INT, `type` INT, `method` INT, `thisId` INT, `returned` INT, `exitPrimType` INT, `exitValueId` INT, `exitLine` INT);

-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vVariableTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vVariableTrace` (`testId` INT, `callStep` INT, `step` INT, `methodId` INT, `variableId` INT, `type` INT, `method` INT, `variable` INT, `parameter` INT, `primType` INT, `valueId` INT, `line` INT);

-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vPutTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vPutTrace` (`testId` INT, `callStep` INT, `step` INT, `thisId` INT, `fieldId` INT, `type` INT, `field` INT, `primType` INT, `valueId` INT, `line` INT);

-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vObjectTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vObjectTrace` (`testId` INT, `id` INT, `typeId` INT, `type` INT);

-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`InvocationTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`InvocationTrace` (`testId` INT, `callStep` INT, `exitStep` INT, `methodId` INT, `thisId` INT, `depth` INT, `callLine` INT, `returned` INT, `exitPrimType` INT, `exitValueId` INT, `exitLine` INT);

-- -----------------------------------------------------
-- View `accidit`.`vInvocationTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`vInvocationTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`vInvocationTrace` AS
SELECT i.testId, i.callStep, i.exitStep, i.depth, i.callLine, i.methodId, t.name AS type, m.name AS method, i.thisId, i.returned, i.exitPrimType, i.exitValueId, i.exitLine
FROM InvocationTrace i
JOIN Method m ON i.methodId = m.id
JOIN Type t ON m.declaringTypeId = t.id;

-- -----------------------------------------------------
-- View `accidit`.`vVariableTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`vVariableTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`vVariableTrace` AS
SELECT vt.testId, vt.callStep, vt.step, vt.methodId, vt.variableId, t.name as type, m.name as method, v.name as variable, v.parameter as parameter, vt.primType, vt.valueId, vt.line
FROM VariableTrace vt
JOIN Variable v ON vt.methodId = v.methodId AND vt.variableId = v.id
JOIN Method m ON v.methodId = m.id
JOIN Type t ON m.declaringTypeId = t.id;

-- -----------------------------------------------------
-- View `accidit`.`vPutTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`vPutTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`vPutTrace` AS
SELECT pt.testId, pt.callStep, pt.step, pt.thisId, pt.fieldId, t.name as type, f.name as field, pt.primType, pt.valueId, pt.line
FROM PutTrace pt
JOIN Field f ON pt.fieldId = f.id
JOIN Type t ON f.declaringTypeId = t.id;

-- -----------------------------------------------------
-- View `accidit`.`vObjectTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`vObjectTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`vObjectTrace` AS
SELECT o.testId, o.id, o.typeId, t.name as type
FROM ObjectTrace o
JOIN Type t WHERE o.typeId = t.id;

-- -----------------------------------------------------
-- View `accidit`.`InvocationTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`InvocationTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`InvocationTrace` AS
SELECT c.testId, c.step AS callStep, e.step AS exitStep, 
       c.methodId, c.thisId, c.depth, c.line AS callLine,
       e.returned, e.primType AS exitPrimType, 
       e.valueId AS exitValueId, e.line AS exitLine
FROM CallTrace c
JOIN ExitTrace e ON c.testId = e.testId AND c.step = e.callStep;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
