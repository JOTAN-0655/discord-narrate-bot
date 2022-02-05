# discord-narrate-bot
DISCORDのチャットを読み上げるBOTです。

# requires
このBOTを起動するには以下の環境が必要です。
- JDK 14 or higher

# installation
How_to_Install.mdに書いてあります。

# bot.config　について
コンフィグの項目を次に示します。
### audio_export_path
読み上げ音声が出力されるパス
相対パスと絶対パスを使用できます。

### audio_gen_command
読み上げ音声を生成するスクリプトの実行コマンド<br>
相対パスと絶対パスを使用できます。

### dictionary_path
辞書ファイルを保存するフォルダのパス
相対パスと絶対パスを使用できます。

### token
discordのBOTのtoken

### name_read_mode
名前の読み上げモード
次にモードを示します。

- off<br>このモードがname_read_modeに含まれている場合は名前を読み上げません。
- user_id<br>ユーザーIDを読み上げます。
- nickname<br>ニックネームを読み上げます。
- name<br>表示名を読み上げます。
 
これらのモードをコンマで組み合わせて使用することができます。次に例を示します。

`name_read_mode = user_id,nickname`

この場合は、ユーザーIDを読み上げた後、ニックネームを読み上げて　本文を読み上げるようになります。

### bot_command_prefix
BOTを制御するコマンドの接頭語<br>
初期設定は```!jn```<br>
空白を含めて設定することはできません。

### generate_sound_timeout
合成音声の生成待機時間
これ以上の時間が経過すると、読み上げをキャンセルします。

### generate_log
音声生成スクリプトの実行結果をコンソールに出力します。

### auto_join
自動でVCに参加します。

### auto_leave
自動でVCから抜けます。

# usage
このBOTは次のコマンドがあります。
## !jn help
コマンド一覧を表示します
## !jn
ボイスチャンネル参加＆読み上げ開始
## !jn join
ボイスチャンネル参加
## !jn leave
ボイスチャンネルを抜ける
## !jn n stasrt
読み上げ開始
## !jn n stop
読み上げ停止
## !jn dic add [単語] [読み]
辞書を追加します。
## !jn dic remove [単語]
辞書を削除します。
## !jn reload
コンフィグのリロード

