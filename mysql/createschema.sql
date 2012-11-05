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
  PRIMARY KEY (`id`) )
ENGINE = InnoDB;

CREATE INDEX `Type_name_idx` ON `accidit`.`Type` (`name` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`Method`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`Method` (
  `id` INT NOT NULL ,
  `declaringTypeId` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `signature` VARCHAR(2048) NOT NULL ,
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
-- Table `accidit`.`InvocationTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`InvocationTrace` (
  `testId` INT NOT NULL ,
  `entry` BIGINT NOT NULL ,
  `exit` BIGINT NOT NULL ,
  `depth` INT NOT NULL ,
  `callLine` INT NOT NULL ,
  `methodId` INT NOT NULL ,
  `thisId` BIGINT NULL ,
  `returned` TINYINT(1) NOT NULL ,
  `retPrimType` INT NOT NULL ,
  `retValue` BIGINT NOT NULL ,
  `retLine` INT NOT NULL ,
  PRIMARY KEY (`testId`, `entry`) ,
  CONSTRAINT `fk_InvocationTrace_TestTrace`
    FOREIGN KEY (`testId` )
    REFERENCES `accidit`.`TestTrace` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_InvocationTrace_Method`
    FOREIGN KEY (`methodId` )
    REFERENCES `accidit`.`Method` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_InvocationTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_InvocationTrace_TestTrace_idx` ON `accidit`.`InvocationTrace` (`testId` ASC) ;

CREATE INDEX `fk_InvocationTrace_Method_idx` ON `accidit`.`InvocationTrace` (`methodId` ASC) ;

CREATE INDEX `fk_InvocationTrace_ObjectTrace_idx` ON `accidit`.`InvocationTrace` (`testId` ASC, `thisId` ASC) ;


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
  PRIMARY KEY (`id`) ,
  CONSTRAINT `fk_Field_Type`
    FOREIGN KEY (`declaringTypeId` )
    REFERENCES `accidit`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_Field_Type_idx` ON `accidit`.`Field` (`declaringTypeId` ASC) ;

CREATE INDEX `Field_declType_name_idx` ON `accidit`.`Field` (`declaringTypeId` ASC, `name` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`Local`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`Local` (
  `methodId` INT NOT NULL ,
  `id` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `arg` TINYINT(1) NOT NULL ,
  PRIMARY KEY (`methodId`, `id`) ,
  CONSTRAINT `fk_Local_Method`
    FOREIGN KEY (`methodId` )
    REFERENCES `accidit`.`Method` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `Local_method_name_idx` ON `accidit`.`Local` (`methodId` ASC, `name` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`LocalTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`LocalTrace` (
  `testId` INT NOT NULL ,
  `invEntry` BIGINT NOT NULL ,
  `step` INT NOT NULL ,
  `methodId` INT NOT NULL ,
  `localId` INT NOT NULL ,
  `primType` INT NOT NULL ,
  `value` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `invEntry`, `step`) ,
  CONSTRAINT `fk_LocalTrace_InvocationTrace`
    FOREIGN KEY (`testId` , `invEntry` )
    REFERENCES `accidit`.`InvocationTrace` (`testId` , `entry` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_LocalTrace_Local`
    FOREIGN KEY (`methodId` , `localId` )
    REFERENCES `accidit`.`Local` (`methodId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_LocalTrace_Local_idx` ON `accidit`.`LocalTrace` (`methodId` ASC, `localId` ASC) ;

CREATE INDEX `fk_LocalTrace_InvocationTrace_idx` ON `accidit`.`LocalTrace` (`testId` ASC, `invEntry` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`FieldTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`FieldTrace` (
  `testId` INT NULL ,
  `invEntry` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `thisId` BIGINT NULL ,
  `fieldId` INT NOT NULL ,
  `primType` INT NOT NULL ,
  `value` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `invEntry`, `step`) ,
  CONSTRAINT `fk_FieldTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_FieldTrace_Field`
    FOREIGN KEY (`fieldId` )
    REFERENCES `accidit`.`Field` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_FieldTrace_InvocationTrace`
    FOREIGN KEY (`testId` , `invEntry` )
    REFERENCES `accidit`.`InvocationTrace` (`testId` , `entry` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_FieldTrace_Field_idx` ON `accidit`.`FieldTrace` (`fieldId` ASC) ;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `accidit`.`FieldTrace` (`testId` ASC, `invEntry` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `accidit`.`FieldTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`AccessTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`AccessTrace` (
  `testId` INT NULL ,
  `invEntry` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `thisId` BIGINT NULL ,
  `fieldId` INT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `invEntry`, `step`) ,
  CONSTRAINT `fk_AccessTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_AccessTrace_Field`
    FOREIGN KEY (`fieldId` )
    REFERENCES `accidit`.`Field` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_AccessTrace_InvocationTrace`
    FOREIGN KEY (`testId` , `invEntry` )
    REFERENCES `accidit`.`InvocationTrace` (`testId` , `entry` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_AccessTrace_Field_idx` ON `accidit`.`AccessTrace` (`fieldId` ASC) ;

CREATE INDEX `fk_AccessTrace_InvocationTrace_idx` ON `accidit`.`AccessTrace` (`testId` ASC, `invEntry` ASC) ;

CREATE INDEX `fk_AccessTrace_ObjectTrace_idx` ON `accidit`.`AccessTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`ThrowTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`ThrowTrace` (
  `testId` INT NOT NULL ,
  `invEntry` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `exceptionId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `invEntry`, `step`) ,
  CONSTRAINT `fk_ThrowTrace_InvocationTrace`
    FOREIGN KEY (`testId` , `invEntry` )
    REFERENCES `accidit`.`InvocationTrace` (`testId` , `entry` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ThrowTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `exceptionId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_ThrowTrace_ObjectTrace_idx` ON `accidit`.`ThrowTrace` (`testId` ASC, `exceptionId` ASC) ;

CREATE INDEX `fk_ThrowTrace_InvocationTrace_idx` ON `accidit`.`ThrowTrace` (`testId` ASC, `invEntry` ASC) ;


-- -----------------------------------------------------
-- Table `accidit`.`CatchTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `accidit`.`CatchTrace` (
  `testId` INT NOT NULL ,
  `invEntry` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `exceptionId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `invEntry`, `step`) ,
  CONSTRAINT `fk_CatchTrace_InvocationTrace`
    FOREIGN KEY (`testId` , `invEntry` )
    REFERENCES `accidit`.`InvocationTrace` (`testId` , `entry` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_CatchTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `exceptionId` )
    REFERENCES `accidit`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `fk_CatchTrace_ObjectTrace_idx` ON `accidit`.`CatchTrace` (`testId` ASC, `exceptionId` ASC) ;

CREATE INDEX `fk_CatchTrace_InvocationTrace_idx` ON `accidit`.`CatchTrace` (`testId` ASC, `invEntry` ASC) ;


-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vInvocationTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vInvocationTrace` (`testId` INT, `entry` INT, `exit` INT, `depth` INT, `callLine` INT, `methodId` INT, `type` INT, `method` INT, `thisId` INT, `returned` INT, `retPrimType` INT, `retValue` INT, `retLine` INT);

-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vLocalTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vLocalTrace` (`testId` INT, `invEntry` INT, `step` INT, `methodId` INT, `localId` INT, `type` INT, `method` INT, `variable` INT, `arg` INT, `primType` INT, `value` INT, `line` INT);

-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vFieldTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vFieldTrace` (`testId` INT, `invEntry` INT, `step` INT, `thisId` INT, `fieldId` INT, `type` INT, `field` INT, `primType` INT, `value` INT, `line` INT);

-- -----------------------------------------------------
-- Placeholder table for view `accidit`.`vObjectTrace`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `accidit`.`vObjectTrace` (`testId` INT, `id` INT, `typeId` INT, `type` INT);

-- -----------------------------------------------------
-- View `accidit`.`vInvocationTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`vInvocationTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`vInvocationTrace` AS
SELECT i.testId, i.entry, i.exit, i.depth, i.callLine, i.methodId, t.name AS type, m.name AS method, i.thisId, i.returned, i.retPrimType, i.retValue, i.retLine
FROM InvocationTrace i
JOIN Method m ON i.methodId = m.id
JOIN Type t ON m.declaringTypeId = t.id;

-- -----------------------------------------------------
-- View `accidit`.`vLocalTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`vLocalTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`vLocalTrace` AS
SELECT lt.testId, lt.invEntry, lt.step, lt.methodId, lt.localId, t.name as type, m.name as method, l.name as variable, l.arg as arg, lt.primType, lt.value, lt.line
FROM LocalTrace lt
JOIN Local l ON lt.methodId = l.methodId AND lt.localId = l.id
JOIN Method m ON l.methodId = m.id
JOIN Type t ON m.declaringTypeId = t.id;

-- -----------------------------------------------------
-- View `accidit`.`vFieldTrace`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `accidit`.`vFieldTrace`;
USE `accidit`;
CREATE  OR REPLACE VIEW `accidit`.`vFieldTrace` AS
SELECT ft.testId, ft.invEntry, ft.step, ft.thisId, ft.fieldId, t.name as type, f.name as field, ft.primType, ft.value, ft.line
FROM FieldTrace ft
JOIN Field f ON ft.fieldId = f.id
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

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
