# AutoJoin
ボイスチャンネルに誰かが参加したときに、そこに自動で参加します。<br>
|設定|説明|
|---|---|
| true | 有効 |
| false | 無効 |
# AutoLeave
BOTが参加しているVCから全員が抜けた場合、自動でVCから抜けます。<br>
|設定|説明|
|---|---|
| true | 有効 |
| false | 無効 |
# DictionarySavePath
辞書ファイルを保存すためのフォルダのパス<br>
# DiscordCommand
BOTの基本コマンドを設定します<br>
デフォルトは`!narrate`<br>
<br>
空白を含めたコマンドには設定できません<br>
NG例<br>
`!read bot`<br>
# DiscordToken
DiscordのBOTのトークン<br>
# NarrateExportPath
読み上げ音声が出力されるパス。<br>
相対パスと絶対パスを使用できます。<br>
# NarrateGenerateCommand
読み上げ音声を生成スクリプトを実行するためのコマンドです。<br>
# NarrateGenerateDelay
読み上げ音声の存在が確認されてからの待機時間です。<br>
wavファイルが最後に一気にできるのではなく、徐々にwavファイルが生成される場合に使えます。<br>
-1に設定すると、無効にできます。<br>
# NarrateGenerateLog
読み上げ音声の生成スクリプトのログを表示<br>
|設定|説明|
|---|---|
| true | 有効 |
| false | 無効 |
# NarrateGenerateTimeout
読み上げ音声の生成の待機時間です。<br>
-1に設定することで、無効にできます。
# NarrateIgnorePrefix
とある文字から始まるチャットを読み上げない設定はここから可能です<br>
例えば、「!」を設定すると、!から始まる文章は読み上げられません。<br>
# NarrateNameMode
メッセージ送信者の名前を読み上げます<br>
|設定|説明|
|---|---|
| off | このモードが`name_read_mode`に含まれる場合は、ほかのモードにかかわらず読み上げられません |
| user_id | ユーザーIDを読み上げます | 
| nickname | ニックネームを読み上げます |
| name | 表示名を読み上げます |
# NarrateUrlMode
URLの読み上げモードを設定します。<br>
|設定|説明|
|---|---|
| message | `URLが送信されました`と読み上げます。 |
| no | 読み上げません。 |

<br><br><br>
## 古いコンフィグファイルとの対応
|古|新|
|audio_export_path|NarrateExportPath|
|audio_gen_command|NarrateGenerateCommand|
|dictionary_path|DictionarySavePath|
|token|DiscordToken|
|generate_sound_timeout|NarrateGenerateTimeout|
|file_export_delay|NarrateGenerateDelay|
|generate_log|NarrateGenerateLog|
|bot_command_prefix|DiscordCommand|
|url_read_mode|NarrateUrlMode|
|name_read_mode|NarrateNameMode|
|auto_join|AutoJoin|
|auto_leave|AutoLeave|
