# Softalk (WINDOWS)
## ①audio_gen.bat
audio_gen.batの内容をいったん全部消して、次の２行に書き換えてください
```
set EXPORT_PATH=[audioフォルダまでの絶対パス]\%2.wav
start .\softalk\SofTalk.exe /X:1 /V:100 /R:%EXPORT_PATH% /W:%1 
```
引数を自分で変えることで音声の種類を変えることができます。
※audioフォルダまでの絶対パスは、エクスプローラー等からわかります。

## ②Softalkをダウンロードする。
ダウンロードしたファイルを展開し、なかのSoftalkというフォルダを、botのjarファイルがあるところにコピーする

# TarakoTalk (WINDOWS)
## audio_gen.bat
```
set EXPORT_PATH=[audioフォルダまでの絶対パス]\%2.wav
call [TarakoTalk.exeまでのパス] save %1 %EXPORT_PATH% 
```

# Voice Vox
## ダウンロードするもの
https://github.com/VOICEVOX/voicevox_core から
- 本体
- Release から Core.zip
- ONNX Runtime
https://visualstudio.microsoft.com/ja/visual-cpp-build-tools/ から インストール
- C++ Build Tools

## /voicevox_core-main/example/python/run.py の変更
一番下あたりの<br>
```run(**vars(parser.parse_args()))```<br>
の上に<br>
```parser.add_argument("--export_path", type=str, required=True)```<br>
を追加。<br>
<br>
``def run`` あたりの<br>
```cpu_num_threads: int```の後ろに`,`をつけて改行し、<br>
```export_path: str``` を追加。<br>

``# 保存`` の下の `soundfile.write ・・・` を <br>
``soundfile.write(export_path, data=wave, samplerate=24000)`` に変更<br>

## audio_gen.bat
```
set TMP=[audioフォルダまでの絶対パス]\%2.wav

python3 .[セットアップしたパス]/voicevox_core-main/example/python/run.py \
--text "%1" \
--speaker_id 0 \
--root_dir_path=".[セットアップしたパス]/voicevox_core-main/release" \
--export_path %TMP%

```

# 音声生成スクリプトを自作する
音声生成スクリプトを自作して、ここにはない音声で読み上げさせることができます。<br>
次に、スクリプトが満たすべき要件を示します。<br>
| 説明 |
| --- |
| スクリプトは第一引数で、読み上げる文章を受け付けるようにしてください。 |
| スクリプトは第二引数で、書き出しファイル名を受け付けるようにしてください。(.wav)は含まない |
