# audio_export_path
読み上げ音声が出力されるパス。<br>
相対パスと絶対パスを使用できます。<br>

# audio_gen_command
読み上げ音声を生成スクリプトを実行するためのコマンドです。<br>

# dictionary_path
辞書ファイルを保存すためのフォルダのパス<br>

# token
DiscordのBOTのトークン<br>

# generate_sound_timeout
読み上げ音声の生成の待機時間です。<br>
-1に設定することで、無効にできます。

# file_export_delay
読み上げ音声の存在が確認されてからの待機時間です。<br>
wavファイルが最後に一気にできるのではなく、徐々にwavファイルが生成される場合に使えます。<br>
-1に設定すると、無効にできます。<br>

# generate_log
読み上げ音声の生成スクリプトのログを表示<br>

# bot_command_prefix
BOTの基本コマンドを設定します<br>
デフォルトは`!jn`<br>
<br>
空白を含めたコマンドには設定できません<br>
NG例<br>
`!read bot`<br>

# url_read_mode
URLの読み上げモードを設定します。<br>
|設定|説明|
|---|---|
| message | `URLが送信されました`と読み上げます。 |
| no | 読み上げません。 |

# name_read_mode
メッセージ送信者の名前を読み上げます<br>
|設定|説明|
|---|---|
| off | このモードが`name_read_mode`に含まれる場合は、ほかのモードにかかわらず読み上げられません |
| user_id | ユーザーIDを読み上げます | 
| nickname | ニックネームを読み上げます |
| name | 表示名を読み上げます |

これらのモードをコンマで組み合わせて使用することができます。次に例を示します。<br>
`name_read_mode = user_id,nickname`<br>
この場合は、ユーザーIDを読み上げた後、ニックネームを読み上げて　本文を読み上げるようになります。<br>

# auto_join
ボイスチャンネルに誰かが参加したときに、そこに自動で参加します。<br>
|設定|説明|
|---|---|
| true | 有効 |
| false | 無効 |

# auto_leave
BOTが参加しているVCから全員が抜けた場合、自動でVCから抜けます。<br>
|設定|説明|
|---|---|
| true | 有効 |
| false | 無効 |
