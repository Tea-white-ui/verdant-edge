/*
 Navicat Premium Dump SQL

 Source Server         : Tea-MYSQL95
 Source Server Type    : MySQL
 Source Server Version : 90500 (9.5.0)
 Source Host           : localhost:3306
 Source Schema         : verdant_edge

 Target Server Type    : MySQL
 Target Server Version : 90500 (9.5.0)
 File Encoding         : 65001

 Date: 16/04/2026 17:30:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户密码',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户手机号',
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for machine
-- ----------------------------
DROP TABLE IF EXISTS `machine`;
CREATE TABLE `machine`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `type` int NOT NULL COMMENT '设备类型',
  `number` int NOT NULL COMMENT '设备编号',
  `plant_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '植物名称',
  `plant_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '植物类型',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_machine_employe`(`employee_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for machine_camera
-- ----------------------------
DROP TABLE IF EXISTS `machine_camera`;
CREATE TABLE `machine_camera`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `machine_id` bigint NULL DEFAULT NULL COMMENT '设备id',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '摄影图像',
  `disease_result` int NULL DEFAULT NULL COMMENT '病害结果',
  `warning` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '警告内容',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备摄影表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for machine_record
-- ----------------------------
DROP TABLE IF EXISTS `machine_record`;
CREATE TABLE `machine_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `machine_id` bigint NOT NULL COMMENT '设备id',
  `soil_moisture` decimal(5, 2) NULL DEFAULT NULL COMMENT '土壤湿度 (%)',
  `soil_temp` decimal(5, 2) NULL DEFAULT NULL COMMENT '土壤温度 (°C)',
  `air_temp` decimal(5, 2) NULL DEFAULT NULL COMMENT '空气温度 (°C)',
  `air_humidity` decimal(5, 2) NULL DEFAULT NULL COMMENT '空气湿度 (%)',
  `co2` decimal(5, 2) NULL DEFAULT NULL COMMENT '二氧化碳浓度 (ppm)',
  `light_intensity` decimal(10, 2) NULL DEFAULT NULL COMMENT '光照强度 (Lux)',
  `growth_stage` int NULL DEFAULT NULL COMMENT '成长阶段',
  `warning` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '警告内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_record_machine`(`machine_id` ASC) USING BTREE,
  CONSTRAINT `fk_record_machine` FOREIGN KEY (`machine_id`) REFERENCES `machine` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
