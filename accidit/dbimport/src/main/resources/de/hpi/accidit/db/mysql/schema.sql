SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;

SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

DROP SCHEMA IF EXISTS `$SCHEMA$` ;

CREATE SCHEMA IF NOT EXISTS `$SCHEMA$` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci ;

-- -----------------------------------------------------
-- Table `$SCHEMA$`.`TestTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`TestTrace` (
  `id` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  PRIMARY KEY (`id`) )
ENGINE = MyISAM;

CREATE UNIQUE INDEX `id_UNIQUE` ON `$SCHEMA$`.`TestTrace` (`id` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`Type`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`Type` (
  `id` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `file` VARCHAR(255) NULL ,
  `componentTypeId` INT NULL ,
  PRIMARY KEY (`id`) ,
  CONSTRAINT `fk_Type_componentType`
    FOREIGN KEY (`componentTypeId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `Type_name_idx` ON `$SCHEMA$`.`Type` (`name` ASC) ;

CREATE INDEX `fk_Type_componentTypeId_idx` ON `$SCHEMA$`.`Type` (`componentTypeId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`Method`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`Method` (
  `id` INT NOT NULL ,
  `declaringTypeId` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `signature` VARCHAR(2048) NOT NULL ,
  `line` INT NULL ,
  `hashcode` BIGINT NOT NULL ,
  PRIMARY KEY (`id`) ,
  CONSTRAINT `fk_Method_Type`
    FOREIGN KEY (`declaringTypeId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_Method_Type_idx` ON `$SCHEMA$`.`Method` (`declaringTypeId` ASC) ;

-- CREATE INDEX `Method_declType_name_sig_idx` ON `$SCHEMA$`.`Method` (`declaringTypeId` ASC, `name` ASC, `signature` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`ObjectTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`ObjectTrace` (
  `testId` INT NOT NULL ,
  `id` BIGINT NOT NULL ,
  `typeId` INT NOT NULL ,
  `arrayLength` INT NULL ,
  PRIMARY KEY (`testId`, `id`) ,
  CONSTRAINT `fk_ObjectTrace_TestTrace`
    FOREIGN KEY (`testId` )
    REFERENCES `$SCHEMA$`.`TestTrace` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ObjectTrace_Type`
    FOREIGN KEY (`typeId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_ObjectTrace_Type_idx` ON `$SCHEMA$`.`ObjectTrace` (`typeId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`CallTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`CallTrace` (
  `testId` INT NOT NULL ,
  `parentStep` BIGINT NULL ,
  `step` BIGINT NOT NULL ,
  `exitStep` BIGINT NOT NULL ,
  `methodId` INT NOT NULL ,
  `thisId` BIGINT NULL ,
  `depth` INT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `step`) ,
  CONSTRAINT `fk_CallTrace_TestTrace`
    FOREIGN KEY (`testId` )
    REFERENCES `$SCHEMA$`.`TestTrace` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_CallTrace_Method`
    FOREIGN KEY (`methodId` )
    REFERENCES `$SCHEMA$`.`Method` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_CallTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `thisId` )
    REFERENCES `$SCHEMA$`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_InvocationTrace_TestTrace_idx` ON `$SCHEMA$`.`CallTrace` (`testId` ASC) ;

CREATE INDEX `fk_InvocationTrace_Method_idx` ON `$SCHEMA$`.`CallTrace` (`methodId` ASC) ;

CREATE INDEX `fk_InvocationTrace_ObjectTrace_idx` ON `$SCHEMA$`.`CallTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`Extends`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`Extends` (
  `subId` INT NOT NULL ,
  `superId` INT NOT NULL ,
  PRIMARY KEY (`subId`, `superId`) ,
  CONSTRAINT `fk_Extends_Type_sub`
    FOREIGN KEY (`subId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_Extends_Type_super`
    FOREIGN KEY (`superId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_Extends_Type_super_idx` ON `$SCHEMA$`.`Extends` (`superId` ASC) ;

CREATE INDEX `fk_Extends_Type_sub_idx` ON `$SCHEMA$`.`Extends` (`subId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`Field`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`Field` (
  `id` INT NOT NULL ,
  `declaringTypeId` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `typeId` INT NOT NULL ,
  PRIMARY KEY (`id`) ,
  CONSTRAINT `fk_Field_Type_declaringType`
    FOREIGN KEY (`declaringTypeId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_Field_Type_type`
    FOREIGN KEY (`typeId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_Field_Type_idx` ON `$SCHEMA$`.`Field` (`declaringTypeId` ASC) ;

CREATE INDEX `Field_declType_name_idx` ON `$SCHEMA$`.`Field` (`declaringTypeId` ASC, `name` ASC) ;

CREATE INDEX `fk_Field_Type_type_idx` ON `$SCHEMA$`.`Field` (`typeId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`Variable`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`Variable` (
  `methodId` INT NOT NULL ,
  `id` INT NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `typeId` INT NOT NULL ,
  `parameter` TINYINT(1) NOT NULL ,
  PRIMARY KEY (`methodId`, `id`) ,
  CONSTRAINT `fk_Variable_Method`
    FOREIGN KEY (`methodId` )
    REFERENCES `$SCHEMA$`.`Method` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_Variable_Type`
    FOREIGN KEY (`typeId` )
    REFERENCES `$SCHEMA$`.`Type` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `Local_method_name_idx` ON `$SCHEMA$`.`Variable` (`methodId` ASC, `name` ASC) ;

CREATE INDEX `fk_Variable_Typ_idx` ON `$SCHEMA$`.`Variable` (`typeId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`VariableTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`VariableTrace` (
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
    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_VariableTrace_Variable`
    FOREIGN KEY (`methodId` , `variableId` )
    REFERENCES `$SCHEMA$`.`Variable` (`methodId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_LocalTrace_Local_idx` ON `$SCHEMA$`.`VariableTrace` (`methodId` ASC, `variableId` ASC) ;

CREATE INDEX `fk_LocalTrace_InvocationTrace_idx` ON `$SCHEMA$`.`VariableTrace` (`testId` ASC, `callStep` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`PutTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`PutTrace` (
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
    REFERENCES `$SCHEMA$`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_PutTrace_Field`
    FOREIGN KEY (`fieldId` )
    REFERENCES `$SCHEMA$`.`Field` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_PutTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_FieldTrace_Field_idx` ON `$SCHEMA$`.`PutTrace` (`fieldId` ASC) ;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `$SCHEMA$`.`PutTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `$SCHEMA$`.`PutTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`ThrowTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`ThrowTrace` (
  `testId` INT NOT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `exceptionId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_ThrowTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ThrowTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `exceptionId` )
    REFERENCES `$SCHEMA$`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_ThrowTrace_ObjectTrace_idx` ON `$SCHEMA$`.`ThrowTrace` (`testId` ASC, `exceptionId` ASC) ;

CREATE INDEX `fk_ThrowTrace_InvocationTrace_idx` ON `$SCHEMA$`.`ThrowTrace` (`testId` ASC, `callStep` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`CatchTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`CatchTrace` (
  `testId` INT NOT NULL ,
  `callStep` BIGINT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `exceptionId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `callStep`, `step`) ,
  CONSTRAINT `fk_CatchTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_CatchTrace_ObjectTrace`
    FOREIGN KEY (`testId` , `exceptionId` )
    REFERENCES `$SCHEMA$`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_CatchTrace_ObjectTrace_idx` ON `$SCHEMA$`.`CatchTrace` (`testId` ASC, `exceptionId` ASC) ;

CREATE INDEX `fk_CatchTrace_InvocationTrace_idx` ON `$SCHEMA$`.`CatchTrace` (`testId` ASC, `callStep` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`ExitTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`ExitTrace` (
  `testId` INT NOT NULL ,
  `step` BIGINT NOT NULL ,
  `returned` TINYINT(1) NOT NULL ,
  `primType` CHAR NOT NULL ,
  `valueId` BIGINT NOT NULL ,
  `line` INT NULL ,
  PRIMARY KEY (`testId`, `step`)
-- ,
--  CONSTRAINT `fk_ExitTrace_CallTrace`
--    FOREIGN KEY (`testId` , `callStep` )
--    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
--    ON DELETE NO ACTION
--    ON UPDATE NO ACTION
)
ENGINE = MyISAM;

CREATE INDEX `fk_ExitTrace_CallTrace_idx` ON `$SCHEMA$`.`ExitTrace` (`testId` ASC, `step` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`GetTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`GetTrace` (
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
    REFERENCES `$SCHEMA$`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_GetTrace_Field`
    FOREIGN KEY (`fieldId` )
    REFERENCES `$SCHEMA$`.`Field` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_GetTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_FieldTrace_Field_idx` ON `$SCHEMA$`.`GetTrace` (`fieldId` ASC) ;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `$SCHEMA$`.`GetTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `$SCHEMA$`.`GetTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`ArrayPutTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`ArrayPutTrace` (
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
    REFERENCES `$SCHEMA$`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ArrayPutTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `$SCHEMA$`.`ArrayPutTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `$SCHEMA$`.`ArrayPutTrace` (`testId` ASC, `thisId` ASC) ;


-- -----------------------------------------------------
-- Table `$SCHEMA$`.`ArrayGetTrace`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `$SCHEMA$`.`ArrayGetTrace` (
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
    REFERENCES `$SCHEMA$`.`ObjectTrace` (`testId` , `id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ArrayGetTrace_CallTrace`
    FOREIGN KEY (`testId` , `callStep` )
    REFERENCES `$SCHEMA$`.`CallTrace` (`testId` , `step` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = MyISAM;

CREATE INDEX `fk_FieldTrace_InvocationTrace_idx` ON `$SCHEMA$`.`ArrayGetTrace` (`testId` ASC, `callStep` ASC) ;

CREATE INDEX `fk_FieldTrace_ObjectTrace_idx` ON `$SCHEMA$`.`ArrayGetTrace` (`testId` ASC, `thisId` ASC) ;

SET SQL_MODE=@OLD_SQL_MODE;

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
