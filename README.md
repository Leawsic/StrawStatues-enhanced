# Straw Statues

[English](#english) | [中文](#chinese)

---

## English

### Features

- **Straw Statue:** Place a life-sized statue that displays any player's skin. Supports pose editing, scale/rotation, model parts toggle (hat, jacket, sleeves, pants, cape), slim arms, crouching, equipment, and eye gaze configuration.
- **Imported Straw Statue:** Place statues that render external GeckoLib-format 3D models loaded from the config directory.

### Imported Model Setup

Models must be in **GeckoLib 4 format** (exported from Blockbench as "GeckoLib Gecko Entity").

#### File Structure

Place files in the config directory:

```
config/strawstatues/imported_models/<model_id>/
├── model.geo.json            # Required — geometry file
├── texture.png                # Required — texture (64x64 recommended)
└── animation.animation.json   # Optional — animation file
```

`<model_id>` may contain letters, numbers, dashes, and underscores.

#### Commands

| Command | Description |
|---|---|
| `/strawstatues import reload` | Rescan `config/strawstatues/imported_models/` |
| `/strawstatues import list` | List available model IDs |
| `/strawstatues import select <modelId>` | Assign model to the statue you are looking at (crosshair) |
| `/strawstatues import near <modelId>` | Assign model to the nearest statue within 10 blocks |

#### Workflow

1. Place model files in `config/strawstatues/imported_models/<id>/`
2. Run `/strawstatues import reload`
3. Run `/strawstatues import list` to verify
4. Look at the statue and run `/strawstatues import select <id>`, OR
   run `/strawstatues import near <id>` to target the closest statue

### Statue Screens

Sneak + right-click with empty hand on a statue to open the configuration menu.

| Screen | Description |
|---|---|
| Poses | Adjust head, arms, legs, body, cape rotation |
| Style | Slim arms, crouching, gravity, name, sealed |
| Model Parts | Toggle hat/jacket/sleeves/pants/cape, set player name |
| Position | Fine-tune position, center/corner align |
| Scale & Rotations | Scale (1x–8x), X/Z rotation, Y rotation |
| Eye Config | Define eye/pupil regions, adjust pupil offset |
| Equipment | Equip armor, items, player heads |

### Dependencies

- Fabric API (>=0.92.6)
- Puzzles Lib (>=8.1.32)
- Puzzles Api (>=8.1.7)
- GeckoLib 4 (>=4.8.2)

---

## Chinese

### 功能

- **稻草人雕像:** 放置一个显示任意玩家皮肤的真人大小雕像。支持姿态编辑、缩放/旋转、模型部件开关（帽子、夹克、袖子、裤腿、披风）、细手臂、潜行姿态、装备、眼部细节配置。
- **导入模型雕像:** 放置一个渲染外部 GeckoLib 格式 3D 模型的雕像，模型从配置目录加载。

### 导入模型设置

模型必须使用 **GeckoLib 4 格式**（在 Blockbench 中以 "GeckoLib Gecko Entity" 格式导出）。

#### 文件结构

将文件放入配置目录：

```
config/strawstatues/imported_models/<模型名称>/
├── model.geo.json            # 必需 — 几何文件
├── texture.png                # 必需 — 纹理 (建议 64x64)
└── animation.animation.json   # 可选 — 动画文件
```

`<模型名称>` 可以使用字母、数字、连字符和下划线。

#### 命令

| 命令 | 说明 |
|---|---|
| `/strawstatues import reload` | 重新扫描 `config/strawstatues/imported_models/` |
| `/strawstatues import list` | 列出可用模型 ID |
| `/strawstatues import select <模型Id>` | 为准星对准的雕像指定模型 |
| `/strawstatues import near <模型Id>` | 为 10 格内最近的雕像指定模型 |

#### 使用步骤

1. 将模型文件放入 `config/strawstatues/imported_models/<名称>/`
2. 执行 `/strawstatues import reload`
3. 执行 `/strawstatues import list` 确认模型已加载
4. 注视雕像，执行 `/strawstatues import select <名称>`，
   或执行 `/strawstatues import near <名称>` 选择最近的雕像

### 雕像配置界面

潜行 + 空手右键点击雕像打开配置菜单。

| 界面 | 功能 |
|---|---|
| Poses | 调整头部、手臂、腿部、身体、披风旋转 |
| Style | 细手臂、潜行姿态、重力、名称、封存 |
| Model Parts | 开关帽子/夹克/袖子/裤腿/披风、设置玩家名 |
| Position | 微调位置、居中/角落对齐 |
| Scale & Rotations | 缩放 (1x–8x)、X/Z 轴旋转、Y 轴旋转 |
| Eye Config | 定义眼睛/瞳孔区域、调整瞳孔偏移 |
| Equipment | 装备盔甲、物品、玩家头颅 |

### 依赖

- Fabric API (>=0.92.6)
- Puzzles Lib (>=8.1.32)
- Puzzles Api (>=8.1.7)
- GeckoLib 4 (>=4.8.2)

---

## License

CC0-1.0
