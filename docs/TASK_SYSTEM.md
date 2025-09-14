# Task System - SQL-based Schema & API

## Overview

Hệ thống Task được refactor từ hardcode sang SQL-based với các tính năng:
- Quản lý task requirements và rewards riêng biệt
- Support nhiều loại requirements: KILL_MOB, KILL_BOSS, TALK_NPC, PICK_ITEM, GO_TO_MAP
- Flexible map restrictions và conditions
- Cache system cho performance
- Debug logging chi tiết

---

## Database Schema

### 1. `task_main_template` - Tasks chính (đã có sẵn)
```sql
CREATE TABLE task_main_template (
    id INT PRIMARY KEY,
    NAME VARCHAR(255) NOT NULL,               -- Tên task chính
    detail VARCHAR(255) NOT NULL              -- Mô tả chi tiết
);
```

### 2. `task_sub_template` - Sub tasks (đã có sẵn)
```sql
CREATE TABLE task_sub_template (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_main_id INT NOT NULL,                -- FK to task_main_template
    NAME VARCHAR(255) NOT NULL,               -- Tên sub task
    max_count INT NOT NULL DEFAULT -1,        -- Số lượng cần hoàn thành
    notify VARCHAR(255) NOT NULL DEFAULT '',  -- Thông báo khi hoàn thành
    npc_id INT NOT NULL DEFAULT -1,           -- NPC liên quan
    map INT NOT NULL,                         -- Map của task
    FOREIGN KEY (task_main_id) REFERENCES task_main_template(id)
);
```

### 3. `task_requirements` - Yêu cầu task (mới)
```sql
CREATE TABLE task_requirements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_main_id INT NOT NULL,                -- FK to task_main_template
    task_sub_id INT NOT NULL,                 -- Sub task index (0,1,2...)
    requirement_type ENUM('KILL_MOB', 'KILL_BOSS', 'TALK_NPC', 'PICK_ITEM', 'GO_TO_MAP', 'USE_ITEM') NOT NULL,
    target_id INT NOT NULL,                   -- mob_id, boss_id, npc_id, item_id, map_id
    target_count INT NOT NULL DEFAULT 1,      -- Số lượng cần hoàn thành
    map_restriction VARCHAR(100),             -- Map nào được tính (null = all maps)
    extra_data JSON,                          -- Data thêm nếu cần
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_task (task_main_id, task_sub_id),
    INDEX idx_type (requirement_type),
    INDEX idx_target (target_id)
);
```

### 4. `task_rewards` - Phần thưởng task (mới)
```sql
CREATE TABLE task_rewards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_main_id INT NOT NULL,                -- FK to task_main_template
    task_sub_id INT NOT NULL,                 -- Sub task index (0,1,2...)
    reward_type ENUM('ITEM', 'GOLD', 'EXP', 'RUBY', 'POWER_POINT') NOT NULL,
    reward_id INT DEFAULT 0,                  -- item_id (nếu là item)
    reward_quantity BIGINT NOT NULL DEFAULT 1, -- Số lượng reward
    reward_description VARCHAR(200),          -- Mô tả reward
    INDEX idx_task (task_main_id, task_sub_id),
    INDEX idx_type (reward_type)
);
```

---

## Requirement Types

### 1. `KILL_MOB` - Giết quái
- **target_id**: `mob.tempId` (0=Khỉ Bư, 1=Sói, 2=Thay Ma, etc.)
- **target_count**: Số lượng cần giết
- **map_restriction**: Map nào được tính ("3", "1-5", "!10", etc.)

### 2. `KILL_BOSS` - Giết boss
- **target_id**: `boss.id` 
- **target_count**: Số lượng boss cần giết
- **map_restriction**: Map restriction nếu cần

### 3. `TALK_NPC` - Nói chuyện với NPC
- **target_id**: `npc.tempId` (0=Rock, 105=Hướng dẫn, etc.)
- **target_count**: Thường là 1
- **map_restriction**: Map nào có NPC

### 4. `PICK_ITEM` - Nhặt item
- **target_id**: `item.templateId`
- **target_count**: Số lượng item cần nhặt
- **map_restriction**: Map nào được tính

### 5. `GO_TO_MAP` - Đi đến map
- **target_id**: `mapId`
- **target_count**: Thường là 1
- **map_restriction**: Không dùng

### 6. `USE_ITEM` - Sử dụng item
- **target_id**: `item.templateId`
- **target_count**: Số lượng cần dùng

---

## Reward Types

### 1. `ITEM` - Vật phẩm
- **reward_id**: `item.templateId`
- **reward_quantity**: Số lượng item
- **reward_description**: "Nhận được X vật phẩm Y"

### 2. `GOLD` - Vàng
- **reward_id**: 0 (không dùng)
- **reward_quantity**: Số vàng
- **reward_description**: "Nhận được X vàng"

