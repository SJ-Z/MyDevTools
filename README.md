# myDevTools
模拟springboot项目的devtools工具实现类的热部署

### 简介
在springboot项目中，devtools是一个热部署工具，能够让我们的服务器在运行的过程中，动态监听到项目中代码的改变，并快速将改变应用到服务器上，而不需要重启整个服务器来适应变动。
### 内部原理
它内部实现的原理其实是使用了两个ClassLoader，一个ClassLoader加载那些不会被改变的类（第三方jar包），另一个ClassLoader加载用户编写的会更改的类，称为restart ClassLoader。
这样在有代码更改的时候（devtools会监听classpath下的文件变动，并且会立即重启应用，发生在保存时机），原来的restart ClassLoader被丢弃（让GC回收），重新创建一个restart ClassLoader，由于需要加载的类相对比较少，所以实现了较快的重启时间（5秒以内）。
### 类卸载的时机
为了实现在文件变动的时候，让GC能够回收用户自定义的类以及restart ClassLoader这个类加载器，我们首先要知道类卸载的前提。
JVM中的Class只有满足以下三个条件，才能被GC回收，也就是该Class被卸载（unload）：

 1. 该类所有的实例都已被GC，也就是JVM中不存在该Class的任何实例。
 2. 加载该类的ClassLoader已经被GC。
 3. 该类的 java.lang.Class 对象没有在任何地方被引用，如不能在任何地方通过反射访问该类的方法。

##### 原因
由于每个对象都有相应的Class对象，所以当该类仍有实例的时候，是无法卸载的，因为此时Class对象仍可达；
对于ClassLoader对象，留意双亲委托机制中，每个ClassLoader都会记录自身已加载的类信息，所以如果ClassLoader可达，那么Class对象仍是可达的，这就解释了为什么我们为什么需要自定义ClassLoader，因为系统的ClassLoader永远是可达的，他们加载的类在运行时永远不会被卸载；
那现在问题就简单了，我们在加载类的方法时，定义一个临时的ClassLoader，返回结果为Class对象，当这个方法结束后，就仅有该Class对象可以获取到这个ClassLoader；也就是说，当该类的所有实例对象都被gc后，就仅有Class对象可以获得这个ClassLoader了，当我们把这个Class置为空并进行gc后，这个类就会被卸载。
### 代码实现
首先，看下整个项目的目录结构：
com.cose.start包下放的是我们自定义devtools工具所涉及的类，com.jvm.plugin包下放的是测试类（即模拟用户自定义的会发生变动的类）。

最终实现的场景就是，启动项目后，动态修改Constant里面k的值，可以看到TestTools里面main方法打印出的k值一样发生了改变。
#### 自定义类加载器 MyClassLoader
该类加载器即前面提到的restart ClassLoader。
baseClassPath保存用户类的.class文件的路径；classMap维护所有被该类加载器加载的Class，便于类加载器卸载时，将所有的类进行卸载。
重写findClass方法，由类的全限定名从类路径下定位到该类的.class文件，并加载。

#### MyDevTools
让我们自定义的MyClassLoader去加载用户类路径下的.class文件，在初始化MyClassLoader时需要指定类的父加载器为空，绕过双亲委派机制，防止用户类被系统的类加载器加载。
然后去用户类路径下扫描出所有的类字节码文件的路径（这里由于工具类和测试类在一个项目下，需要排除掉我们使用的工具类的类路径）。将所有的.class文件路径放入一个.txt文件中保存起来，这样做的原因是，用户类之间可能存在依赖关系，存在依赖关系的类如果单独用javac命令编译会报错提示“找不到符号”，因此我们让所有的用户类一起编译。
重新编译用户类之后，再用自定义类加载器去加载所有的用户类。利用反射，调用了测试类里面的main方法。
调用完成后，先卸载所有的用户类，再卸载自定义类加载器。此后进入下一次循环，模拟用户类被改变的情况，就可以重新加载用户类，模拟devtools实现了热部署机制。

#### 最终效果
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191021174324922.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzQwMTIxNTAy,size_16,color_FFFFFF,t_70)
Constant.k的初始值是10，修改为20后，可以看出下一次加载编译用户类时，Constant.k的值也被更新为20了。
