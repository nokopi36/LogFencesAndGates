# 進捗

最終更新: 2026-10-03

## 現在地
- 実装中: なし
- 次にやること: 公開済み（1.0.0）。今後は不具合対応・NeoForge 正式版への追従・SPEC「いつか」の機能

## 完了
| 日付 | 機能 | コミット | メモ |
|---|---|---|---|
| 2026-10-03 | プロジェクト作成（MDK 26.3 から） | 初回コミット | `./gradlew build` 成功。Gradle Wrapper の jar を 9.2.1 に更新し、公式チェックサムで検証済み |
| 2026-10-03 | 1. 共通基盤 | (このコミット) | DeferredRegister（ModBlocks/ModItems）、datagen 入口（GatherDataEvent.Client）、言語プロバイダー（en_us/ja_jp）。runData 成功、MOD 一覧に表示を確認 |
| 2026-10-03 | 2. オークの原木のフェンス（剥ぎなし） | (このコミット) | 見た目・繋がり・レシピ（12 個）・ドロップ・燃料・専用タブ + 建築ブロックタブをゲーム内で確認 |
| 2026-10-03 | 3. フェンスの部位ごとの樹皮剥ぎ | (このコミット) | useItemOn 上書き + クリック位置で部位判定。GameTest 2 件（部位判定・剥ぎ）成功。ゲーム内で 8 通り・再読み込みを確認 |
| 2026-10-03 | 4. オークの原木のフェンスゲート | (このコミット) | 内側の縦木は柱扱い、開いているときは斧でも開閉のみ。剥ぎ処理を BarkStripping に共通化。GameTest 計 4 件成功。ゲーム内で開閉・塀付き・4 方向を確認。**MVP 完了** |
| 2026-10-03 | 調整: ゲートの剥ぎ部位を 4 部位に | 内側の縦木を独立（GatePart / StrippablePart）。GameTest 5 件成功、ゲーム内で 16 通りを確認 |
| 2026-10-03 | 6. 全種類の木への展開（12 種） | (このコミット) | LogWood enum で登録・datagen・タブをループ化。ネザーの木は不燃・燃料なし・non_flammable_wood。GameTest 6 件成功、ゲーム内で見た目・音・名前・燃焼を確認 |
| 2026-10-03 | 7. 樹皮を剥いだ版のクラフト | (このコミット) | 別アイテム（StrippedLogFenceItem、ブロック共通）。全部剥がれていれば剥いだ版をドロップ・ピックブロック。両タブに追加。GameTest 7 件成功、ゲーム内で確認 |
| 2026-10-03 | 3.7 竹ブロックのフェンス | (このコミット) | LogWood に竹を追加（表皮・板材 2 枚）。レシピ個数を planksPerLog から計算（竹 6・2）。GameTest 8 件成功、ゲーム内で確認 |

## 保留・課題
- `mcsrc/` に NeoForge 本体のソース（gradle キャッシュの `neoforge-*-sources.jar`）も展開済み。NeoForge 更新時は両方展開し直す
- GameTest 用ファイル（`data/log_fences/structure/test_area.nbt`・`test_instance/`）が配布 jar に入る。フェーズ 7 で除外するか検討
- NeoForge 26.3 はベータ版。正式版が出たら `neo_version` を上げて追従する

## 公開
- 2026-10-11 時点で CurseForge に公開済み: https://www.curseforge.com/minecraft/mc-mods/log-fences-and-gates（1.0.0、Release）

## 公開準備（フェーズ 7）
- **2026-10-04 に MOD 名を変更**: Log Fences → Log Fences and Gates（CurseForge に同名の別 MOD があったため）。mod_id `log_fences_and_gates`、パッケージ `com.nokopi.logfencesandgates`、メインクラス `LogFencesAndGates`、jar `logfencesandgates-neoforge-<MC>-<ver>.jar`、GitHub `nokopi36/LogFencesAndGates`。CurseForge のプロジェクト「Log Fences and Gates」作成済み。以下の記録の旧名はそのまま残している
- 1.0.0 は Release で公開（2026-10-05 に Beta から変更。MOD は完成しているため。NeoForge 26.3 はまだベータなので、NeoForge 側の変更で壊れたら修正版で対応する）。jar 名 `logfences-neoforge-26.3-1.0.0.jar`
- mods.toml: 説明文更新、displayURL / issueTrackerURL（GitHub）、bannerFile = `logo.png`（26.3 では logoFile は非推奨で警告が出る）
- アイコン: ゲーム内スクリーンショット（2026-10-04_22.52.48.png）を切り抜き・面積平均で 400×400 に縮小。`src/main/resources/logo.png` と `docs/curseforge/icon.png`（同じ画像）
- MOD 一覧の左の小さいアイコン: NeoForge 26.3 は `iconFile`（24×24 表示）が `logoFile` と別。柱 1 本と剥いだ横木をアップで切り抜いた 128×128 の `src/main/resources/icon.png`、`iconBlur=true`
- クライアントの確認（2026-10-04、ランチャーの新規プロファイル・NeoForge 26.3.0.48-beta・jar 単体）: 起動・MOD 一覧・設置と剥ぎ OK
- GitHub Releases: v1.0.0 を pre-release（Beta）で作成し jar を添付。改名に伴い作り直した（タグ v1.0.0 = 849d78e、`logfencesandgates-neoforge-26.3-1.0.0.jar`、SHA-256 bd34ce90…）。リリースノートは CHANGELOG の該当節 + 対応バージョン + SHA-256。README・CurseForge の説明文にはバージョンを書かず、CHANGELOG と Releases で追う
- GitHub リポジトリ: 公開準備の間 private にし、2026-10-04 に public に戻した。README の状態を「1.0.0 Beta、CurseForge で公開予定」に更新
- GameTest 用ファイルは jar に含めたまま（バニラと同じ形。/test を使わなければ影響なし）
- CurseForge の説明文: `docs/curseforge/description.md`（英語 → 日本語）、変更履歴: `CHANGELOG.md`
- CurseForge 規約（2026-10-04 確認）: AI はショーケース画像の誤認防止のみ規定、コードの規定なし。アイコン 400×400・単色不可・著作物不可・WebP 不可

- 配布 jar のサーバー確認（2026-10-04、NeoForge 26.3.0.42-beta 専用サーバー・本番モード・jar 単体）: 起動 OK、setblock で剥ぎ状態付きのブロックを設置 OK、ルートテーブル（全部剥がれ → 剥いだ版／一部 → 普通の版）OK。`/test run log_fences:*` はプレイヤーのいない専用サーバーでは開始直後から進まず未完（テスト環境側の制約と判断。同じテストは開発環境の GameTestServer で全件成功）

## 他 MOD との互換性
- Diagonal Fences 26.3.0（+ Puzzles Lib 26.3.9）: 一緒に入れても動くが、Log Fences のフェンスは斜めにならない（状態の項目数が違うため対象外）。GameTest も成功。連携は SPEC 3.6「いつか」に記録

## 遊んでみた感想・調整したいこと
- テクスチャ（横向きの木目・柱上面の年輪）は好評
- ゲートはフェンスより細かく剥げてよい → 4 部位に変更済み
- 横木だけ剥ぐと「キ」の形が目立つ（フェンスの形そのもの。色の差で強調される）→ 形の変更案（上の横木を柱の上に載せる／柱の手前に打ち付ける）を検討し、バニラと同じ今の形を維持すると決定
