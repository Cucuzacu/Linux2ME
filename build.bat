mkdir output

"C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe" -bootclasspath "C:\Java_ME_platform_SDK_3.0\lib\cldc_1.1.jar;C:\Java_ME_platform_SDK_3.0\lib\midp_2.0.jar" -source 1.3 -target 1.3 -d output src\*.java

"C:\Java_ME_platform_SDK_3.0\bin\preverify.exe" -classpath "C:\Java_ME_platform_SDK_3.0\lib\cldc_1.1.jar;C:\Java_ME_platform_SDK_3.0\lib\midp_2.1.jar" -d verified output

"C:\Program Files\Java\jdk1.8.0_202\bin\jar.exe" cvfm Linux2ME.jar MANIFEST.MF Image.bin tux.png -C verified .
