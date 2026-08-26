/*
 Navicat Premium Data Transfer

 Source Server         : c5_ciyon
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : schema_mysql_web

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 24/08/2026 21:33:46
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for article_collections
-- ----------------------------
DROP TABLE IF EXISTS `article_collections`;
CREATE TABLE `article_collections`  (
  `collection_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `article_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`collection_id`) USING BTREE,
  UNIQUE INDEX `uk_user_article`(`user_id` ASC, `article_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `article_id`(`article_id` ASC) USING BTREE,
  CONSTRAINT `article_collections_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `article_collections_ibfk_2` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of article_collections
-- ----------------------------
INSERT INTO `article_collections` VALUES (1, 3, 7);

-- ----------------------------
-- Table structure for articles
-- ----------------------------
DROP TABLE IF EXISTS `articles`;
CREATE TABLE `articles`  (
  `article_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `views` int NULL DEFAULT 0,
  PRIMARY KEY (`article_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of articles
-- ----------------------------
INSERT INTO `articles` VALUES (1, '糖尿病饮食指南：这5种食物要少吃', 'img/a1.jpg', '张医生', '2026-08-19 08:55:13', '糖尿病患者在饮食上需要特别注意，以下5种食物尽量少吃：1. 含糖饮料；2. 精制白米面；3. 油炸食品；4. 高糖水果；5. 加工肉制品。合理饮食有助于控制血糖。', '饮食', 1024);
INSERT INTO `articles` VALUES (2, '如何正确监测血糖？', 'img/a2.jpg', '李医生', '2026-08-19 08:55:13', '正确监测血糖是糖尿病管理的重要环节。建议选择正规血糖仪，注意采血部位轮换，记录每次测量结果，并在复诊时带上记录，方便医生调整方案。', '日常护理', 856);
INSERT INTO `articles` VALUES (3, '1型糖尿病与2型糖尿病的区别', 'img/a1.jpg', '王医生', '2026-08-19 08:55:13', '1型糖尿病多发于青少年，胰岛β细胞被破坏，需依赖胰岛素治疗；2型糖尿病多见于中老年，与胰岛素抵抗相关，可通过口服药物和生活方式干预。两者病因和治疗方法都有明显区别。', '科普', 320);
INSERT INTO `articles` VALUES (4, '运动对糖尿病患者的益处', 'img/a3.jpg', '赵医生', '2026-08-19 08:55:13', '规律运动能帮助糖尿病患者控制体重、降低血糖、改善胰岛素敏感性。建议每周至少进行150分钟中等强度有氧运动，如快走、慢跑、游泳等，运动前后注意监测血糖。', '运动', 679);
INSERT INTO `articles` VALUES (5, '妊娠期糖尿病的注意事项', 'img/a1.jpg', '刘医生', '2026-08-19 08:55:13', '妊娠期糖尿病会影响母婴健康，要注意控制饮食总热量，少食多餐，适当运动，并按要进行血糖监测。必要时在医生指导下使用胰岛素治疗，产后也需复查血糖。', '特殊人群', 453);
INSERT INTO `articles` VALUES (6, '糖尿病足部护理小贴士', 'img/a3.jpg', '陈医生', '2026-08-19 08:55:13', '糖尿病患者要每天检查双足是否有破损、水泡或红肿，保持足部清洁干燥，穿宽松舒适的鞋子。如发现异常应及时就医，避免小伤口发展成严重感染。', '日常护理', 235);
INSERT INTO `articles` VALUES (7, '糖尿病预防：从生活细节做起，远离“甜蜜的负担”', '/img/a1.jpg', '无名医学人', '2026-08-22 20:35:00', '<p>糖尿病是一种以高血糖为特征的代谢性疾病，长期高血糖会损害心、脑、肾、眼等多种器官。预防糖尿病，关键在于养成健康的生活方式，从日常细节入手，才能有效降低患病风险。</p><h3>一、合理饮食，控制总热量</h3><p>饮食是预防糖尿病的基石。建议减少高糖、高脂、高盐食物的摄入，如甜饮料、油炸食品、加工肉类。主食优选全谷物、杂豆和薯类，它们富含膳食纤维，能延缓血糖上升。每餐保证适量优质蛋白（鱼、禽、蛋、豆制品），多吃新鲜蔬菜，特别是绿叶蔬菜。进餐顺序可先吃蔬菜、再吃肉蛋、最后吃主食，有助于平稳餐后血糖。</p><h3>二、坚持运动，保持健康体重</h3><p>超重和肥胖是糖尿病最重要的危险因素。每周至少进行150分钟中等强度有氧运动（如快走、慢跑、游泳），并结合2-3次力量训练（如深蹲、俯卧撑）。运动可提高胰岛素敏感性，帮助控制体重。饭后1小时运动效果更佳，但避免空腹剧烈运动。</p><h3>三、规律作息，减轻精神压力</h3><p>长期熬夜、睡眠不足会导致体内皮质醇等升糖激素分泌增加，增加糖尿病风险。建议保证每晚7-8小时睡眠，避免睡前使用电子产品。同时，学会通过冥想、深呼吸、社交等方式缓解压力，避免长期焦虑。</p><h3>四、定期体检，早发现早干预</h3><p>有糖尿病家族史、超重、高血压、高血脂等高危人群，应每年检测一次空腹血糖和糖化血红蛋白。若发现血糖异常，及时寻求专业医生指导，通过生活方式干预或必要时的药物干预，完全有可能推迟或预防糖尿病的发生。</p><p>糖尿病预防不是一蹴而就，而是融入日常的点点滴滴。从今天开始，管住嘴、迈开腿、睡好觉、放宽心，为自己和家人筑起一道健康的防线。</p>', '科普', 2);
INSERT INTO `articles` VALUES (8, '糖尿病科普：科学管理血糖的5个关键点', '/img/a3.jpg', '孙医生', '2026-08-23 09:44:12', '糖尿病是一种以高血糖为特征的慢性代谢性疾病，长期高血糖会损害心、脑、肾、眼等重要器官。科学管理血糖是糖尿病治疗的核心目标。\n\n1. 合理饮食：控制总热量摄入，减少精制糖和饱和脂肪，增加膳食纤维；\n2. 规律运动：每周至少150分钟中等强度有氧运动；\n3. 规范用药：遵医嘱按时服药或注射胰岛素，不随意停药；\n4. 定期监测：定期检测空腹血糖、餐后血糖和糖化血红蛋白；\n5. 健康生活方式：戒烟限酒，保持良好睡眠，管理情绪压力。\n\n做到以上几点，糖尿病患者同样可以拥有高质量的生活。', '科普', 0);

-- ----------------------------
-- Table structure for diabetes_types
-- ----------------------------
DROP TABLE IF EXISTS `diabetes_types`;
CREATE TABLE `diabetes_types`  (
  `type_id` int NOT NULL AUTO_INCREMENT,
  `type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `pathogenesis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `manifestation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `treatment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  PRIMARY KEY (`type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of diabetes_types
-- ----------------------------

-- ----------------------------
-- Table structure for doctor_information
-- ----------------------------
DROP TABLE IF EXISTS `doctor_information`;
CREATE TABLE `doctor_information`  (
  `info_id` int NOT NULL AUTO_INCREMENT,
  `doctor_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `department` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `chat_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0正常 1已删除',
  PRIMARY KEY (`info_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of doctor_information
-- ----------------------------
INSERT INTO `doctor_information` VALUES (1, '张明华', '内分泌科', '主任医师', '2型糖尿病个体化治疗', '/img/doc1.jpg', 'app-Ao5sIcq1RmPJifG2Cp1mlQ4w', 1);
INSERT INTO `doctor_information` VALUES (2, '李秀芬', '内分泌科', '副主任医师', '糖尿病前期干预、妊娠糖尿病', '/img/doc2.png', 'app-Ao5sIcq1RmPJifG2Cp1mlQ4w', 0);
INSERT INTO `doctor_information` VALUES (3, '王建国', '内分泌科', '主任医师', '1型糖尿病、糖尿病肾病', '/img/doc3.png', 'app-Ao5sIcq1RmPJifG2Cp1mlQ4w', 0);
INSERT INTO `doctor_information` VALUES (4, '陈雅琴', '营养科', '副主任医师', '糖尿病医学营养治疗', '/img/doc2.png', 'app-Ao5sIcq1RmPJifG2Cp1mlQ4w', 0);
INSERT INTO `doctor_information` VALUES (5, '刘志远', '内分泌科', '主治医师', '青少年糖尿病、动态血糖监测', '/img/doc3.png', 'app-Ao5sIcq1RmPJifG2Cp1mlQ4w', 0);
INSERT INTO `doctor_information` VALUES (6, '张明华', '内分泌科', '主任', '超级牛逼的专家', '/img/doc1.jpg', 'app-Ao5sIcq1RmPJifG2Cp1mlQ4w', 0);

-- ----------------------------
-- Table structure for life_advice
-- ----------------------------
DROP TABLE IF EXISTS `life_advice`;
CREATE TABLE `life_advice`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of life_advice
-- ----------------------------
INSERT INTO `life_advice` VALUES (1, 3, '控制总热量，优化餐盘结构', '饮食建议', '根据您的身高体重，建议每天减少500-800千卡热量摄入。每餐按“蔬菜-肉类-主食”顺序进食，先吃半碗绿叶蔬菜，再吃手掌大小的瘦肉或鱼虾，最后吃一拳头杂粮饭。彻底戒掉含糖饮料、奶茶和果汁，避免油炸食品和糕点。');
INSERT INTO `life_advice` VALUES (2, 3, '用低GI主食替代精米白面', '饮食建议', '将白米饭、馒头、面条换成糙米、燕麦、荞麦或玉米等低升糖指数主食，每餐主食量控制在一拳头大小。早餐推荐无糖燕麦加鸡蛋，午餐杂粮饭配清蒸鱼和蔬菜，晚餐可用红薯或豆腐替代部分主食，既增加饱腹感又避免血糖快速升高。');
INSERT INTO `life_advice` VALUES (3, 3, '低冲击有氧，保护关节', '运动建议', '因体重基数较大，暂时避免跑步、跳绳等冲击性运动。建议从每天快走30分钟开始，速度以微微出汗、能说话但不宜唱歌为宜。每周至少5天，可逐渐延长至40分钟。若有条件，游泳或椭圆机对膝盖更友好，每次40-50分钟。');
INSERT INTO `life_advice` VALUES (4, 3, '加入抗阻训练，提升代谢', '运动建议', '每周进行2-3次力量训练，使用弹力带或小哑铃，重点锻炼胸、背、腿等大肌群，如深蹲、俯卧撑（可跪姿）、哑铃划船等。每次20-30分钟，组间休息1分钟。抗阻训练能增加肌肉量，提高基础代谢，帮助长期控糖和减重。');
INSERT INTO `life_advice` VALUES (5, 3, '定期监测体重和腰围', '日常提醒', '每周固定一天早上空腹称体重，并测量腰围，记录在手机备忘录中。建议减重目标为每月2-4公斤，避免快速减重。若体重连续2周不降反升，或出现口渴、多尿、乏力等症状，请及时就医筛查血糖，做到早发现早干预。');
INSERT INTO `life_advice` VALUES (6, 3, '规律作息，充足睡眠', '日常提醒', '长期睡眠不足会增加胰岛素抵抗和肥胖风险。请尽量晚上11点前入睡，保证7-8小时睡眠。每天饮水2升以上，不喝含糖饮料。避免久坐，每坐1小时起身活动3分钟，有助于改善代谢。同时，保持心情平稳，减少压力性暴饮暴食。');
INSERT INTO `life_advice` VALUES (7, 3, '认识您的BMI：极重度肥胖风险', '科普知识', '您的BMI约为43.6，属于极重度肥胖（正常为18.5-23.9）。肥胖是2型糖尿病、高血压和血脂异常的重要诱因。即使目前未确诊，胰岛素抵抗可能已经存在。临床研究显示，减重5%-10%即可显著改善胰岛素敏感性，降低糖尿病发生风险。');
INSERT INTO `life_advice` VALUES (8, 3, '腰围与健康风险的关系', '科普知识', '您的腰围83.66厘米，虽然未达到成人中心型肥胖的男性标准90厘米，但结合极低的BMI和身高，内脏脂肪可能仍然超标。内脏脂肪比皮下脂肪更易引发代谢异常。建议通过饮食和运动优先减少腹部脂肪，这比单纯降体重更能改善血糖和血脂水平。');

-- ----------------------------
-- Table structure for life_plans
-- ----------------------------
DROP TABLE IF EXISTS `life_plans`;
CREATE TABLE `life_plans`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `order` int NOT NULL,
  `time` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  CONSTRAINT `life_plans_chk_1` CHECK (`type` in (_utf8mb4'饮食',_utf8mb4'运动',_utf8mb4'其他'))
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of life_plans
-- ----------------------------
INSERT INTO `life_plans` VALUES (1, 1, '运动', 1, '7:00-7:30', '晨练运动', '轻度有氧拉伸与快走，每周三次，注意心率控制在最大心率的60%-70%，如有关节不适请改为游泳或骑行。');
INSERT INTO `life_plans` VALUES (2, 1, '运动', 2, '18:30-19:00', '晚间运动', '晚饭后一小时进行舒缓瑜伽或平衡训练，每次二十分钟，帮助放松肌肉，改善睡眠。如有高血压请避免高难度倒立体式。');
INSERT INTO `life_plans` VALUES (3, 1, '运动', 3, '8:30-10:00', '周末户外运动', '周六上午进行健走或慢跑，总时长约一个半小时，量力而行，中间休息十分钟；如膝盖压力大，可选择骑车或划船机。周日以休息和散步为主，避免连续高强度训练。');
INSERT INTO `life_plans` VALUES (4, 1, '饮食', 1, '7:30-8:00', '早餐建议', '一杯牛奶、两片全麦面包和一个苹果。');
INSERT INTO `life_plans` VALUES (5, 1, '饮食', 2, '12:00-13:00', '午餐建议', '一份糙米饭、清蒸鱼和西兰花炒胡萝卜。');
INSERT INTO `life_plans` VALUES (6, 1, '饮食', 3, '18:00-19:00', '晚餐建议', '一碗杂粮粥、凉拌鸡胸肉和炒青菜。');
INSERT INTO `life_plans` VALUES (7, 1, '饮食', 4, '15:30-16:00', '加餐建议', '一小把坚果和一杯无糖酸奶。');
INSERT INTO `life_plans` VALUES (52, 3, '运动', 1, '7:00-7:30', '晨间拉伸与快走', '起床后先进行5分钟动态拉伸，再快走25分钟，心率控制在110-130次/分。一周三次，如遇膝盖不适改为游泳或骑车。');
INSERT INTO `life_plans` VALUES (53, 3, '运动', 2, '19:00-19:40', '晚间力量与平衡训练', '晚餐后1小时进行靠墙静蹲、臀桥、单腿站立各3组，每组8-12次；配合10分钟核心平板支撑。每周两次，注意腰背挺直，避免过度负荷。');
INSERT INTO `life_plans` VALUES (54, 3, '运动', 3, '周末 8:30-10:00', '户外有氧与郊野活动', '周末选择一次60-90分钟的中低强度运动，如骑行、登山或快走。注意补充水分，穿着防滑鞋。如有高血压或心血管问题，改为平地步行为主，避免剧烈爬坡。');
INSERT INTO `life_plans` VALUES (55, 3, '运动', 4, '周末 15:30-17:00', '休闲球类或游泳', '周末下午安排一次团体运动（羽毛球、乒乓球）或游泳40分钟，增强心肺功能并放松身心。注意运动前后充分热身和冷身，若有关节问题避免急停急转动作。');
INSERT INTO `life_plans` VALUES (56, 3, '饮食', 1, '7:30-8:00', '早餐建议', '一杯无糖豆浆（250ml）、一个水煮蛋、两片全麦面包和一小份凉拌黄瓜。');
INSERT INTO `life_plans` VALUES (57, 3, '饮食', 2, '12:00-12:30', '午餐建议', '杂粮饭（约150g）、清蒸鱼（100g）、炒西兰花（150g）和一碗紫菜蛋花汤，少油少盐。');
INSERT INTO `life_plans` VALUES (58, 3, '饮食', 3, '15:00-15:30', '加餐建议', '一小把原味坚果（约10g）和一个中等大小的苹果。');
INSERT INTO `life_plans` VALUES (59, 3, '饮食', 4, '18:00-18:30', '晚餐建议', '小米粥（一小碗）、鸡胸肉（80g）凉拌菠菜（200g）和少量蒸南瓜，避免油炸和油腻食物。');

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` int NULL DEFAULT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` int NULL DEFAULT NULL,
  `detail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_operator`(`operator_id` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (1, 3, 'lzy', 'doctor', 'delete', 1, '软删除医生 ID：1', '2026-08-22 20:46:13');
INSERT INTO `operation_log` VALUES (2, 3, 'lzy', 'doctor', 'create', 6, '新增医生：张明华', '2026-08-22 21:14:55');
INSERT INTO `operation_log` VALUES (3, 3, 'lzy', 'doctor', 'update', 6, '修改医生：张明华', '2026-08-22 21:15:30');
INSERT INTO `operation_log` VALUES (4, 3, 'lzy', 'doctor', 'delete', 6, '软删除医生 ID：6', '2026-08-22 21:15:39');
INSERT INTO `operation_log` VALUES (5, 3, 'lzy', 'doctor', 'restore', 6, '恢复医生 ID：6', '2026-08-23 09:40:17');
INSERT INTO `operation_log` VALUES (6, 3, 'lzy', 'article', 'update', 3, '修改文章：1型糖尿病与2型糖尿病的区别', '2026-08-23 09:44:33');
INSERT INTO `operation_log` VALUES (7, 3, 'lzy', 'article', 'update', 8, '修改文章：糖尿病科普：科学管理血糖的5个关键点', '2026-08-23 09:44:49');

-- ----------------------------
-- Table structure for punch_in
-- ----------------------------
DROP TABLE IF EXISTS `punch_in`;
CREATE TABLE `punch_in`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `punch_time` datetime NOT NULL,
  `punch_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `completion_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of punch_in
-- ----------------------------
INSERT INTO `punch_in` VALUES (8, 3, '2026-08-22 20:02:46', '饮食', '已完成', '吃10碗饭');

-- ----------------------------
-- Table structure for user_ai_config
-- ----------------------------
DROP TABLE IF EXISTS `user_ai_config`;
CREATE TABLE `user_ai_config`  (
  `user_id` int NOT NULL COMMENT '用户ID（关联 users.user_id）',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'OpenAI API Key（用户自带，云端大模型调用密钥）',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API Base URL（OpenAI 兼容接口，为空用系统默认）',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型名称（为空用系统默认）',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `user_ai_config_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_ai_config
-- ----------------------------
INSERT INTO `user_ai_config` VALUES (3, '6+4sMcBJZ4F15gbDqcqCfCuNMCZ921UF9aAn1KQmRl7Pbf3x1l809BkUPl71wjdpoC40622M1NV+ZPmP8ZaHyvnNkbW+sktd+OYVUOERUQ==', 'https://tokenhub.tencentmaas.com/v1', 'deepseek-v4-flash-202605', '2026-08-24 21:32:26');
INSERT INTO `user_ai_config` VALUES (4, 'sk-JPPFBaJXIumMwSi9H4qbUbWCBGrTgHBOgK3yIaRZTTgHwFh4', 'https://tokenhub.tencentmaas.com/v1', 'deepseek-v4-flash-202605', '2026-08-24 16:47:51');

-- ----------------------------
-- Table structure for user_risk_info
-- ----------------------------
DROP TABLE IF EXISTS `user_risk_info`;
CREATE TABLE `user_risk_info`  (
  `userId` int NOT NULL AUTO_INCREMENT,
  `age` int NULL DEFAULT NULL,
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `height` double NULL DEFAULT NULL,
  `weight` double NULL DEFAULT NULL,
  `familyHistory` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `waistline` double NULL DEFAULT NULL,
  `systolicPressure` double NULL DEFAULT NULL,
  `isPregnancy` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `disease` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `diabetesType` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '糖尿病类型',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`userId`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_risk_info
-- ----------------------------
INSERT INTO `user_risk_info` VALUES (1, 45, '男', 165, 98, '爷爷有糖尿病', 186.09, 135, '否', '1787104563227.result', '无', NULL, NULL);
INSERT INTO `user_risk_info` VALUES (2, 28, '男', 176, 82, '爷爷患有糖尿病', 119.71, 125, '否', '【低风险】您的糖尿病风险评分为22分，低于25分，目前属于低风险人群。建议您保持健康生活方式，定期体检，关注血糖、血压和体重变化。您的腰围较大，建议适当控制体重，增加运动，减少腹部脂肪堆积。', '否', NULL, NULL);
INSERT INTO `user_risk_info` VALUES (3, 28, '男', 150, 98, '否', 83.66, 115, '否', '【低风险】“风险较低，但建议定期体检并保持健康生活方式。”]', '否', NULL, '2026-08-21 19:31:13');
INSERT INTO `user_risk_info` VALUES (4, 50, '男', 182, 58, '否', 85.54, 115, '否', '【低风险】您的糖尿病风险评分为23分，低于25分，属于低风险人群。建议保持健康生活方式，定期体检，并关注体重和腰围变化。', '否', NULL, '2026-08-21 19:37:36');
INSERT INTO `user_risk_info` VALUES (9, 52, '男', 172, 78, '有', 92, 138, '否', NULL, '是', '2型糖尿病', '2026-08-19 16:50:08');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个人简介',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0正常 1已删除',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'tester01', '一号用户', '智慧控糖', '$2a$10$AiccUCHuyAvfy92iv.n9wewJfzuiRORv.EzOLc/mPMv3HJU9llv0i', '/img/user_icon.png', 'user', '2026-08-12 15:30:55', 0);
INSERT INTO `users` VALUES (2, 'tester02', '二号测试', NULL, '$2a$10$VD2G4DgXW3bqKRbFxx2NvuPca4dfUaf4sbvT77vX/7rUSK/o0wDrS', '/img/user_icon.png', 'user', '2026-08-12 15:32:02', 0);
INSERT INTO `users` VALUES (3, 'lzy', 'lzy', NULL, '$2a$10$eG6BzAEr7pFWoiVFU6pWm.3Er3lk1PLRs5fMcVH7gMXguWoXY5kqO', '/img/user_icon.png', 'admin', '2026-08-12 15:43:07', 0);
INSERT INTO `users` VALUES (4, 'hzp', 'hzp', '', '$2a$10$LwSVkHyUAd7JxV8XcMDw6.B88R2A2U7BAOxhiGP/VzVwfs/0jzwve', '/img/user1.png', 'user', '2026-08-12 16:39:28', 0);
INSERT INTO `users` VALUES (5, 'test', '测试用户', NULL, '$2a$10$hBzPCCjF/Z1AkiNtcoZkAeXL6d4N8NWN0zU/HL40ExczmRpf80yVu', '/img/user_icon.png', 'user', '2026-08-13 11:38:07', 0);
INSERT INTO `users` VALUES (6, 'addUser', 'addUser', NULL, '$2a$10$.iachUCJnoXPENNZHI5pC.5OBdE3ymA1aDfezicQjPGCEuNph3bwi', '/img/user_icon.png', 'user', '2026-08-15 19:25:36', 0);
INSERT INTO `users` VALUES (7, 'mcptest', 'mcptest', NULL, '123456', NULL, 'admin', '2026-08-18 21:43:14', 0);
INSERT INTO `users` VALUES (9, 'apitest6738', 'apitest', NULL, '$2a$10$tGLhUAXs2.FBVfv6zn3GPuG8n3ZOtXugqSI/jxvuhnvLe6Nqk6qgi', '/img/user_icon.png', 'user', '2026-08-19 16:50:08', 0);
INSERT INTO `users` VALUES (10, 'apitest3680', 'apitest', NULL, '$2a$10$0L4Ad6dfEmIB22R.Ok4tIOoPX9ufsQ4JpvWA9slz4W50oIq3Jn2ki', '/img/user_icon.png', 'user', '2026-08-19 16:50:20', 0);
INSERT INTO `users` VALUES (11, 'apitest7482', 'apitest', NULL, '$2a$10$EMLOZjTRVNXAGpc7ElJn6.wmyyuB.YlL6gni8bBDnRd2I3uHXdMGy', '/img/user_icon.png', 'user', '2026-08-19 16:53:25', 0);
INSERT INTO `users` VALUES (12, 'jwq', '麻辣小麻花', NULL, '$2a$10$vCUC2zN6A7OePtxqtiF13uvMPg5ke1M5nz6.yt2Jrd9Ue3Xc.7f8S', '/img/user_icon.png', 'user', '2026-08-19 20:02:01', 0);
INSERT INTO `users` VALUES (13, 'sse_test9', 'SSE娴嬭瘯', NULL, '$2a$10$BtAWWS.Gelc.Tf/AfxLC..VniJ3MZzrr0uKxFwZkDX0pwEXt/kh.a', '/img/user_icon.png', 'user', '2026-08-21 20:57:27', 0);
INSERT INTO `users` VALUES (14, 'cb_test_ai', 'cb_test_ai', NULL, '$2a$10$MRCk8PKAAa8rezNvhJtJQeYav20RbV9QqS3/O5dRbM/Jz/En.648C', '/img/user_icon.png', 'user', '2026-08-21 21:32:01', 0);

SET FOREIGN_KEY_CHECKS = 1;
