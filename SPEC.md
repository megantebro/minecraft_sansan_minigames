# Minecraft Party Games Plugin - 仕様書

## 概要
Hypixel風パーティーゲームプラグイン（Paper 1.21.1対応）
2-8人対応、ロビーシステム付き、12種類のミニゲームをクラス分けして実装。

---

## システム構成

### パッケージ構造
```
lobby.minecraft_minigames/
  Minecraft_minigames.java           メインクラス
  model/                             データモデル
    GameState.java                   ゲーム状態 (WAITING/COUNTDOWN/ACTIVE/ENDING/RESETTING)
    Arena.java                       アリーナデータ
    ArenaConfig.java                 YAML設定読み書き
    PlayerData.java                  プレイヤーセッションデータ
    Party.java                       パーティーデータ
    GameResult.java                  ゲーム結果
  manager/                           マネージャー
    GameManager.java                 ゲームライフサイクル管理
    ArenaManager.java                アリーナ管理 + ブロック復元
    LobbyManager.java                ロビー + レディ + カウントダウン
    ScoreboardManager.java           サイドバースコアボード
    PartyManager.java                パーティー管理
  game/                              ゲームフレームワーク
    Minigame.java                    抽象基底クラス (Template Method Pattern)
    MinigameType.java                ゲーム種類Enum (12種類)
    MinigameRegistry.java            ゲームファクトリー
    impl/                            ミニゲーム実装 (12クラス)
  command/                           コマンド
    MinigameCommand.java             /minigame コマンド
    PartyCommand.java                /party コマンド
  listener/                          イベントリスナー
    GlobalListener.java              グローバルイベント
    LobbyListener.java               ロビーインタラクション
    GameProtectionListener.java      ゲーム保護
  util/                              ユーティリティ
    MessageUtil.java                 チャットフォーマット (Adventure API)
    CountdownTimer.java              カウントダウンタイマー
    LocationUtil.java                Location <-> YAML変換
    ItemBuilder.java                 ItemStack Builder
    Cuboid.java                      領域判定
```

---

## ミニゲーム一覧 (12種類)

| # | ゲーム名 | クラス名 | ルール | 勝利条件 | 制限時間 |
|---|---------|---------|--------|---------|---------|
| 1 | **TNT Run** | `TntRunGame` | 踏んだ床ブロックが0.4秒後に消える。3層構造。 | 最後の1人生存 | 3分 |
| 2 | **Spleef** | `SpleefGame` | ダイヤショベル(効率V)と雪玉で雪ブロックを壊して相手を落とす | 最後の1人生存 | 3分 |
| 3 | **Sumo** | `SumoGame` | ノックバックII付き棒で相手を場外に押し出す。ダメージ無し。 | 最後の1人生存 | 2分 |
| 4 | **One in the Quiver** | `OneInTheQuiverGame` | 弓矢1本で一撃キル。近接キルで矢補充。3秒後にリスポーン。 | 10キル先取 | 3分 |
| 5 | **Hot Potato** | `HotPotatoGame` | TNTを持つ人が爆発。パンチで他プレイヤーに渡す。ラウンド制。 | 最後の1人生存 | 5分 |
| 6 | **Color Swap** | `ColorSwapGame` | 指定された色の羊毛に制限時間内に乗る。床はラウンド毎にランダム化。 | 最後の1人生存 | 5分 |
| 7 | **King of the Hill** | `KingOfTheHillGame` | 中央エリアに立つと毎秒1pt獲得。ノックバック棒で妨害。 | 60pt先取 | 3分 |
| 8 | **Parkour Race** | `ParkourRaceGame` | パルクールコースを競争。落下時はチェックポイントに戻る。 | 最初にゴール | 2分 |
| 9 | **Musical Minecarts** | `MusicalMinecartsGame` | 音楽停止時にトロッコに乗る。カートは人数-1個。 | 最後の1人生存 | 5分 |
| 10 | **Block Shuffle** | `BlockShuffleGame` | 指定ブロックを探して乗る。時間切れで脱落。ラウンド制。 | 最後の1人生存 | 5分 |
| 11 | **Anvil Rain** | `AnvilRainGame` | 空から金床が降る。当たったら脱落。難易度が時間と共に上昇。 | 最後の1人生存 | 3分 |
| 12 | **Snowball Fight** | `SnowballFightGame` | 雪玉でダメージ(1ハート/発)。HP5ハート。雪玉は自動補充。 | 最後の1人生存 | 3分 |

---

## ゲームフレームワーク設計

### Minigame抽象基底クラス
- **Template Methodパターン**: `start()`/`end()` はfinal、サブクラスは以下を実装:
  - `onStart()` - ゲーム開始時の初期化
  - `onEnd()` - ゲーム終了時のクリーンアップ
  - `onTick()` - 毎秒呼ばれるゲームループ
  - `onPlayerEliminate(Player)` - プレイヤー脱落時の処理
