# JavaRemoteDebugger使用说明

## 系统要求
+ Windows Java 推荐版本:java version "1.8.0_181"
+ Linux Java 推荐版本:openjdk version "1.8.0_275"

## 服务器
### 选项

**-server &lt;Port&gt; &lt;DebugPort&gt;**
1. **-server** 启动服务器
1. **Port** 用于客户端和服务器通信连接的端口
1. **DebugPort** 用于使用Java远程调试的端口
> 如：/usr/bin/java -jar /var/remotedebug/RemoteDebugger.jar -server 4416 4417
表示用于客户端通信的连接端口为4416，远程调试的端口为4417
在开启服务之前请确保正确配置了防火墙

### 后台运行
可以使用一个service文件保持后台运行
如下是一个示范systemctl service文件：
```
[Unit]
Description=Java Remote Debugger Server

[Service]
WorkingDirectory=/var/remotedebug/
ExecStart=/usr/bin/java -jar /var/remotedebug/RemoteDebugger.jar -server 4416 4417
Restart=always
# Restart service after 10 seconds if the service crashes:
RestartSec=10

[Install]
WantedBy=multi-user.target
```

## 客户端
### 选项

**-remoterun &lt;URL&gt; &lt;File1|File2|...&gt; &lt;JarFileName&gt; &lt;RUN|DEBUG&gt; &lt;StringArguments[]&gt;**
1. **-remoterun** 客户端模式
1. **File1|File2|……** 上传的文件，包括主要运行程序所依赖的jar包，用"|"作为分隔符
1. **JarFileName** 要运行的Java程序文件名
1. **RUN or DEBUG** 运行模式，RUN为普通运行模式，DEBUG为启动远程调试模式
1. **StringArguments[]** 运行程序的参数，用空格作为分隔符
> 如：/usr/bin/java -jar /var/remotedebug/RemoteDebugger.jar -remoterun ws://192.168.1.1:4416 C:\build\libs\example.jar|C:\files\google\gson-2.8.5.jar example.jar RUN param1 param2
表示连接的服务器地址为192.168.1.1:4416，要运行的程序为example.jar，运行模式是普通运行模式，包括两个参数param1和param2

### 配合Gradle使用

1.首先在Gradle项目根目录下新建一个存放程序的文件夹debugger
1.将JavaRemoteDebugger主程序拷贝到文件夹内并重命名为client.jar
1.在build.gradle添加如下内容：
```groovy
def server = "ws://192.168.1.1:4416"
//连接的服务器地址

def outname = "example"
//输出的文件名

def mainclass = "test.example.App"
//包含main()函数的类名完整路径

def runargs = "param1 param2"
//运行程序的所需参数

//如下内容不推荐修改：
def target = outname + ".jar"
def debugger = projectDir.path + "//debugger//client.jar"
jar {
	baseName = outname
	manifest {
		attributes(
			"Manifest-Version": 1.0,
			"Main-Class": mainclass,
		)
		if (!configurations.implementation.isEmpty()) {
			jar.manifest.attributes(
				"Class-Path": ". " + configurations.implementation.collect{ it.name }.join(" ")
			)
		}
	}
}
def uploadfiles = jar.archivePath.toString() + "|" + configurations.implementation.collect{ it }.join("|")
//运行模式
task RemoteRun(type: JavaExec,dependsOn: ":build") {
	classpath = files(debugger)
	args "-remoterun", server, uploadfiles, target, "RUN", runargs
}
//调试模式
task RemoteDebug(type: JavaExec,dependsOn: ":build") {
	classpath = files(debugger)
	args "-remoterun", server, uploadfiles, target, "DEBUG", runargs
}
```

4.启动服务器
5.运行gradle RemoteRun或gradle RemoteDebug就可以在远程运行并调试Java程序

## 终止服务
如需终止服务，可在服务器的目录/var/remotedebug/下创建一个空文件stop.flag用于完全终止服务，服务完全终止后该文件也会被删除，若在不终止服务的情况下终止当前运行的程序，可创建空文件stoprun.flag，运行终止后该文件会被删除，但服务仍然继续运行。


>作者：neronoaka
&lt;cwslive@live.com&gt;
