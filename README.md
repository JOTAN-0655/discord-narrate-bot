# discord-narrate-bot
DISCORDのチャットを読み上げるBOTです。

## requires
このBOTを起動するには以下の環境が必要です。
- JDK 14 or higher

## notice
この解説では、音声合成にgTTSを使用することを前提に解説します。

## setup
### Java Install
#### windows
は、次のURLから最新のJDKをインストールしてください
https://www.oracle.com/java/technologies/
#### linux
は、次のコマンドをコンソールに入力してインストール
`sudo apt install openjdk-14-jdk`

### botの取得
releases から、最新版をダウンロードしてください。

https://github.com/JOTAN-0655/discord-narrate-bot/releases

ダウンロードした、ファイルは、どこかのフォルダに移動しておいてください。
できれば、読み上げBOT用のフォルダがあるといいです。

### 音声合成のインストール(gTTS)
gTTSをインストールするにはpipを使うため、pythonがインストールされている必要がありますが、ここでは解説しません。
#### windows
cmd を起動します。
cmdに、次のコマンドを入力
`pip install gTTS`
#### linux
コンソールを起動します。
そこに次のコマンドを入力
`pip install gTTS`

### botの準備
#### windows
次にbotを起動します。
botのファイルと同じディレクトリに、start.batを作ります。
start.batの内容
```
java -jar ./Discord-ChatReadBot-X.X.X.jar
```
X.X.Xは、バージョンによって適宜変えてください。
botを起動すると、`bot.config`ができるはずです。
次に、botのファイルと同じディレクトリに、audio_gen.batを作ります。
```
set TEMP_PATH=./audio/%2.wav
gtts-cli %1 -l ja --output $TEMP_PATH
```
次に、botのファイルと同じディレクトリに、audio と dictionary というフォルダを作ります。

次に、bot.configの内容を編集します
今回の解説の通りで行きますと次の内容になります。
```
audio_export_path=./audio
audio_gen_command=./audio_gen.bat
dictionary_path=./dictionary
token=xxxxxxxxxxxxxxxxxxxxxx
```
token は、自分のDiscordのBOT のtokenを入力してください

最後に、start.batをダブルクリックしてうまく起動するはずです。

#### linux
botを起動します。
コンソールなどから、起動します。なお、start.sh等を作ってから起動してもOKです。
botを起動すると、`bot.config`ができるはずです。
次に、botのファイルと同じディレクトリに、audio_gen.shを作ります。
```
#!/bin/sh
TMP=./audio/"$2".wav
gtts-cli "$1" -l ja --output $TMP
```
次に、botのファイルと同じディレクトリに、audio と dictionary というフォルダを作ります。

次に、bot.configの内容を編集します
今回の解説の通りで行きますと次の内容になります。
```
audio_export_path=./audio
audio_gen_command=sh audio_gen.sh
dictionary_path=./dictionary
token=xxxxxxxxxxxxxxxxxxxxxx
```
token は、自分のDiscordのBOT のtokenを入力してください

最後に、起動すればOKです。
