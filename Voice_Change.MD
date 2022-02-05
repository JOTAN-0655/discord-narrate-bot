# Softalk (WINDOWS)
## ①audio_gen.bat
audio_gen.batの内容をいったん全部消して、次の２行に書き換えてください
```
set EXPORT_PATH=[audioフォルダまでの絶対パス]\%2.wav
start .\softalk\SofTalk.exe /X:1 /V:100 /R:%EXPORT_PATH% /W:%1 
```
※audioフォルダまでの絶対パスは、エクスプローラー等からわかります。

## ⑤Softalkをダウンロードする。
ダウンロードしたファイルを展開し、なかのSoftalkというフォルダを、botのjarファイルがあるところにコピーする