### 3. `EXP` - Kinh nghiệm
- **reward_id**: 0 (không dùng)  
- **reward_quantity**: Số EXP
- **reward_description**: "Nhận được X kinh nghiệm"

### 4. `RUBY` - Hồng ngọc
- **reward_id**: 0 (không dùng)
- **reward_quantity**: Số hồng ngọc
- **reward_description**: "Nhận được X hồng ngọc"

---

## Map Restrictions

### Syntax hỗ trợ:
- **`"3"`** - Chỉ map 3
- **`"1,2,3"`** - Map 1 hoặc 2 hoặc 3
- **`"1-5"`** - Map từ 1 đến 5
- **`"1-3,10,15-20"`** - Map 1-3, map 10, map 15-20
- **`"!10"`** - Tất cả map trừ map 10
- **`"!5-10"`** - Tất cả map trừ map 5-10
- **`null`** - Không giới hạn map

---

## Sample Data

### Task 14: Nhiệm vụ đầu tiên
```sql
-- Sub task 0: Gặp Rock ở map 1
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(14, 0, 'TALK_NPC', 0, 1, '1');

-- Sub task 1: Gặp Rock Rock ở map 2  
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(14, 1, 'TALK_NPC', 0, 1, '2');

-- Sub task 2: Giết 5 Khỉ Bư ở map 3
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(14, 2, 'KILL_MOB', 0, 5, '3');

-- Reward cho task 14_2
INSERT INTO task_rewards (task_main_id, task_sub_id, reward_type, reward_id, reward_quantity, reward_description) VALUES
(14, 2, 'EXP', 0, 1000, 'Hoàn thành nhiệm vụ giết Khỉ Bư');

-- Sub task 3: Quay về báo cáo Rock Rock
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(14, 3, 'TALK_NPC', 0, 1, '2');
```

### Task 15: Gặp NPCs
```sql
-- Sub task 0: Gặp Người Hướng Dẫn
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(15, 0, 'TALK_NPC', 105, 1, '2');

INSERT INTO task_rewards (task_main_id, task_sub_id, reward_type, reward_id, reward_quantity, reward_description) VALUES
(15, 0, 'EXP', 0, 500, 'Thưởng gặp NPC Hướng Dẫn');

-- Sub task 1-6: Gặp các NPCs khác
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(15, 1, 'TALK_NPC', 17, 1, '2'),   -- Bò Mộng
(15, 2, 'TALK_NPC', 16, 1, '3'),   -- Uron
(15, 3, 'TALK_NPC', 21, 1, '3'),   -- Bà Hạt Mít
(15, 4, 'TALK_NPC', 55, 1, '3'),   -- Berrus
(15, 5, 'TALK_NPC', 107, 1, '3'),  -- Thần Bí
(15, 6, 'TALK_NPC', 0, 1, '2');    -- Quay về Rock Rock
```

### Task 16: Combat tasks
```sql
-- Sub task 0: Gặp Berrus
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(16, 0, 'TALK_NPC', 55, 1, '3');

-- Sub task 1: Giết 50 Sói ở map 4
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(16, 1, 'KILL_MOB', 1, 50, '4');

INSERT INTO task_rewards (task_main_id, task_sub_id, reward_type, reward_id, reward_quantity, reward_description) VALUES
(16, 1, 'ITEM', 457, 5, 'Thưởng 5 Thỏi Vàng'),
(16, 1, 'GOLD', 0, 50000, 'Thưởng 50,000 vàng');

-- Sub task 2: Giết 100 Thay Ma ở map 4
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(16, 2, 'KILL_MOB', 2, 100, '4');
```

---

## API Usage

### Java Service
```java
// Task system sẽ tự động check khi player thực hiện actions
TaskServiceNew.getInstance().checkDoneTaskKillMob(player, mob);
TaskServiceNew.getInstance().checkDoneTaskKillBoss(player, boss);
TaskServiceNew.getInstance().checkDoneTaskTalkNpc(player, npc);
TaskServiceNew.getInstance().checkDoneTaskPickItem(player, item);
TaskServiceNew.getInstance().checkDoneTaskGoToMap(player, zone);
```

### Task Flow
1. **Player thực hiện action** (giết mob, gặp NPC, etc.)
2. **TaskServiceNew check requirements** từ cache
3. **Match requirement** với action (mob.tempId, npc.tempId, etc.)
4. **Check map restriction** nếu có
5. **Increment progress** cho requirement
6. **Complete task** nếu đủ target_count
7. **Give rewards** từ task_rewards table
8. **Move to next sub task**