- **自己登録Listener**: ゲーム開始時に `registerEvents()` → 終了時に `unregisterAll()`
- **デフォルト勝利条件**: 最後の1人（スコア制ゲームはオーバーライド）

### ゲームライフサイクル
```
WAITING → COUNTDOWN(15秒) → ACTIVE → ENDING(5秒結果表示) → RESETTING → WAITING
```

---

## ロビーシステム

### フロー
1. プレイヤーがコマンド/GUI/看板でゲームに参加
2. アリーナロビーにテレポート
3. ロビーアイテム配布:
   - **スロット0**: ネザースター「準備完了」(右クリック)
   - **スロット4**: コンパス「ゲーム選択」(GUI表示)
   - **スロット8**: 赤染料「退出」
4. 全員準備完了 & 2人以上 → 15秒カウントダウン
5. カウントダウン終了 → ゲーム開始

### ゲーム選択GUI
- 27スロット(3行)のチェストインベントリ
- 各ゲームのアイコン + 説明 + アリーナ数を表示
- クリックで参加

### スコアボード
- ロビー: ゲーム名、プレイヤー数、準備状態
- ゲーム中: 各ゲーム固有の情報 (残り時間、スコア、生存者数)

---

## コマンド

### /minigame (エイリアス: /mg)
| サブコマンド | 説明 | 権限 |
|------------|------|------|
| `join <game>` | ゲームのロビーに参加 | `minigames.play` |
| `leave` | ゲーム/ロビーから退出 | `minigames.play` |
| `list` | ゲーム一覧表示 | `minigames.play` |
| `start` | ゲーム強制開始 | `minigames.admin` |
| `stop [arena]` | ゲーム停止 | `minigames.admin` |

### /party
| サブコマンド | 説明 |
|------------|------|
| `create` | パーティー作成 |
| `invite <player>` | プレイヤーを招待 |
| `accept` | 招待を承認 |
| `deny` | 招待を拒否 |
| `leave` | パーティーから退出 |
| `kick <player>` | メンバーをキック |
| `list` | メンバー一覧 |
| `disband` | パーティー解散 |

---

## アリーナ設定

### arenas.yml
```yaml
arenas:
  tntrun_1:
    display-name: "TNT Run Arena 1"
    game-type: TNT_RUN
    world: world
    bounds:
      corner1: {x: 100, y: 50, z: 100}
      corner2: {x: 150, y: 80, z: 150}
    lobby-spawn: {x: 125, y: 85, z: 125, yaw: 0, pitch: 0}
    spawn-points:
      0: {x: 110, y: 75, z: 110}
      1: {x: 140, y: 75, z: 140}
    custom-points:
      death-y: 50
```

### ゲーム別カスタムデータ
| ゲーム | カスタムキー | 説明 |
|--------|------------|------|
| TNT Run | `death-y` | 脱落判定のY座標 |
| Spleef | `death-y` | 脱落判定のY座標 |
| Sumo | `death-y` | 脱落判定のY座標 |
| Color Swap | `floor-y` | 床のY座標 |
| King of the Hill | `hill-y`, `hill-radius` | 丘の中心Y座標と半径 |
| Parkour Race | `death-y` | 落下リスポーン判定Y座標 |
| Anvil Rain | (自動計算) | アリーナ上空から落下 |

### ブロック復元
TNT Run / Spleef / Color Swap のアリーナは、ゲーム開始時にBlockStateスナップショットを取得し、ゲーム終了時に自動復元。WorldEdit不要。

---

## 設定ファイル

### config.yml
```yaml
lobby:
  spawn:
    world: world
    x: 0
    y: 65
    z: 0
settings:
  min-players: 2
  max-players: 8
  countdown-seconds: 15
```

---

## 技術仕様

| 項目 | 値 |
|------|-----|
| Minecraft | 1.21.1 |
| サーバー | Paper |
| Java | 21 |
| ビルド | Maven (shade plugin) |
| API | Paper API (Adventure Component) |
| 依存関係 | なし (Paper APIのみ) |
| ファイル数 | 39ファイル (36 Java + 3 YAML) |

---

## セットアップ手順

1. `mvn clean package` でビルド
2. `target/minecraft_minigames-1.0-SNAPSHOT.jar` をサーバーの `plugins/` に配置
3. サーバー起動 → `config.yml` と `arenas.yml` が生成される
4. `config.yml` でロビースポーン座標を設定
5. ゲーム用アリーナを建築
6. `arenas.yml` にアリーナの座標を定義
7. `/reload confirm` またはサーバー再起動
8. `/minigame list` でゲーム一覧を確認
9. `/minigame join <game>` でプレイ開始
