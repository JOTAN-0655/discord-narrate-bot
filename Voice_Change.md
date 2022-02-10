# Softalk (WINDOWS)
## ①audio_gen.bat
audio_gen.batの内容をいったん全部消して、次の２行に書き換えてください
```
set EXPORT_PATH=[audioフォルダまでの絶対パス]\%2.wav
start .\softalk\SofTalk.exe /X:1 /V:100 /R:%EXPORT_PATH% /W:%1 
```
※audioフォルダまでの絶対パスは、エクスプローラー等からわかります。

## ②Softalkをダウンロードする。
ダウンロードしたファイルを展開し、なかのSoftalkというフォルダを、botのjarファイルがあるところにコピーする

# Voice Vox
## ダウンロードするもの
https://github.com/VOICEVOX/voicevox_core から
- 本体
- Release から Core.zip
- ONNX Runtime
https://visualstudio.microsoft.com/ja/visual-cpp-build-tools/ から インストール
- C++ Build Tools

## audio_gen.bat
```
set TMP=[audioフォルダまでの絶対パス]\%2.wav

python3 ./audio_gen/voicevox_core-main/example/python/run.py \
--text "%1" \
--speaker_id 0 \
--root_dir_path="./audio_gen/voicevox_core-main/release" \
--export_path %TMP%

```

# 音声生成スクリプトを自作する
音声生成スクリプトを自作して、ここにはない音声で読み上げさせることができます。<br>
次に、スクリプトが満たすべき要件を示します。<br>
| 説明 |
| --- |
| スクリプトは第一引数で、読み上げる文章を受け付けるようにしてください。 |
| スクリプトは第二引数で、書き出しファイル名を受け付けるようにしてください。(.wav)は含まない |