### Debug Logs
```
TaskServiceNew: Player sdasd killed mob 0 at map 3
TaskServiceNew: Task requirement matched - TaskRequirement{task=14_2, type=KILL_MOB, target=0, count=5, map=3}
TaskServiceNew: Task progress 14_2: 4 + 1 = 5/5
TaskServiceNew: Task completed! TaskRequirement{task=14_2, type=KILL_MOB, target=0, count=5, map=3}
TaskServiceNew: Completing task 14_2 for player sdasd
TaskServiceNew: Giving reward TaskReward{task=14_2, type=EXP, id=0, quantity=1000} to player sdasd
```

---

## CLI Commands

### Task Cache Management
```bash
refreshtaskcache    # Refresh task cache từ database
taskcachestats      # Xem thống kê task cache
```

### Sample Output
```
TaskCache: Starting cache initialization...
TaskCache: Loading task requirements from database...
TaskCache: Loaded requirement: KILL_MOB target=0 count=5 for task 14_2
TaskCache: Loaded requirement: TALK_NPC target=0 count=1 for task 14_0
TaskCache: Successfully loaded 13 task requirements
TaskCache: Loading task rewards from database...
TaskCache: Loaded reward: EXP id=0 quantity=1000 for task 14_2
TaskCache: Successfully loaded 4 task rewards
TaskCache: Cache initialized successfully!
```

---

## Migration từ Hardcode

### Current Hardcode Tasks
```java
// Trong TaskService.java
case ConstMob.KHI_BU:
    if (mob.zone.map.mapId == 3) {
        doneTask(player, ConstTask.TASK_14_2);  // 28676
    }
    break;
case ConstMob.SOI:
    doneTask(player, ConstTask.TASK_16_1);      // 32770
    break;
```

### Migrated to SQL
```sql
-- TASK_14_2: Giết Khỉ Bư ở map 3
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(14, 2, 'KILL_MOB', 0, 5, '3');

-- TASK_16_1: Giết Sói ở map 4  
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(16, 1, 'KILL_MOB', 1, 50, '4');
```

---

## Example: Adding New Task

### 1. Tạo task mới trong database
```sql
-- Main task
INSERT INTO task_main_template (id, NAME, detail) VALUES
(17, 'Nhiệm Vụ Boss', 'Thử thách với các Boss mạnh');

-- Sub tasks
INSERT INTO task_sub_template (task_main_id, NAME, max_count, notify, npc_id, map) VALUES
(17, 'Gặp Boss Trainer', 1, 'Đã gặp trainer', 55, 3),
(17, 'Giết Boss Cấp Đấu Đế', 1, 'Đã giết boss', -1, -1);
```

### 2. Tạo requirements
```sql
INSERT INTO task_requirements (task_main_id, task_sub_id, requirement_type, target_id, target_count, map_restriction) VALUES
(17, 0, 'TALK_NPC', 55, 1, '3'),      -- Gặp Berrus
(17, 1, 'KILL_BOSS', 11, 1, NULL);    -- Giết Boss Cấp Đấu Đế
```

### 3. Tạo rewards
```sql
INSERT INTO task_rewards (task_main_id, task_sub_id, reward_type, reward_id, reward_quantity, reward_description) VALUES
(17, 1, 'ITEM', 1816, 1, 'Nhận được vật phẩm đặc biệt'),
(17, 1, 'GOLD', 0, 100000, 'Nhận được 100,000 vàng'),
(17, 1, 'EXP', 0, 5000, 'Nhận được 5,000 kinh nghiệm');
```

### 4. Refresh cache
```bash
refreshtaskcache
```

**Không cần code Java! Chỉ cần SQL!** 🎉

---

## Integration với hệ thống cũ

### Dual System Support
- **TaskService** (cũ): Vẫn hoạt động cho backward compatibility
- **TaskServiceNew** (mới): SQL-based system chạy song song
- **Gradual Migration**: Dần dần chuyển từ cũ sang mới

### Current Implementation
```java
// Trong Boss.java
TaskService.gI().checkDoneTaskKillBoss(plKill, this);          // Old system
TaskServiceNew.getInstance().checkDoneTaskKillBoss(plKill, this); // New system

// Trong Mob.java  
TaskService.gI().checkDoneTaskKillMob(plAtt, this);            // Old system
TaskServiceNew.getInstance().checkDoneTaskKillMob(plAtt, this);   // New system
```

---

## Performance Notes

### Cache System
- **TaskCache**: Load tất cả requirements và rewards vào memory
- **Key Format**: `"taskMainId_taskSubId"` để lookup nhanh
- **Thread-safe**: Sử dụng `ConcurrentHashMap`
- **Auto-refresh**: CLI commands để reload từ database

### Indexes
- **idx_task**: Fast lookup theo task_main_id + task_sub_id
- **idx_type**: Fast filter theo requirement_type
- **idx_target**: Fast lookup theo target_id

---

## Contact

- **Developer**: Ahwuocdz
- **Date Created**: September 14, 2025
- **Last Updated**: September 15, 2025
- **Version**: 1.0.0
