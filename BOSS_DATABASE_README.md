# Boss Database README

This document explains the database schema, queries, and runtime flow for the Boss system in this codebase.

Relevant source files:
- `src/Dragon/jdbc/daos/BossDataService.java`
- `src/Dragon/models/boss/RefactoredBossManager.java`
- `src/Dragon/models/boss/Boss.java`
- `src/Dragon/models/skill/Skill.java`
- `src/Dragon/utils/SkillUtil.java`

## 1. Tables

### 1.1 bosses
Columns:
- `id` INT PRIMARY KEY
- `name` VARCHAR NOT NULL
- `gender` TINYINT NOT NULL
- `dame` DOUBLE NOT NULL
- `hp_json` TEXT NOT NULL               // JSON array of double (HP per level)
- `map_join_json` TEXT NOT NULL         // JSON array of int (map IDs)
- `seconds_rest` INT NOT NULL           // seconds to rest before respawn
- `type_appear` INT NOT NULL            // enum ordinal of `TypeAppear`
- `bosses_appear_together_json` TEXT    // JSON array of int (boss IDs that appear together), nullable
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE

Example DDL:
```sql
CREATE TABLE bosses (
  id INT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  gender TINYINT NOT NULL,
  dame DOUBLE NOT NULL,
  hp_json TEXT NOT NULL,
  map_join_json TEXT NOT NULL,
  seconds_rest INT NOT NULL,
  type_appear INT NOT NULL,
  bosses_appear_together_json TEXT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

### 1.2 boss_outfits
Columns (simplified):
- `id` INT AUTO_INCREMENT PRIMARY KEY
- `boss_id` INT NOT NULL REFERENCES bosses(id)
- `item_id` SMALLINT NOT NULL           // refers to `item_template.id`

Server will map `item_id` -> `ItemTemplate` (from table `item_template`) and use its `head`, `body`, `leg` values for boss appearance.

Example DDL:
```sql
CREATE TABLE boss_outfits (
  id INT AUTO_INCREMENT PRIMARY KEY,
  boss_id INT,
  item_id SMALLINT,
  FOREIGN KEY (boss_id) REFERENCES bosses(id)
);
```

### 1.3 boss_skills
Columns:
- `id` INT AUTO_INCREMENT PRIMARY KEY
- `boss_id` INT NOT NULL REFERENCES bosses(id)
- `skill_id` INT NOT NULL               // `Skill` template id (see Skill.java)
- `skill_level` INT NOT NULL            // 1..7
- `cooldown` INT NOT NULL               // milliseconds

Example DDL:
```sql
CREATE TABLE boss_skills (
  id INT AUTO_INCREMENT PRIMARY KEY,
  boss_id INT NOT NULL,
  skill_id INT NOT NULL,
  skill_level INT NOT NULL,
  cooldown INT NOT NULL,
  CONSTRAINT fk_boss_skills_boss FOREIGN KEY (boss_id) REFERENCES bosses(id)
);
```

### 1.4 boss_texts
Columns:
- `id` INT AUTO_INCREMENT PRIMARY KEY
- `boss_id` INT NOT NULL REFERENCES bosses(id)
- `text_type` VARCHAR(16) NOT NULL      // 'start' | 'middle' | 'end'
- `text_content` TEXT NOT NULL          // see prefix rules below
- `display_order` INT NOT NULL

Example DDL:
```sql
CREATE TABLE boss_texts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  boss_id INT NOT NULL,
  text_type VARCHAR(16) NOT NULL,
  text_content TEXT NOT NULL,
  display_order INT NOT NULL,
  CONSTRAINT fk_boss_texts_boss FOREIGN KEY (boss_id) REFERENCES bosses(id)
);
```

## 2. Queries used by the code

From `BossDataService.java`:

- Load all active bosses
```sql
SELECT * FROM bosses WHERE is_active = TRUE ORDER BY id;
```

- Load one boss by id
```sql
SELECT * FROM bosses WHERE id = ? AND is_active = TRUE;
```

- Load outfit (one row, simplified schema)
```sql
SELECT item_id
FROM boss_outfits
WHERE boss_id = ?
ORDER BY id
LIMIT 1;
```

- Load skills
```sql
SELECT skill_id, skill_level, cooldown FROM boss_skills WHERE boss_id = ? ORDER BY id;
```

- Load texts
```sql
SELECT text_type, text_content
FROM boss_texts
WHERE boss_id = ?
ORDER BY text_type, display_order;
```

- Insert a boss (partial example; outfit/skills/texts inserted separately)
```sql
INSERT INTO bosses (id, name, gender, dame, hp_json, map_join_json, seconds_rest, type_appear, bosses_appear_together_json)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
```

## 3. JSON formats

- `hp_json`: JSON array of numbers (double). Example: `[100000000, 120000000, 150000000]`.
- `map_join_json`: JSON array of integers (map ids). Example: `[9, 10, 11]`.
- `bosses_appear_together_json`: JSON array of integers (other boss IDs). Example: `[15, 16]` or `null`.

These are parsed via `org.json.simple` in `BossDataService` methods `parseJsonToDoubleArray` and `parseJsonToIntArray`.

## 4. Skill type mapping

Skill types are constants declared in `src/Dragon/models/skill/Skill.java`. `skill_id` in `boss_skills` should match these template ids.

Examples:
- DRAGON = 0
- KAMEJOKO = 1
- DEMON = 2
- MASENKO = 3
- GALICK = 4
- ANTOMIC = 5
- THAI_DUONG_HA_SAN = 6
- TRI_THUONG = 7
- TAI_TAO_NANG_LUONG = 8
- KAIOKEN = 9
- QUA_CAU_KENH_KHI = 10
- MAKANKOSAPPO = 11
- DE_TRUNG = 12
- BIEN_KHI = 13
- TU_SAT = 14
- KHIEN_NANG_LUONG = 19
- DICH_CHUYEN_TUC_THOI = 20
- HUYT_SAO = 21
- THOI_MIEN = 22
- TROI = 23
- SUPER_KAME = 24
- LIEN_HOAN_CHUONG = 25
- MA_PHONG_BA = 26
- SUPER_TRANFORMATION = 27
- EVOLUTION = 28
- PHAN_THAN = 29

Additional helpers in `src/Dragon/utils/SkillUtil.java`:
- `isUseSkillDam`, `isUseSkillChuong`, `isUseSkillDacBiet`
- `getTyleSkillAttack(Skill)` returns 0 (default), 1 (beam), 2 (heal)

## 5. Text prefix rules (boss_texts.text_content)

`Boss.chat(int prefix, String text)` interprets prefixes:
- `-1|<text>`: boss chats to map (self)
- `-2|<text>`: boss chats to a random player in the same zone (ignored if `zone == null`)
- `-3|<text>`: parent boss chats
- `N|<text>` (N >= 0): another boss in `bossAppearTogether[currentLevel][N]` chats

Store full string like `"-1|I have arrived!"` in `text_content`.

## 6. Runtime flow

- `RefactoredBossManager.loadBosses()` calls `BossDataService.loadAllBosses()` to obtain `BossData` for active bosses.
- For each `BossData`:
  - A `Boss` instance is created (see `createGenericBoss` logic).
  - `spawnBossToMaps` picks a map/zone from `map_join_json` and sets `boss.zoneFinal`.
  - Boss lifecycle proceeds: `RESPAWN -> JOIN_MAP -> CHAT_S -> ACTIVE`.
- Boss skills are turned into in-memory `int[][] skillTemp` and applied by `Boss.initSkill()`.

## 7. Example data inserts

### 7.1 Boss row
```sql
INSERT INTO bosses
(id, name, gender, dame, hp_json, map_join_json, seconds_rest, type_appear, bosses_appear_together_json, is_active)
VALUES
(14, 'Ke Ngoai Toc', 0, 150000.0, '[1.0E8, 1.2E8, 1.5E8]', '[9,10,11]', 60, 0, null, TRUE);
```

### 7.2 Outfit rows (simplified)
```sql
-- Choose one item_template id that carries head/body/leg visuals
INSERT INTO boss_outfits (boss_id, item_id) VALUES
(14, 1234);
```

### 7.3 Skill rows
```sql
-- skill_id is the template id from Skill.java
INSERT INTO boss_skills (boss_id, skill_id, skill_level, cooldown) VALUES
(14, 1, 5, 800),   -- KAMEJOKO lv5, cooldown 800ms
(14, 17, 3, 500),  -- LIEN_HOAN lv3, cooldown 500ms
(14, 23, 1, 5000); -- TROI lv1, cooldown 5000ms
```

### 7.4 Text rows
```sql
INSERT INTO boss_texts (boss_id, text_type, text_content, display_order) VALUES
(14, 'start', '-1|Ta den day!', 1),
(14, 'middle', '-2|Chuan bi di!', 1),
(14, 'end', '-1|Lan sau ta se manh hon!', 1);
```

## 8. Tips & troubleshooting

- Ensure every map id in `map_join_json` exists in server `Manager.MAPS`; missing maps cause bosses not to join zones.
- If a boss has no skills in DB, `Boss.initSkill()` will add a default skill as fallback.
- Avoid excessive logging in production; debug logs are wrapped or removed.

## 9. Extending: Admin UI to edit boss skills (optional)

Two options if you want to manage skills live:
- Admin NPC menu: list bosses, list/edit skills, persist to `boss_skills`, and reload into running boss.
- Admin chat commands: e.g. `/boss skills <bossId>`, `/boss addskill <bossId> <skillId> <level> <cooldown>`, then reload.

Add a helper in `RefactoredBossManager` like `reloadBossSkills(int bossId)` that re-reads `boss_skills` and calls `boss.initSkill()` to apply immediately.
