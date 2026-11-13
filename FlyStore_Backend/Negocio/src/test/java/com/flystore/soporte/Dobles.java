package com.flystore.soporte;

import java.lang.reflect.Proxy;
import java.util.function.BiFunction;

public final class Dobles {
    private Dobles() {}
    public static <T> T puerto(Class<T> type, BiFunction<String, Object[], Object> behavior) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> behavior.apply(method.getName(), args)));
    }
}
